package at.hannibal2.hanni.features.misc.compacttablist

class TabSection(val columnValue: TabColumn) {

    val lines = mutableListOf<String>()

    fun addLine(line: String) {
        lines.add(line)
    }

    fun size() = lines.size
}
