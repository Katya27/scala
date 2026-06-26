package src.parser

import scala.collection.mutable.ListBuffer

class QSHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    var nextLine: String = null
    try {
      if (iter.hasNext) nextLine = iter.next()
      else {
        ctx.addError("QS", s"QS: нет следующей строки с данными поиска: $line")
        ctx.currentSearch = None
        return
      }
    } catch {
      case e: Exception =>
        ctx.addError("QS", s"QS: ошибка при чтении следующей строки: ${e.getMessage} в строке: $line")
        ctx.currentSearch = None
        return
    }

    try {
      val parts = line.split(" ")
      val datetime = DateTimeParser.parse(parts(1))
      val query = extractQuery(line)

      val tokens = nextLine.split("\\s+")
      val searchId = tokens.head.toInt
      val docs = tokens.tail.toList

      val builder = QuickSearchBuilder(
        datetime,
        query,
        searchId,
        ListBuffer.empty ++= docs,
        ListBuffer.empty
      )
      ctx.qsBuilders += builder
      ctx.currentSearch = Some(Left(builder))
    } catch {
      case e: Exception =>
        ctx.addError("QS", s"QS: ошибка парсинга: ${e.getMessage} в строке: $line")
        ctx.currentSearch = None
    }
  }

  private def extractQuery(line: String): String = {
    val start = line.indexOf('{')
    val end = line.indexOf('}')
    if (start >= 0 && end > start) line.substring(start + 1, end) else ""
  }
}