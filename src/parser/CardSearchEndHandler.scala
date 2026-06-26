package src.parser

class CardSearchEndHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    var nextLine: String = null
    try {
      if (iter.hasNext) nextLine = iter.next()
      else {
        ctx.addError("CARD_SEARCH_END", s"CARD_SEARCH_END: нет следующей строки с данными поиска: $line")
        return
      }
    } catch {
      case e: Exception =>
        ctx.addError("CARD_SEARCH_END", s"CARD_SEARCH_END: ошибка при чтении следующей строки: ${e.getMessage} в строке: $line")
        return
    }

    try {
      val tokens = nextLine.split("\\s+")
      val searchId = tokens.head.toInt
      val docs = tokens.tail.toList

      ctx.currentSearch match {
        case Some(Right(csb)) =>
          csb.searchId = searchId
          csb.documents ++= docs
        case _ =>
          ctx.addError("CARD_SEARCH_END", s"CARD_SEARCH_END: нет активного карточного поиска для строки: $line")
      }
    } catch {
      case e: Exception =>
        ctx.addError("CARD_SEARCH_END", s"CARD_SEARCH_END: ошибка парсинга: ${e.getMessage} в строке: $line")
    }
  }
}