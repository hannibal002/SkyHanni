// Naming is intentional
@file:Suppress("FunctionName")

package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.world.entity.Entity

interface EntityRenderStateStore {
    fun `skyhanni$getEntity`(): Entity? = throw UnsupportedOperationException("Implemented via mixin")

    fun `skyhanni$setEntity`(value: Entity) {
        throw UnsupportedOperationException("Implemented via mixin")
    }
}
