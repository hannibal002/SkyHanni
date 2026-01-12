package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.events.minecraft.add
import net.minecraft.network.chat.Component

class TabColumn(val columnTitle: Component) {

    constructor(columnTitle: String) : this(Component.literal(columnTitle))

    val lines = mutableListOf<Component>()
    val sections = mutableListOf<TabSection>()

    fun addLine(line: Component) {
        lines.add(line)
    }

    fun addLine(line: String) {
        lines.add(line)
    }

    fun addSection(section: TabSection) {
        sections.add(section)
    }

    fun size() = lines.size + 1
}
