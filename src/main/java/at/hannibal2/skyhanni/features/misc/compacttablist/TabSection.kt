package at.hannibal2.skyhanni.features.misc.compacttablist

import java.util.*

class TabSection(column: TabColumn) {

    var columnValue = column

    val lines: MutableList<String> = LinkedList()

    fun addLine(line: String) {
        lines.add(line)
    }

    fun size(): Int {
        return lines.size
    }
}