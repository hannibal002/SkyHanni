package at.hannibal2.skyhanni.features.misc.compacttablist

import net.minecraft.network.chat.Component

class TabSection(val columnValue: TabColumn) {

    val lines = mutableListOf<Component>()

    fun addLine(line: Component) {
        lines.add(line)
    }

    fun size() = lines.size
}
