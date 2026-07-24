package src.parser

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.util.Locale
import com.fasterxml.jackson.databind.ObjectMapper
import scala.io.Source

object DateTimeParser {
  private val defaultLocale = Locale.ENGLISH
  private val mapper = new ObjectMapper()

  private lazy val formatters: List[DateTimeFormatter] = {
    try {
      val json = Source.fromResource("dataformats.json").mkString
      val patterns: Array[String] = mapper.readValue(json, classOf[Array[String]])
      patterns.map(DateTimeFormatter.ofPattern(_, defaultLocale)).toList
    } catch {
      case _: Exception => List.empty
    }
  }

  def parse(str: String, errorCollector: ErrorCollector): Option[LocalDateTime] = {
    val trimmed = str.trim
    if (formatters.isEmpty) {
      errorCollector.add("DATE_FORMAT_LOAD_ERROR", "Форматы не загружены")
      return None
    }


    formatters.find { f =>
      try {
        LocalDateTime.parse(trimmed, f)
        true
      } catch {
        case _: DateTimeParseException => false
      }
    }.map { f => LocalDateTime.parse(trimmed, f) } match {
      case Some(dt) => Some(dt)
      case None =>
        errorCollector.add("DATE_PARSE_ERROR", s"Не удалось распарсить: $trimmed")
        None
    }
  }
}
