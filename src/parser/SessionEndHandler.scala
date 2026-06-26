package src.parser

class SessionEndHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    try {
      val parts = line.split(" ")
      ctx.endTime = Some(DateTimeParser.parse(parts(1)))
    } catch {
      case e: Exception =>
        ctx.addError("SESSION_END", s"SESSION_END: ${e.getMessage} в строке: $line")
    }
  }
}