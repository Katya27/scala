package src.parser

import scala.collection.mutable.{ListBuffer, Map => MutableMap}

class CardSearchStartHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    try {
      val parts = line.split(" ")
      val startTime = DateTimeParser.parse(parts(1))
      val builder = CardSearchBuilder(
        startTime = startTime,
        searchId = 0,
        params = MutableMap.empty,
        documents = ListBuffer.empty,
        documentOpens = ListBuffer.empty
      )
      ctx.csBuilders += builder
      ctx.currentSearch = Some(Right(builder))
    } catch {
      case e: Exception =>
        ctx.addError("CARD_SEARCH_START", s"CARD_SEARCH_START: ${e.getMessage} в строке: $line")
    }
  }
}