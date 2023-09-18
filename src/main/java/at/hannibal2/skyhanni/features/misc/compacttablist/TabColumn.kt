package at.hannibal2.skyhanni.features.misc.compacttablist


class TabColumn(title: String) {

    var columnTitle = title
    val lines = mutableListOf<String>()
    val sections = mutableListOf<TabSection>()

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