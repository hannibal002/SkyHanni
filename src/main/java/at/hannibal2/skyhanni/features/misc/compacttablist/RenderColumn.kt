package at.hannibal2.skyhanni.features.misc.compacttablist

import java.util.*

class RenderColumn {

    val lines: MutableList<TabLine> = LinkedList<TabLine>()

    fun size(): Int {
        return lines.size
    }

    fun addLine(line: TabLine) {
        lines.add(line)
    }

    fun getMaxWidth(): Int {
        var maxWidth = 0
        for (tabLine in lines) {
            maxWidth = maxWidth.coerceAtLeast(tabLine.getWidth())
        }
        return maxWidth
    }
}