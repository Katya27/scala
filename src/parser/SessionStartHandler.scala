package src.parser

class SessionStartHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    try {
      val parts = line.split(" ")
      ctx.startTime = Some(DateTimeParser.parse(parts(1)))
    } catch {
      case e: Exception =>
        ctx.addError("SESSION_START", s"SESSION_START: ${e.getMessage} в строке: $line")
    }
  }
}