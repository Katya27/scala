package com.parser

case class Session(
                    sessionId: String,
                    startTime: String,
                    endTime: String,
                    quickSearches: List[QuickSearch],
                    cardSearches: List[CardSearch]
                  )

case class QuickSearch(
                        timestamp: String,
                        query: String,
                        sessionId: String,
                        documents: List[String],
                        openedDocuments: List[OpenedDocument]
                      )

case class CardSearch(
                       startTime: String,
                       endTime: String,
                       sessionId: String,
                       param0: String,
                       param134: String,
                       documents: List[String],
                       openedDocuments: List[OpenedDocument]
                     )

case class OpenedDocument(
                           timestamp: String,
                           docId: String,
                           openSessionId: String
                         )