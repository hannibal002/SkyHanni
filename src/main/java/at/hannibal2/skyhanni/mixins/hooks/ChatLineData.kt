package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.util.IChatComponent

interface ChatLineData {
    @Suppress("VariableNaming", "PropertyName")
    var skyHanni_fullComponent: IChatComponent
}
