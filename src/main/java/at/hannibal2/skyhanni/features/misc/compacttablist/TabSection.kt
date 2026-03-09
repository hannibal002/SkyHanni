package at.hannibal2.skyhanni.features.misc.compacttablist

import net.minecraft.network.chat.Component

class TabSection(val columnValue: TabColumn) {

    val components = mutableListOf<Component>()

    fun addComponent(component: Component) {
        components.add(component)
    }

    fun size() = components.size
}
