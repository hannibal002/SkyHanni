package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.world.entity.Entity

//~ if < 26.2 '{' -> ': GlowingStateStore {'
interface EntityRenderStateStore {
    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$getEntity`(): Entity? = throw UnsupportedOperationException("Implemented via mixin")

    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$setEntity`(value: Entity) {
        throw UnsupportedOperationException("Implemented via mixin")
    }
}
