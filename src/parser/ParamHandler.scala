package src.parser

class ParamHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    try {
      val dollarIdx = line.indexOf('$')
      val rest = line.substring(dollarIdx + 1)
      val spaceIdx = rest.indexOf(' ')
      val key = rest.substring(0, spaceIdx).trim
      val value = rest.substring(spaceIdx + 1).trim

      ctx.currentSearch match {
        case Some(Right(csb)) =>
          csb.params += (key -> value)
        case _ =>
          ctx.addError("PARAM", s"PARAM: параметр $key=$value встречен вне карточного поиска: $line")
      }
    } catch {
      case e: Exception =>
        ctx.addError("PARAM", s"PARAM: ошибка парсинга: ${e.getMessage} в строке: $line")
    }
  }
}