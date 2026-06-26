package src.parser

import java.time.LocalDateTime
import scala.collection.mutable.{ListBuffer, Map => MutableMap}

private[parser] class ParsingContext {
  var sessionId: Option[Int] = None
  var startTime: Option[LocalDateTime] = None
  var endTime: Option[LocalDateTime] = None
  var currentSearch: Option[Either[QuickSearchBuilder, CardSearchBuilder]] = None
  val qsBuilders = ListBuffer.empty[QuickSearchBuilder]
  val csBuilders = ListBuffer.empty[CardSearchBuilder]

  var errorCount: Int = 0
  val errorTypes: ListBuffer[String] = ListBuffer.empty
  val errorMessages: ListBuffer[String] = ListBuffer.empty

  def addError(errorType: String, msg: String): Unit = {
    errorCount += 1
    errorTypes += errorType
    errorMessages += msg
  }
}

private[parser] case class QuickSearchBuilder(
                                               datetime: LocalDateTime,
                                               query: String,
                                               searchId: Int,
                                               documents: ListBuffer[String],
                                               documentOpens: ListBuffer[DocumentOpen]
                                             ) {
  def build(): QuickSearch =
    QuickSearch(datetime, query, searchId, documents.toList, documentOpens.toList)
}

private[parser] case class CardSearchBuilder(
                                              startTime: LocalDateTime,
                                              var searchId: Int,
                                              params: MutableMap[String, String],
                                              documents: ListBuffer[String],
                                              documentOpens: ListBuffer[DocumentOpen]
                                            ) {
  def build(): CardSearch =
    CardSearch(startTime, searchId, params.toMap, documents.toList, documentOpens.toList)
}

object SessionParser {
  private val handlers: Map[String, EventHandler] = Map(
    "SESSION_START"     -> new SessionStartHandler(),
    "SESSION_END"       -> new SessionEndHandler(),
    "QS"                -> new QSHandler(),
    "CARD_SEARCH_START" -> new CardSearchStartHandler(),
    "CARD_SEARCH_END"   -> new CardSearchEndHandler(),
    "$"                 -> new ParamHandler(),
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

    while (iter.hasNext) {
      val line = iter.next()
      val prefix = if (line.startsWith("$")) "$" else line.takeWhile(_ != ' ').trim
      handlers.get(prefix) match {
        case Some(handler) => handler.handle(line, ctx, iter)
        case None =>
          ctx.addError("UNKNOWN_LINE", s"Неизвестная строка: $line")
      }
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
      errorCount = ctx.errorCount,
      errorTypes = ctx.errorTypes.toList,
      errorMessages = ctx.errorMessages.toList
    )

    (session, report)
  }
}