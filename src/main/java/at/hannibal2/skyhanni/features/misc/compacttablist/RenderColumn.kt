package at.hannibal2.skyhanni.features.misc.compacttablist

class RenderColumn {

    val lines = mutableListOf<TabLine>()

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