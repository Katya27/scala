import java.io.File
import scala.io.Source
import java.io.PrintWriter
import scala.collection.mutable.Map

object QS {
  def main(args: Array[String]): Unit = {
    
    val count = Map[(String, String), Int]()
    
    new File("Sessions").listFiles().foreach { f =>
      try {
        val lines = Source.fromFile(f, "windows-1251").getLines().toList
        
        var i = 0
        while (i < lines.length) {
          if (lines(i).startsWith("QS ")) {
            val qsDate = lines(i).split(" ")(1).split("_")(0)
            
            var j = i + 1
            while (j < lines.length && 
                   !lines(j).startsWith("QS ") && 
                   !lines(j).startsWith("SESSION_END") && 
                   !lines(j).startsWith("CARD_SEARCH_START")) {
              
              if (lines(j).startsWith("DOC_OPEN ")) {
                val w = lines(j).split(" ")
                if (w.length >= 4) {
                  val doc = w(3)
                  var date = qsDate
                  
                  val d = w(1)
                  if (d != "" && d.contains("_")) {
                    val dd = d.split("_")(0)
                    if (dd.length == 10) date = dd
                  }
                  
                  val key = (date, doc)
                  count(key) = count.getOrElse(key, 0) + 1
                }
              }
              j = j + 1
            }
          }
          i = i + 1
        }
      } catch { case _: Exception => }
    }
    
    val out = new PrintWriter("task2_output.txt", "UTF-8")
    
    out.println("=" * 60)
    out.println("Result:")
    out.println("=" * 60)
    
    for (((d, doc), c) <- count.toList.sortBy { case ((_, doc), _) => doc }) {
      out.println(s"  $doc -- $c times ($d)")
    }
    
    out.close()
    
    println("Result is uploaded to a file task2_output.txt")
  }
}

QS.main(Array())