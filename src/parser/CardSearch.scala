package src.parser

import scala.collection.mutable.{ListBuffer, Map => MutableMap}
import java.time.LocalDateTime

case class CardSearch(
                       startTime: LocalDateTime,
                       searchId: Int,
                       params: Map[String, String],
                       documents: List[String],
                       documentOpens: List[DocumentOpen]
                     )

case class CardSearchBuilder(
                              startTime: LocalDateTime,
                              var searchId: Int,
                              params: MutableMap[String, String],
                              documents: ListBuffer[String],
                              documentOpens: ListBuffer[DocumentOpen]
                            ) {
  def build(): CardSearch =
    CardSearch(startTime, searchId, params.toMap, documents.toList, documentOpens.toList)
}

class CardSearchHandler extends EventHandler {
  override def handle(line: String, ctx: ParsingContext, iter: Iterator[String]): Unit = {
    try {
      val parts = line.split(" ")
      val startTime = DateTimeParser.parse(parts(1), ctx.errorCollector).getOrElse(LocalDateTime.MIN)

      val builder = CardSearchBuilder(
        startTime = startTime,
        searchId = 0,
        params = MutableMap.empty,
        documents = ListBuffer.empty,
        documentOpens = ListBuffer.empty
      )

      var foundEnd = false
      while (!foundEnd && iter.hasNext) {
        val nextLine = iter.next()
        if (nextLine.startsWith("$")) {
          val dollarIdx = nextLine.indexOf('$')
          val rest = nextLine.substring(dollarIdx + 1)
          val spaceIdx = rest.indexOf(' ')
          if (spaceIdx > 0) {
            val key = rest.substring(0, spaceIdx).trim
            val value = rest.substring(spaceIdx + 1).trim
            builder.params += (key -> value)
          } else {
            ctx.addError("CARD_SEARCH", s"Неверный формат параметра: $nextLine")
          }
        } else if (nextLine.startsWith("CARD_SEARCH_END")) {
          foundEnd = true
        } else {
          ctx.addError("CARD_SEARCH", s"Неизвестная строка внутри карточного поиска: $nextLine")
        }
      }

      if (!foundEnd) {
        ctx.addError("CARD_SEARCH", s"Не найдена CARD_SEARCH_END для карточного поиска, начатого: $line")
        return
      }

      if (iter.hasNext) {
        val dataLine = iter.next()
        val tokens = dataLine.split("\\s+")
        if (tokens.nonEmpty) {
          val searchId = tokens.head.toInt
          val docs = tokens.tail.toList
          builder.searchId = searchId
          builder.documents ++= docs
        } else {
          ctx.addError("CARD_SEARCH", s"Пустая строка после CARD_SEARCH_END: $dataLine")
        }
      } else {
        ctx.addError("CARD_SEARCH", "Нет строки с данными после CARD_SEARCH_END")
      }

      ctx.csBuilders += builder
      ctx.currentSearch = Some(Right(builder))
    } catch {
      case e: Exception =>
        ctx.addError("CARD_SEARCH", s"Ошибка парсинга карточного поиска: ${e.getMessage} в строке: $line")
    }
  }
}