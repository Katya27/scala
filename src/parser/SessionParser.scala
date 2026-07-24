package src.parser

import java.time.LocalDateTime
import scala.collection.mutable.{ListBuffer, Map => MutableMap}

case class Session(
                    sessionId: Int,
                    startTime: LocalDateTime,
                    endTime: LocalDateTime,
                    quickSearches: List[QuickSearch],
                    cardSearches: List[CardSearch]
                  )

private[parser] class ParsingContext(val errorCollector: ErrorCollector = new ErrorCollector()) {
  var sessionId: Option[Int] = None
  var startTime: Option[LocalDateTime] = None
  var endTime: Option[LocalDateTime] = None
  var currentSearch: Option[Either[QuickSearchBuilder, CardSearchBuilder]] = None
  val qsBuilders = ListBuffer.empty[QuickSearchBuilder]
  val csBuilders = ListBuffer.empty[CardSearchBuilder]

  def addError(errorType: String, msg: String): Unit = {
    errorCollector.add(errorType, msg)
  }
}

trait EventHandler {
  def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit
}

object SessionParser {
  private val handlers: Map[String, EventHandler] = Map(
    "QS"                -> new QSHandler(),
    "CARD_SEARCH_START" -> new CardSearchHandler(),
    "DOC_OPEN"          -> new DocOpenHandler()
  )

  def parse(fileName: String, content: String): (Session, ParseReport) = {
    val lines = content.split("\n").map(_.trim)
    val iter = lines.iterator
    val ctx = new ParsingContext()

    try {
      ctx.sessionId = Some(fileName.toInt)
    } catch {
      case _: NumberFormatException =>
        ctx.addError("SESSION_ID", s"Не удалось преобразовать имя файла '$fileName' в Int")
    }

    try {
      while (iter.hasNext) {
        val line = iter.next()
        val prefix = if (line.startsWith("$")) "$" else line.takeWhile(_ != ' ').trim

        prefix match {
          case "SESSION_START" =>
            val parts = line.split(" ")
            DateTimeParser.parse(parts(1), ctx.errorCollector).foreach(dt => ctx.startTime = Some(dt))

          case "SESSION_END" =>
            val parts = line.split(" ")
            DateTimeParser.parse(parts(1), ctx.errorCollector).foreach(dt => ctx.endTime = Some(dt))

          case _ =>
            handlers.get(prefix) match {
              case Some(handler) => handler.handle(line, ctx, iter)
              case None => ctx.addError("UNKNOWN_LINE", s"Неизвестная строка: $line")
            }
        }
      }
    } catch {
      case e: Exception =>
        ctx.errorCollector.addWithStack("PARSER_CRASH", s"Критическая ошибка при парсинге файла $fileName", e)
    }

    val quickSearches = ctx.qsBuilders.map(_.build()).toList
    val cardSearches  = ctx.csBuilders.map(_.build()).toList

    val session = Session(
      sessionId = ctx.sessionId.getOrElse(0),
      startTime = ctx.startTime.getOrElse(LocalDateTime.MIN),
      endTime   = ctx.endTime.getOrElse(LocalDateTime.MIN),
      quickSearches = quickSearches,
      cardSearches  = cardSearches
    )

    val report = ParseReport(
      errorCount = ctx.errorCollector.getTotal,
      errorTypes = ctx.errorCollector.getCounters.keys.toList,
      errorMessages = ctx.errorCollector.getMessages
    )

    (session, report)
  }
}