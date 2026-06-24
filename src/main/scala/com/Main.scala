package com

import org.apache.spark.sql.SparkSession
import com.parser.SessionParser
import com.analyzer.SessionAnalyzer
import java.io.{File, PrintWriter}

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SessionProcess")
      .master("local[*]")
      .getOrCreate()

    val folderPath = "C:/Users/kubra/Documents/educational_task/Sessions"

    val sessionsRDD = spark.sparkContext.wholeTextFiles(folderPath + "/*")
      .map { case (_, content) => SessionParser.parse(content) }

    // 1. Количество поисков документа ACC_45616 в карточках
    val docId = "ACC_45616"
    val cardSearchCount = SessionAnalyzer.countCardSearchForDocument(sessionsRDD, docId)

    // 2. Открытия документов из QS по дням
    val opensByDay = SessionAnalyzer.countDocumentOpensByDayFromQS(sessionsRDD)
    val collectedData = opensByDay
      .map { case ((date, docId), count) => (date, (docId, count)) }
      .groupByKey()
      .collect()
      .sortBy(_._1) // сортировка по дате

    // Создаём папку output, если её нет
    val outputDir = new File("output")
    if (!outputDir.exists()) outputDir.mkdirs()

    // Запись в файл
    val pw = new PrintWriter(new File(outputDir, "results.txt"))
    try {
      pw.println(s"Количество поисков документа $docId в карточках: $cardSearchCount")

      pw.println("\n=== Открытия документов (из быстрого поиска) по дням ===")
      collectedData.foreach { case (date, iter) =>
        pw.println(s"Дата $date:")
        iter.toList
          .sortBy(-_._2) // сортировка по убыванию количества
          .foreach { case (docId, count) =>
            pw.println(s"  $docId: $count")
          }
      }
    } finally {
      pw.close()
    }

    spark.stop()
    println(s"Результаты сохранены в файл: ${outputDir.getAbsolutePath}/results.txt")
  }
}