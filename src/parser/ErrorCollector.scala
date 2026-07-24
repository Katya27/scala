package src.parser

import java.io.{PrintWriter, StringWriter}

case class ParseReport(
                        errorCount: Int,
                        errorTypes: List[String],
                        errorMessages: List[String]
                      )

class ErrorCollector(val maxMessages: Int = 100) extends Serializable {
  private var totalCount: Int = 0
  private var counters: Map[String, Int] = Map.empty
  private var messages: List[String] = Nil

  def add(errorType: String, msg: String): Unit = {
    totalCount += 1
    counters = counters.updated(errorType, counters.getOrElse(errorType, 0) + 1)
    if (messages.size < maxMessages) messages = msg :: messages
  }

  def addWithStack(errorType: String, msg: String, t: Throwable): Unit = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    t.printStackTrace(pw)
    pw.close()
    add(errorType, s"$msg\nStack trace:\n${sw.toString}")
  }

  def merge(other: ErrorCollector): Unit = {
    this.totalCount += other.totalCount
    other.counters.foreach { case (k, v) =>
      this.counters = this.counters.updated(k, this.counters.getOrElse(k, 0) + v)
    }
    this.messages = (this.messages ++ other.messages).take(maxMessages)
  }

  def getTotal: Int = totalCount
  def getCounters: Map[String, Int] = counters
  def getMessages: List[String] = messages
  def getReport: ParseReport = ParseReport(totalCount, counters.keys.toList, messages)
}

object ErrorJsonFormatter {
  private def escapeJson(str: String): String =
    str.replace("\\", "\\\\").replace("\"", "\\\"")

  def extractErrorType(msg: String): String = {
    val colonIdx = msg.indexOf(':')
    if (colonIdx > 0) msg.substring(0, colonIdx).trim else "UNKNOWN"
  }

  def toJsonError(filePath: String, msg: String): String = {
    val errorType = extractErrorType(msg)
    val escapedMsg = escapeJson(msg)
    val escapedFile = escapeJson(filePath)
    val timestamp = java.time.Instant.now().toString
    s"""{"timestamp":"$timestamp","file":"$escapedFile","errorType":"$errorType","message":"$escapedMsg"}"""
  }
}