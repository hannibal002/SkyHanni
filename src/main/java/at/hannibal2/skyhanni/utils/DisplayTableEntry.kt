package at.hannibal2.skyhanni.utils

import net.minecraft.network.chat.Component

class DisplayTableEntry(
    val left: Component,
    val right: Component,
    val sort: Number,
    val item: NeuInternalName,
    val hover: List<Component> = emptyList(),
    val highlightsOnHoverSlots: List<Int> = emptyList(),
)
