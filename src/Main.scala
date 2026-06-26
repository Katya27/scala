package src

import org.apache.spark.sql.SparkSession
import src.parser.SessionParser
import src.analyzer.SessionAnalyzer
import org.apache.spark.util.{LongAccumulator, CollectionAccumulator}
import java.io.{File, PrintWriter}
import scala.collection.JavaConverters._

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SessionProcess")
      .master("local[*]")
      .getOrCreate()

    val errorCountAcc = spark.sparkContext.longAccumulator("errorCount")
    val errorMessagesAcc = spark.sparkContext.collectionAccumulator[String]("errorMessages")
    val errorTypesAcc = spark.sparkContext.collectionAccumulator[String]("errorTypes")

    val folderPath = "C:/Users/kubra/Documents/educational_task/Sessions"

    val sessionsRDD = spark.sparkContext.wholeTextFiles(folderPath + "/*")
      .map { case (filePath, content) =>
        val baseName = new File(filePath).getName
        val (session, report) = SessionParser.parse(baseName, content)

        errorCountAcc.add(report.errorCount)

        report.errorMessages.foreach { msg =>
          val json = toJsonError(filePath, msg)
          errorMessagesAcc.add(json)
        }

        report.errorTypes.foreach { errorType =>
          errorTypesAcc.add(errorType)
        }

        (filePath, session)
      }
      .values
      .cache()

    val docId = "ACC_45616"
    val cardSearchCount = SessionAnalyzer.countCardSearchForDocument(sessionsRDD, docId)

    val opensByDay = SessionAnalyzer.countDocumentOpensByDayFromQS(sessionsRDD)
    val collectedData = opensByDay
      .map { case ((date, docId), count) => (date, (docId, count)) }
      .groupByKey()
      .collect()
      .sortBy(_._1)

    val outputDir = new File("output")
    if (!outputDir.exists()) outputDir.mkdirs()

    val pwResults = new PrintWriter(new File(outputDir, "results.txt"))
    try {
      pwResults.println(s"Количество поисков документа $docId в карточках: $cardSearchCount")

      pwResults.println("\n=== Открытия документов (из быстрого поиска) по дням ===")
      collectedData.foreach { case (date, iter) =>
        pwResults.println(s"Дата $date:")
        iter.toList
          .sortBy(-_._2)
          .foreach { case (docId, count) =>
            pwResults.println(s"  $docId: $count")
          }
      }
    } finally {
      pwResults.close()
    }

    val pwErrorsJson = new PrintWriter(new File(outputDir, "errors.json"))
    try {
      errorMessagesAcc.value.asScala.foreach(jsonLine => pwErrorsJson.println(jsonLine))
    } finally {
      pwErrorsJson.close()
    }

    val pwStats = new PrintWriter(new File(outputDir, "errors_stats.txt"))
    try {
      pwStats.println(s"=== Общая статистика ошибок ===")
      pwStats.println(s"Всего ошибок: ${errorCountAcc.value}")

      val allTypes = errorTypesAcc.value.asScala.toList
      val totalCounts = allTypes.groupBy(identity).mapValues(_.size).toList.sortBy(-_._2)
      pwStats.println("\n=== Общая статистика по типам ошибок ===")
      totalCounts.foreach { case (errorType, count) =>
        pwStats.println(s"  $errorType: $count")
      }
    } finally {
      pwStats.close()
    }

    spark.stop()
    println(s"Результаты сохранены в файлы:\n" +
      s"  ${outputDir.getAbsolutePath}/results.txt\n" +
      s"  ${outputDir.getAbsolutePath}/errors.json\n" +
      s"  ${outputDir.getAbsolutePath}/errors_stats.txt")
  }


  private def escapeJson(str: String): String = {
    str.replace("\\", "\\\\").replace("\"", "\\\"")
  }

  private def extractErrorType(msg: String): String = {
    val colonIdx = msg.indexOf(':')
    if (colonIdx > 0) msg.substring(0, colonIdx).trim else "UNKNOWN"
  }


  private def toJsonError(filePath: String, msg: String): String = {
    val errorType = extractErrorType(msg)
    val escapedMsg = escapeJson(msg)
    val escapedFile = escapeJson(filePath)
    val timestamp = java.time.Instant.now().toString
    s"""{"timestamp":"$timestamp","file":"$escapedFile","errorType":"$errorType","message":"$escapedMsg"}"""
  }
}