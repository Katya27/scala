package src.analyzer

import src.parser.Session
import org.apache.spark.rdd.RDD

object SessionAnalyzer {
  def countDocumentOpensByDayFromQS(sessionsRDD: RDD[Session]): RDD[((String, String), Int)] = {
    sessionsRDD.flatMap { session =>
      session.quickSearches.flatMap { qs =>
        qs.documentOpens
          .map { opened =>
            val date = opened.timestamp.toLocalDate.toString
            ((date, opened.docId), 1)
          }
      }
    }.reduceByKey(_ + _)
  }

  def countCardSearchForDocument(sessionsRDD: RDD[Session], docId: String): Long = {
    sessionsRDD.flatMap(_.cardSearches)
      .filter { cs =>
        cs.params.values.exists(_.contains(docId))
      }
      .count()
  }
}