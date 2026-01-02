package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.network.chat.Component

interface ChatLineData {
    @Suppress("VariableNaming", "PropertyName")
    var skyHanni_fullComponent: Component
}
