@file:Suppress("NoEmptyFile")

package at.hannibal2.skyhanni.mixins.hooks

//? if < 26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessage

interface MessageStore {

    fun `skyhanni$getParent`(): GuiMessage? = throw UnsupportedOperationException("Implemented via mixin")

    fun `skyhanni$setParent`(parent: GuiMessage?) {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    // Kotlin-only accessor
    @get:JvmSynthetic
    @set:JvmSynthetic
    var parent: GuiMessage?
        get() = `skyhanni$getParent`()
        set(parent) { `skyhanni$setParent`(parent) }
}
*///?}
