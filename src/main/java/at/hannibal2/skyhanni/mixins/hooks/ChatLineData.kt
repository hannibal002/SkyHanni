package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.util.IChatComponent

interface ChatLineData {
    @Suppress("ktlint:standard:property-naming")
    var skyHanni_getFullComponent: IChatComponent?
}
