package at.hannibal2.skyhanni.features.misc.compacttablist

class TabSection(column: TabColumn) {

    var columnValue = column

    val lines = mutableListOf<String>()

    fun addLine(line: String) {
        lines.add(line)
    }

    fun size(): Int {
        return lines.size
    }
}