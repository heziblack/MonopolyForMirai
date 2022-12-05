import io.github.evanrupert.excelkt.workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

fun main(){
    val l = 123L
    val i = l.toInt()
    println(i)
//    io.github.evanrupert.excelkt.Workbook(XSSFWorkbook("test.xlsx"),null)
}

private fun test04() {
    val originStr = "dsagjikg[m5465435]dsadashgj"
    val r = Regex("""\[m\d+]""")
    val match =
        r.replace(originStr) {
            val a = it.value
            val replaced = a.substring(2, a.length - 1)
            println(a.substring(2, a.length - 1))
            replaced
        }
    println(match)
}

private fun test03() {
    val al01 = arrayListOf<Long>(1651, 564, 74897, 16543, 489646)
    val a = 16543L
    println(a in al01)
}

private fun test02() {
    workbook {
        sheet {
            row {
                cell("helloworld")
            }
        }
    }.write("test.xlsx")
}

fun test01() {
    val t = "  125\n\t\r".replace(Regex("""\s"""), "")
    println(t)
    val i = "-012".toInt()
    println(i)
}