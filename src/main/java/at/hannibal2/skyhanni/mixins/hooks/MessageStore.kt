@file:Suppress("NoEmptyFile")

package at.hannibal2.skyhanni.mixins.hooks

//? if < 26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessage

interface MessageStore {

    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$getParent`(): GuiMessage? = throw UnsupportedOperationException("Implemented via mixin")

    @Suppress("FunctionName")
    fun `skyhanni$setParent`(parent: GuiMessage?) {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    companion object {

        var GuiMessage.Line.parent: GuiMessage?
            get() = (this as MessageStore).`skyhanni$getParent`()
            set(value) {
                (this as MessageStore).`skyhanni$setParent`(value)
            }
    }
}
*///?}
