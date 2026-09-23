@file:Suppress("NoEmptyFile")

package at.hannibal2.skyhanni.mixins.hooks

//? if < 26.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState

// Naming is intentional
@Suppress("FunctionName")
interface GlowingStateStore {
    fun `skyhanni$isUsingCustomOutline`(): Boolean = throw UnsupportedOperationException("Implemented via mixin")

    fun `skyhanni$setUsingCustomOutline`() {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    companion object {
        fun EntityRenderState.setUsingCustomOutline() {
            (this as GlowingStateStore).`skyhanni$setUsingCustomOutline`()
        }
    }
}
*///?}
