package src

import org.apache.spark.sql.SparkSession
import src.parser.{SessionParser, ErrorCollector, ErrorJsonFormatter}
import src.analyzer.SessionAnalyzer
import java.io.{File, PrintWriter}
import scala.collection.JavaConverters._

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SessionProcess")
      .master("local[*]")
      .getOrCreate()

    val folderPath = "C:/Users/kubra/Documents/educational_task/Sessions"

    val sessionsAndReports = spark.sparkContext.wholeTextFiles(folderPath + "/*")
      .map { case (filePath, content) =>
        val baseName = new File(filePath).getName
        val (session, report) = SessionParser.parse(baseName, content)
        (filePath, session, report)
      }
      .cache()

    val sessionsRDD = sessionsAndReports.map(_._2)

    val globalCollector = sessionsAndReports.aggregate(new ErrorCollector())(
      (acc, tuple) => {
        val report = tuple._3
        report.errorMessages.foreach { msg =>
          val errorType = ErrorJsonFormatter.extractErrorType(msg)
          acc.add(errorType, msg)
        }
        acc
      },
      (acc1, acc2) => { acc1.merge(acc2); acc1 }
    )

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

    writeResults(outputDir, docId, cardSearchCount, collectedData)
    writeErrors(outputDir, globalCollector)

    spark.stop()
    println(s"Результаты сохранены в папку: ${outputDir.getAbsolutePath}")
  }

  private def writeResults(
                            outputDir: File,
                            docId: String,
                            cardSearchCount: Long,
                            collectedData: Array[(String, Iterable[(String, Int)])]
                          ): Unit = {
    val pw = new PrintWriter(new File(outputDir, "results.txt"))
    try {
      pw.println(s"Количество поисков документа $docId в карточках: $cardSearchCount")
      pw.println("\n=== Открытия документов (из быстрого поиска) по дням ===")
      collectedData.foreach { case (date, iter) =>
        pw.println(s"Дата $date:")
        iter.toList.sortBy(-_._2).foreach { case (id, cnt) =>
          pw.println(s"  $id: $cnt")
        }
      }
    } finally {
      pw.close()
    }
  }

  private def writeErrors(outputDir: File, collector: ErrorCollector): Unit = {
    val pwJson = new PrintWriter(new File(outputDir, "errors.json"))
    try {
      collector.getMessages.foreach(msg => pwJson.println(ErrorJsonFormatter.toJsonError("global", msg)))
    } finally {
      pwJson.close()
    }

    val pwStats = new PrintWriter(new File(outputDir, "errors_stats.txt"))
    try {
      pwStats.println(s"=== Общая статистика ошибок ===")
      pwStats.println(s"Всего ошибок: ${collector.getTotal}")
      val totalCounts = collector.getCounters.toList.sortBy(-_._2)
      pwStats.println("\n=== Общая статистика по типам ошибок ===")
      totalCounts.foreach { case (errorType, count) =>
        pwStats.println(s"  $errorType: $count")
      }
    } finally {
      pwStats.close()
    }
  }
}