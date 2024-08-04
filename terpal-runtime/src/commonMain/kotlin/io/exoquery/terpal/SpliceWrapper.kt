package io.exoquery.terpal

class WrapException(override val message: String, override val cause: Throwable): RuntimeException(message, cause)

fun <T> wrapSplice(locationPath: String, code: String, spliceTermNumber: Int, totalTerms: Int, splice: () -> T): T =
  try {
    splice()
  } catch (e: Exception) {
    throw WrapException(
      """Error in spliced term #${spliceTermNumber} (of ${totalTerms}) at ${locationPath}
        |The code at this locaiton looks approximately like:
        |${code}
      """.trimMargin(), e)
  }


