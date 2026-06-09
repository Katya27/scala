import java.io.File
import scala.io.Source
import scala.collection.mutable.ListBuffer

object QuickTest {
  def main(args: Array[String]): Unit = {
    val folder = "Sessions"
    val targetDoc = "ACC_45616"
    
    val dir = new File(folder)
    val files = dir.listFiles().toList
    
    var foundCount = 0
    var foundFiles = ""
    
	println(s"Sessions Card Search with document ID '$targetDoc'")
	
    files.foreach { file =>
      try {
        val source = Source.fromFile(file, "windows-1251")
        val lines = source.getLines().toList
        source.close()
        
        if (!lines.exists(_.contains("CARD_SEARCH_START")) || !lines.exists(_.contains("CARD_SEARCH_END"))) {
          // Пропуск файла
        } else {
          // Обработка файла
		  foundFiles = file.getName
          
          var inCard = false
          var currentCardLines = ListBuffer[String]()
          
          for (i <- lines.indices) {
            val line = lines(i)
            
            // Начало карточки
            if (line.contains("CARD_SEARCH_START")) {
              inCard = true
              currentCardLines.clear()
              currentCardLines += line
            }
            // Конец карточки
            else if (line.contains("CARD_SEARCH_END") && inCard) {
              currentCardLines += line
              inCard = false
              
              // Проверка карточки на наличие нужного документа
              var documentFound = false
              var searchQuery = "(no query)"
              
              // Поиск $0 с нужным документом
              for (l <- currentCardLines) {
                if (l.contains("$0") && l.contains(targetDoc)) {
                  documentFound = true
                }
              }
              
              // Если нашелся документ - поиск запроса $134
              if (documentFound) {
                for (l <- currentCardLines) {
                  if (l.contains("$134")) {
                    val parts = l.split("\\$134", 2)
                    if (parts.length > 1) {
                      searchQuery = parts(1).trim
                    }
                  }
                }
                
                foundCount += 1
                println(s"Found Sessions '$foundFiles'")
                println(s"Query: $searchQuery")
              }
            }
            // Строки внутри карточки
            else if (inCard) {
              currentCardLines += line
            }
          }
        }
        
      } catch {
        case e: Exception => 
          println(s"Error reading ${file.getName}: ${e.getMessage}")
      }
    }
    
    println("\n" + "=" * 60)
    println(s"RESULT: document ID '$targetDoc' was searched: $foundCount times")
  }
}

QuickTest.main(Array())