//? if < 26.1 {
/*package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.multiplayer.chat.GuiMessage

interface MessageStore {

    fun `skyhanni$getParent`(): GuiMessage? = throw UnsupportedOperationException("Implemented via mixin")

    fun `skyhanni$setParent`(parent: GuiMessage?): Unit {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    var `skyhanni$parent`: GuiMessage?
        get() = `skyhanni$getParent`()
        set(parent) { `skyhanni$setParent`(parent) }
}
*///?}
