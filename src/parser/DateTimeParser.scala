package src.parser

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateTimeParser {
  private val formatter1 = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm:ss")
  private val formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")
  private val formatter3 = DateTimeFormatter.ofPattern("EEE,_d_MMM_yyyy_HH:mm:ss_Z", Locale.ENGLISH)

  def parse(str: String): LocalDateTime = {
    try {
      LocalDateTime.parse(str, formatter1)
    } catch {
      case _: DateTimeParseException =>
        try {
          LocalDateTime.parse(str, formatter2)
        } catch {
          case _: DateTimeParseException =>
           LocalDateTime.parse(str, formatter3)
        }
    }
  }
}