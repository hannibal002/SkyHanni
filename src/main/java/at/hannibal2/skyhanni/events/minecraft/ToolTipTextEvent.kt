package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.text.Text
import net.minecraft.screen.slot.Slot
import net.minecraft.item.ItemStack

class ToolTipTextEvent(val slot: Slot?, val itemStack: ItemStack, val toolTip: MutableList<Text>) : CancellableSkyHanniEvent()

fun MutableList<Text>.add(index: Int, string: String) {
    this.add(index, Text.of(string))
}

fun MutableList<Text>.add(string: String) {
    this.add(Text.of(string))
}

fun MutableList<Text>.addAll(strings: Collection<String>) {
    for (string in strings) {
        this.add(Text.of(string))
    }
}

fun MutableList<Text>.addAll(index: Int, strings: Collection<String>) {
    val texts = strings.map { Text.of(it) }
    this.addAll(index, texts)
}
