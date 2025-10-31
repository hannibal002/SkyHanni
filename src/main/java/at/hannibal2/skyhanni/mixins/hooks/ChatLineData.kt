package at.hannibal2.hanni.mixins.hooks

import net.minecraft.util.IChatComponent

interface ChatLineData {
    @Suppress("VariableNaming", "PropertyName")
    var hanni_fullComponent: IChatComponent
}
