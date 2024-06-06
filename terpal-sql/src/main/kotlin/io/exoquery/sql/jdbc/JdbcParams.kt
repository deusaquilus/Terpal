package io.exoquery.sql.jdbc

import io.exoquery.sql.Decoder
import io.exoquery.sql.Encoder
import io.exoquery.sql.JdbcParam
import java.sql.*
import java.time.*

abstract class Decoders<Session, Row> {
  abstract fun isNull(index: Int, row: Row): Boolean

  abstract val BooleanDecoder: Decoder<Session, Row, Boolean>
  abstract val ByteDecoder: Decoder<Session, Row, Byte>
  abstract val CharDecoder: Decoder<Session, Row, Char>
  abstract val DoubleDecoder: Decoder<Session, Row, Double>
  abstract val FloatDecoder: Decoder<Session, Row, Float>
  abstract val IntDecoder: Decoder<Session, Row, Int>
  abstract val LongDecoder: Decoder<Session, Row, Long>
  abstract val ShortDecoder: Decoder<Session, Row, Short>
  abstract val StringDecoder: Decoder<Session, Row, String>

  abstract val LocalDateDecoder: Decoder<Session, Row, LocalDate>
  abstract val LocalTimeDecoder: Decoder<Session, Row, LocalTime>
  abstract val LocalDateTimeDecoder: Decoder<Session, Row, LocalDateTime>
  abstract val ZonedDateTimeDecoder: Decoder<Session, Row, ZonedDateTime>

  abstract val InstantDecoder: Decoder<Session, Row, Instant>
  abstract val OffsetTimeDecoder: Decoder<Session, Row, OffsetTime>
  abstract val OffsetDateTimeDecoder: Decoder<Session, Row, OffsetDateTime>

  open val decoders by lazy {
    listOf(
      BooleanDecoder,
      ByteDecoder,
      CharDecoder,
      DoubleDecoder,
      FloatDecoder,
      IntDecoder,
      LongDecoder,
      ShortDecoder,
      StringDecoder,
      LocalDateDecoder,
      LocalTimeDecoder,
      LocalDateTimeDecoder,
      ZonedDateTimeDecoder,
      InstantDecoder,
      OffsetTimeDecoder,
      OffsetDateTimeDecoder
    )
  }

}


abstract class Encoders<Session, Stmt> {
  abstract val BooleanEncoder: Encoder<Session, Stmt, Boolean>
  abstract val ByteEncoder: Encoder<Session, Stmt, Byte>
  abstract val CharEncoder: Encoder<Session, Stmt, Char>
  abstract val DoubleEncoder: Encoder<Session, Stmt, Double>
  abstract val FloatEncoder: Encoder<Session, Stmt, Float>
  abstract val IntEncoder: Encoder<Session, Stmt, Int>
  abstract val LongEncoder: Encoder<Session, Stmt, Long>
  abstract val ShortEncoder: Encoder<Session, Stmt, Short>
  abstract val StringEncoder: Encoder<Session, Stmt, String>

  abstract val LocalDateEncoder: Encoder<Session, Stmt, LocalDate>
  abstract val LocalTimeEncoder: Encoder<Session, Stmt, LocalTime>
  abstract val LocalDateTimeEncoder: Encoder<Session, Stmt, LocalDateTime>
  abstract val ZonedDateTimeEncoder: Encoder<Session, Stmt, ZonedDateTime>

  abstract val InstantEncoder: Encoder<Session, Stmt, Instant>
  abstract val OffsetTimeEncoder: Encoder<Session, Stmt, OffsetTime>
  abstract val OffsetDateTimeEncoder: Encoder<Session, Stmt, OffsetDateTime>

  val encoders by lazy {
    listOf(
      BooleanEncoder,
      ByteEncoder,
      CharEncoder,
      DoubleEncoder,
      FloatEncoder,
      IntEncoder,
      LongEncoder,
      ShortEncoder,
      StringEncoder,
      LocalDateEncoder,
      LocalTimeEncoder,
      LocalDateTimeEncoder,
      ZonedDateTimeEncoder,
      InstantEncoder,
      OffsetTimeEncoder,
      OffsetDateTimeEncoder
    )
  }
}

abstract class JdbcParams(val encoders: Encoders<Connection, PreparedStatement>): ContextParams<Connection, PreparedStatement> {
  inline fun <reified T: Any> param(value: T, crossinline encoder: (Connection, PreparedStatement, T, Int) -> Unit): JdbcParam<T> = JdbcParam<T>(value, JdbcEncoder.fromFunction(encoder))

  override fun param(value: Boolean): JdbcParam<Boolean> = JdbcParam<Boolean>(value, encoders.BooleanEncoder)
  override fun param(value: Byte): JdbcParam<Byte> = JdbcParam<Byte>(value, encoders.ByteEncoder)
  override fun param(value: Char): JdbcParam<Char> = JdbcParam<Char>(value, encoders.CharEncoder)
  override fun param(value: Double): JdbcParam<Double> = JdbcParam<Double>(value, encoders.DoubleEncoder)
  override fun param(value: Float): JdbcParam<Float> = JdbcParam<Float>(value, encoders.FloatEncoder)
  override fun param(value: Int): JdbcParam<Int> = JdbcParam<Int>(value, encoders.IntEncoder)
  override fun param(value: Long): JdbcParam<Long> = JdbcParam<Long>(value, encoders.LongEncoder)
  override fun param(value: Short): JdbcParam<Short> = JdbcParam<Short>(value, encoders.ShortEncoder)
  override fun param(value: String): JdbcParam<String> = JdbcParam<String>(value, encoders.StringEncoder)

  override fun param(value: LocalDate): JdbcParam<LocalDate> = JdbcParam(value, encoders.LocalDateEncoder)
  override fun param(value: LocalTime): JdbcParam<LocalTime> = JdbcParam(value, encoders.LocalTimeEncoder)
  override fun param(value: LocalDateTime): JdbcParam<LocalDateTime> = JdbcParam(value, encoders.LocalDateTimeEncoder)
  override fun param(value: ZonedDateTime): JdbcParam<ZonedDateTime> = JdbcParam(value, encoders.ZonedDateTimeEncoder)

  override fun param(value: Instant): JdbcParam<Instant> = JdbcParam(value, encoders.InstantEncoder)
  override fun param(value: OffsetTime): JdbcParam<OffsetTime> = JdbcParam(value, encoders.OffsetTimeEncoder)
  override fun param(value: OffsetDateTime): JdbcParam<OffsetDateTime> = JdbcParam(value, encoders.OffsetDateTimeEncoder)
}
