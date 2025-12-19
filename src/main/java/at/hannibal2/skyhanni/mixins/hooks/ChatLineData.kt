package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.text.Text

interface ChatLineData {
    @Suppress("VariableNaming", "PropertyName")
    var skyHanni_fullComponent: Text
}
