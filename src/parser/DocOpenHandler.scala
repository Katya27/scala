package src.parser

import java.time.LocalDateTime
import scala.collection.mutable.ListBuffer

case class DocumentOpen(
                         timestamp: LocalDateTime,
                         docId: String,
                         searchId: Int
                       )

class DocOpenHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    try {
      val parts = line.split(" ")
      val (timestampStr, searchIdStr, docId) =
        if (parts.length == 4) (parts(1), parts(2), parts(3))
        else ("", parts(1), parts(2))

      val timestamp = if (timestampStr.nonEmpty) DateTimeParser.parse(timestampStr, ctx.errorCollector) else None
      val searchId = searchIdStr.toInt

      ctx.currentSearch match {
        case Some(Left(qsb)) if qsb.searchId == searchId =>
          addOpenIfNotExists(qsb.documentOpens, timestamp.getOrElse(qsb.datetime), docId, searchId)

        case Some(Right(csb)) if csb.searchId == searchId =>
          addOpenIfNotExists(csb.documentOpens, timestamp.getOrElse(csb.startTime), docId, searchId)

        case _ =>
          ctx.qsBuilders.find(_.searchId == searchId).foreach { qsb =>
            addOpenIfNotExists(qsb.documentOpens, timestamp.getOrElse(qsb.datetime), docId, searchId)
          }
          ctx.csBuilders.find(_.searchId == searchId).foreach { csb =>
            addOpenIfNotExists(csb.documentOpens, timestamp.getOrElse(csb.startTime), docId, searchId)
          }
      }
    } catch {
      case e: Exception =>
        ctx.addError("DOC_OPEN", s"DOC_OPEN: ошибка парсинга: ${e.getMessage} в строке: $line")
    }
  }

  private def addOpenIfNotExists(
                                  opens: ListBuffer[DocumentOpen],
                                  ts: LocalDateTime,
                                  docId: String,
                                  searchId: Int
                                ): Unit = {
    if (!opens.exists(_.docId == docId)) {
      opens += DocumentOpen(ts, docId, searchId)
    }
  }
}