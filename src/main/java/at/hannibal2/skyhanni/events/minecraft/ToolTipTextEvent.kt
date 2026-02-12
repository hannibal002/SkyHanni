package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class ToolTipTextEvent(val slot: Slot?, val itemStack: ItemStack, val toolTip: MutableList<Component>) : CancellableSkyHanniEvent()

fun MutableList<Component>.add(index: Int, string: String) {
    this.add(index, Component.literal(string))
}

fun MutableList<Component>.add(string: String) {
    this.add(Component.literal(string))
}

fun MutableList<Component>.addAll(strings: Collection<String>) {
    for (string in strings) {
        this.add(Component.literal(string))
    }
}

fun MutableList<Component>.addAll(index: Int, strings: Collection<String>) {
    val texts = strings.map { Component.literal(it) }
    this.addAll(index, texts)
}
