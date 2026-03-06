package at.hannibal2.skyhanni.features.misc.compacttablist

import net.minecraft.network.chat.Component

class TabColumn(val titleComponent: Component) {

    val components = mutableListOf<Component>()
    val sections = mutableListOf<TabSection>()

    fun addComponent(component: Component) {
        components.add(component)
    }

    fun addSection(section: TabSection) {
        sections.add(section)
    }

    fun size() = components.size + 1
}
