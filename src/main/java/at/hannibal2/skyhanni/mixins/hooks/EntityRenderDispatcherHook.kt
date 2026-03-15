package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.Collections
import java.util.IdentityHashMap

private var savedEntity: Entity? = null
private var savedEntityRenderState: EntityRenderState? = null

val activeHolographicEntities: MutableSet<LivingEntity> =
    Collections.newSetFromMap(IdentityHashMap())

fun setEntity(state: EntityRenderState?) {
    if (state == null) return
    savedEntity = state.`skyhanni$getEntity`()
    savedEntityRenderState = state
}

fun getEntity(): Entity? = savedEntity

fun getEntityRenderState(): EntityRenderState? = savedEntityRenderState

fun clearEntity() {
    savedEntity = null
    savedEntityRenderState = null
}
