package src.parser

trait EventHandler {
  def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit
}