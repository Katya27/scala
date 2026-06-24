package com.analyzer

import com.parser.Session
import org.apache.spark.rdd.RDD

object SessionAnalyzer {

  def countDocumentOpensByDayFromQS(sessionsRDD: RDD[Session]): RDD[((String, String), Int)] = {
    sessionsRDD.flatMap { session =>
      session.quickSearches.flatMap { qs =>
        qs.openedDocuments
          .filter(opened => qs.documents.contains(opened.docId))
          .map { opened =>
            val date = if (opened.timestamp != null && opened.timestamp.nonEmpty && opened.timestamp.contains("_")) {
              opened.timestamp.split("_")(0)
            } else {
              qs.timestamp.split("_")(0)
            }
            ((date, opened.docId), 1)
          }
      }
    }.reduceByKey(_ + _)
  }

  def countCardSearchForDocument(sessionsRDD: RDD[Session], docId: String): Long = {
    sessionsRDD.flatMap(_.cardSearches)
      .filter { cs =>
        cs.param0.contains(docId) || cs.param134.contains(docId)
      }.count()
  }
}