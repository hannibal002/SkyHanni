package at.hannibal2.skyhanni.features.misc.compacttablist


import java.util.*

class TabColumn(title: String) {

    var columnTitle = title
    val lines: MutableList<String> = LinkedList()
    val sections: MutableList<TabSection> = LinkedList<TabSection>()

    fun addLine(line: String) {
        lines.add(line)
    }

    fun addSection(section: TabSection) {
        sections.add(section)
    }

    fun size(): Int {
        return lines.size + 1
    }
}