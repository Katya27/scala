package com.parser

import scala.collection.mutable.ListBuffer

object SessionParser {

  def parse(content: String): Session = {
    val lines = content.split("\n").map(_.trim).filter(_.nonEmpty)
    var sessionId = ""
    var startTime = ""
    var endTime = ""

    var currentSearch: Option[Either[QuickSearchBuilder, CardSearchBuilder]] = None
    val qsBuilders = ListBuffer.empty[QuickSearchBuilder]
    val csBuilders = ListBuffer.empty[CardSearchBuilder]

    var i = 0
    while (i < lines.length) {
      val line = lines(i)

      if (line.startsWith("SESSION_START")) {
        val parts = line.split(" ")
        if (parts.length >= 2) startTime = parts(1)
      }
      else if (line.startsWith("SESSION_END")) {
        val parts = line.split(" ")
        if (parts.length >= 2) endTime = parts(1)
      }
      else if (line.startsWith("QS ")) {
        val parts = line.split(" ")
        if (parts.length >= 3) {
          val timestamp = parts(1)
          val queryStart = line.indexOf('{')
          val queryEnd = line.indexOf('}')
          val query = if (queryStart >= 0 && queryEnd > queryStart) line.substring(queryStart + 1, queryEnd) else ""

          i += 1
          if (i < lines.length) {
            val nextLine = lines(i)
            val tokens = nextLine.split("\\s+")
            if (tokens.nonEmpty) {
              val sessId = tokens.head
              val docs = tokens.tail.toList
              val builder = QuickSearchBuilder(timestamp, query, sessId, ListBuffer.empty ++= docs, ListBuffer.empty)
              qsBuilders += builder
              currentSearch = Some(Left(builder))
              if (sessionId.isEmpty) sessionId = sessId
            }
          }
        }
      }
      else if (line.startsWith("CARD_SEARCH_START")) {
        val parts = line.split(" ")
        if (parts.length >= 2) {
          val start = parts(1)
          val builder = CardSearchBuilder(start, "", "", "", "", ListBuffer.empty, ListBuffer.empty)
          csBuilders += builder
          currentSearch = Some(Right(builder))
        }
      }
      else if (line.startsWith("$134")) {
        val value = line.substring(4).trim
        currentSearch.foreach {
          case Right(csb) => csb.param134 = value
          case _ =>
        }
      }
      else if (line.startsWith("$0")) {
        val value = line.substring(2).trim
        currentSearch.foreach {
          case Right(csb) => csb.param0 = value
          case _ =>
        }
      }
      else if (line.startsWith("CARD_SEARCH_END")) {
        i += 1
        if (i < lines.length) {
          val nextLine = lines(i)
          val tokens = nextLine.split("\\s+")
          if (tokens.nonEmpty) {
            val sessId = tokens.head
            val docs = tokens.tail.toList
            currentSearch.foreach {
              case Right(csb) =>
                csb.sessionId = sessId
                csb.documents ++= docs
              case _ =>
            }
          }
        }
      }
      else if (line.startsWith("DOC_OPEN")) {
        val parts = line.split(" ")
        if (parts.length >= 3) {
          val (timestamp, openSessionId, docId) = if (parts.length >= 4) {
            (parts(1), parts(2), parts(3))
          } else {
            ("", parts(1), parts(2))
          }
          currentSearch.foreach {
            case Left(qsb) => qsb.openedDocuments += OpenedDocument(timestamp, docId, openSessionId)
            case Right(csb) => csb.openedDocuments += OpenedDocument(timestamp, docId, openSessionId)
          }
        }
      }

      i += 1
    }

    val quickSearches = qsBuilders.map(_.build()).toList
    val cardSearches = csBuilders.map(_.build()).toList
    Session(sessionId, startTime, endTime, quickSearches, cardSearches)
  }

  private case class QuickSearchBuilder(
                                         timestamp: String,
                                         query: String,
                                         sessionId: String,
                                         documents: ListBuffer[String],
                                         openedDocuments: ListBuffer[OpenedDocument]
                                       ) {
    def build(): QuickSearch = QuickSearch(timestamp, query, sessionId, documents.toList, openedDocuments.toList)
  }

  private case class CardSearchBuilder(
                                        startTime: String,
                                        var endTime: String,
                                        var sessionId: String,
                                        var param0: String,
                                        var param134: String,
                                        documents: ListBuffer[String],
                                        openedDocuments: ListBuffer[OpenedDocument]
                                      ) {
    def build(): CardSearch = CardSearch(startTime, endTime, sessionId, param0, param134, documents.toList, openedDocuments.toList)
  }
}