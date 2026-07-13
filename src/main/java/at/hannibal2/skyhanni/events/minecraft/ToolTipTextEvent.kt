package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot

/**
 * Event that is called when the tooltip text of an item is being generated.
 * This event is cancellable, meaning that you can cancel it to prevent the tooltip from being shown.
 * You can also modify the tooltip by adding or removing lines from the `toolTip` list.
 *
 * This event is supposed to only be called from the render thread, but due to other mods like JEI
 * needing to search using tooltips, it can be called from other threads as well.
 *
 * @param slot The slot that the item is in, or null if the item is not in a slot.
 * @param itemStack The item stack that the tooltip is being generated for.
 * @param toolTip The list of lines that will be shown in the tooltip. You can modify this list to change the tooltip.
 */
@Thread(ANY)
@PrimaryFunction("onToolTip")
class ToolTipTextEvent(val slot: Slot?, val itemStack: SafeItemStack, val toolTip: MutableList<Component>) : CancellableSkyHanniEvent()

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
