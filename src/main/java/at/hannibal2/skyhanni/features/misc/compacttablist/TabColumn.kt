package at.hannibal2.skyhanni.features.misc.compacttablist

import net.minecraft.network.chat.Component

class TabColumn(val titleComponent: Component) {

    val components = mutableListOf<Component>()
    val sections = mutableListOf<TabSection>()

    fun addComponent(component: Component) {
        components.add(component)
    }

    fun removeLastComponent() {
        if (components.isNotEmpty()) components.removeAt(components.size - 1)
    }

    fun addSection(section: TabSection) {
        sections.add(section)
    }

    fun size() = components.size + 1
}
