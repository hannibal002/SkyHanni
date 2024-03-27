package at.hannibal2.skyhanni.features.gui.compacttablist

class RenderColumn(val lines: MutableList<TabLine> = mutableListOf()) {

    fun size(): Int {
        return lines.size
    }

    fun addLine(line: TabLine) {
        lines.add(line)
    }

    fun getMaxWidth(): Int {
        return lines.maxOfOrNull { it.getWidth() } ?: 0
    }
}
