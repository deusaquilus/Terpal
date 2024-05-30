package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import java.sql.Connection
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import java.sql.PreparedStatement
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement

internal class CoroutineTransaction(private var completed: Boolean = false) : AbstractCoroutineContextElement(CoroutineTransaction) {
  companion object Key : CoroutineContext.Key<CoroutineTransaction>
  val incomplete: Boolean
    get() = !completed

  fun complete() {
    completed = true
  }
  override fun toString(): String = "CoroutineTransaction(completed=$completed)"
}

class JdbcContext(override val database: DataSource): Context<Connection, DataSource>() {
  override fun newSession(): Connection = database.connection
  override fun closeSession(session: Connection): Unit = session.close()
  override fun isClosedSession(session: Connection): Boolean = session.isClosed

  private val JdbcCoroutineContext = object: CoroutineContext.Key<CoroutineSession<Connection>> {}
  override val sessionKey: CoroutineContext.Key<CoroutineSession<Connection>> = JdbcCoroutineContext

  // TODO override by inline despite the warning?
  override internal suspend fun <T> runTransactionally(block: suspend CoroutineScope.() -> T): T {
    val session = coroutineContext.get(sessionKey)?.session ?: error("No connection found")
    session.runWithManualCommit {
      val transaction = CoroutineTransaction()
      try {
        val result = withContext(transaction) { block() }
        commit()
        return result
      } catch (ex: Throwable) {
        rollback()
        throw ex
      } finally {
        transaction.complete()
      }
    }
  }




  internal inline fun <T> Connection.runWithManualCommit(block: Connection.() -> T): T {
    val before = autoCommit

    return try {
      autoCommit = false
      this.run(block)
    } finally {
      autoCommit = before
    }
  }

  //serializer.serialize(PreparedStatementElementEncoder(ps, index+1), value)

  val atomEncoders: AtomEncoders<Connection, PreparedStatement> =
    AtomEncoders.single(CommonAtoms.LocalDate, JdbcAtomEncoder<java.time.LocalDate> { v, idx -> setObject(idx, v) })


  // Do it this way so we can vaoid value casting in the runScoped function
  fun <T> Param<T>.write(index: Int, conn: Connection, ps: PreparedStatement): Unit =
    when (val payload = this.payload) {
      is Param.Payload.Serial -> payload.serializer.serialize(PreparedStatementElementEncoder(ps, index+1), value)
      is Param.Payload.Atomic -> atomEncoders.get(payload.atomKind)?.let { it.encode(conn, ps, index+1, value) }
        ?: error("Could nto find an encoder in the context ${this} for the atom-kind ${payload.atomKind}")
    }

  private suspend fun <T> runScoped(query: Query<T>): List<T> {
    val outputs = mutableListOf<T>()
    withConnection {
      val conn = coroutineContext.get(sessionKey)?.session ?: error("No connection detected in withConnection scope. This should be impossible.")
      conn.prepareStatement(query.sql).use { stmt ->
        // prepare params
        query.params.withIndex().forEach { (idx, param) ->
          param.write(idx, conn, stmt)
        }
        // execute the query and encode results
        stmt.executeQuery().use { rs ->
          while (rs.next()) {
            val decoder = ResultDecoder(rs, query.resultMaker.descriptor)
            outputs += query.resultMaker.deserialize(decoder)
          }
        }
      }
    }
    return outputs
  }

  suspend fun <T> run(query: Query<T>): Deferred<List<T>> =
    CoroutineScope(Dispatchers.IO).async {
      runScoped(query)
    }
}

interface JdbcAtomEncoder<T>: AtomEncoder<Connection, PreparedStatement, T> {
 companion object {
   // TODO another constructor that uses the session (want in future for some rare clob-related use-cases)
   operator fun <T> invoke(encoder: PreparedStatement.(T, Int) -> Unit): JdbcAtomEncoder<T> =
     object: JdbcAtomEncoder<T> {
       override fun encode(sess: Connection, stmt: PreparedStatement, idx: Int, value: T) = encoder(stmt, value, idx)
     }
 }
}

interface AtomEncoder<Session, Stmt, T> {
  fun encode(sess: Session, stmt: Stmt, idx: Int, value: T)
}

abstract class Context<Session, Database> {
  abstract val database: Database
  abstract fun newSession(): Session

  abstract fun closeSession(session: Session): Unit
  abstract fun isClosedSession(session: Session): Boolean
  abstract internal suspend fun <T> runTransactionally(block: suspend CoroutineScope.() -> T): T

  abstract val sessionKey: CoroutineContext.Key<CoroutineSession<Session>>

  inner class CoroutineSession<Session>(val session: Session) : AbstractCoroutineContextElement(sessionKey) {
    override fun toString() = "CoroutineSession($sessionKey)"
  }

  protected suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.() -> T): T {
    return if (coroutineContext.hasOpenConnection()) {
      withContext(coroutineContext) { block() }
    } else {
      val session = newSession()
      try {
        withContext(CoroutineSession(session)) { block() }
      } finally { closeSession(session) }
    }
  }

  protected fun CoroutineContext.hasOpenConnection(): Boolean {
    val session = get(sessionKey)?.session
    return session != null && !isClosedSession(session)
  }

  suspend fun <T> transaction(block: suspend CoroutineScope.() -> T): T {
    val existingTransaction = coroutineContext[CoroutineTransaction]

    return when {
      existingTransaction == null ->
        withConnection { runTransactionally { block() } }

      // This must mean it's a transaction { stuff... transaction { ... } } so let the outer transaction do the committing
      existingTransaction.incomplete ->
        withContext(coroutineContext) { block() }

      else -> error("Attempted to start new transaction within: $existingTransaction")
    }
  }
}


//suspend fun runScoped(): List<T> {
//    val outputs = mutableListOf<T>()
//    coroutineContext.connection.prepareStatement(sql).use { stmt ->
//      // prepare params
//      params.withIndex().forEach { (idx, param) ->
//        param.write(idx, stmt)
//      }
//      // execute the query and encode results
//      stmt.executeQuery().use { rs ->
//        while (rs.next()) {
//          val decoder = ResultDecoder(rs, resultMaker.descriptor)
//          outputs += resultMaker.deserialize(decoder)
//        }
//      }
//    }
//    return outputs
//  }
//
//  suspend fun run(ds: DataSource) =
//    CoroutineScope(Dispatchers.IO + CoroutineDataSource(ds)).async {
//      transaction {
//        runScoped()
//      }
//    }

//suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.() -> T): T {
//  return if (coroutineContext.hasOpenConnection()) {
//    withContext(coroutineContext) { block() }
//  } else {
//    val connection = coroutineContext.dataSource.connection
//    try {
//      withContext(CoroutineConnection(connection)) { block() }
//    } finally { connection.closeCatching() }
//  }
//}


//@PublishedApi
//internal fun Connection.isClosedCatching(): Boolean {
//  return try {
//    isClosed
//  } catch (ex: SQLException) {
//    //logger.warn(ex) { "Connection isClosedCatching check failed, assuming closed:" }
//    true
//  }
//}
