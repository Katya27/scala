package src.parser

class DocOpenHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    try {
      val parts = line.split(" ")
      val (timestampStr, searchIdStr, docId) =
        if (parts.length == 4) (parts(1), parts(2), parts(3))
        else ("", parts(1), parts(2))

      val timestamp = if (timestampStr.nonEmpty) Some(DateTimeParser.parse(timestampStr)) else None
      val searchId = searchIdStr.toInt

      ctx.currentSearch match {
        case Some(Left(qsb)) if qsb.searchId == searchId =>
          val ts = timestamp.getOrElse(qsb.datetime)
          qsb.documentOpens += DocumentOpen(ts, docId, searchId)

        case Some(Right(csb)) if csb.searchId == searchId =>
          val ts = timestamp.getOrElse(csb.startTime)
          csb.documentOpens += DocumentOpen(ts, docId, searchId)

        case _ =>
      }
    } catch {
      case e: Exception =>
        ctx.addError("DOC_OPEN", s"DOC_OPEN: ошибка парсинга: ${e.getMessage} в строке: $line")
    }
  }
}