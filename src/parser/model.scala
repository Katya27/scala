package src.parser

import java.time.LocalDateTime

case class Session(
                    sessionId: Int,
                    startTime: LocalDateTime,
                    endTime: LocalDateTime,
                    quickSearches: List[QuickSearch],
                    cardSearches: List[CardSearch]
                  )

case class QuickSearch(
                        datetime: LocalDateTime,
                        query: String,
                        searchId: Int,
                        documents: List[String],
                        documentOpens: List[DocumentOpen]
                      )

case class CardSearch(
                       startTime: LocalDateTime,
                       searchId: Int,
                       params: Map[String, String],
                       documents: List[String],
                       documentOpens: List[DocumentOpen]
                     )

case class DocumentOpen(
                         timestamp: LocalDateTime,
                         docId: String,
                         searchId: Int
                       )

case class ParseReport(
                        errorCount: Int,
                        errorTypes: List[String],
                        errorMessages: List[String]
                      )