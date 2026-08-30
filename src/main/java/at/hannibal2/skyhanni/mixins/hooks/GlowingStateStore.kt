package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.renderer.entity.state.EntityRenderState

interface GlowingStateStore {
    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$isUsingCustomOutline`(): Boolean = throw UnsupportedOperationException("Implemented via mixin")

    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$setUsingCustomOutline`() {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    companion object {
        fun EntityRenderState.setUsingCustomOutline() {
            (this as GlowingStateStore).`skyhanni$setUsingCustomOutline`()
        }
    }
}
