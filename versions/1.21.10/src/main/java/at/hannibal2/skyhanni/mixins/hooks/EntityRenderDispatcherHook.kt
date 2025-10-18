package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.entity.Entity

private var savedEntity: Entity? = null
private var savedEntityRenderState: EntityRenderStateStore? = null

fun setEntity(state: EntityRenderState?) {
    if (state !is EntityRenderStateStore) return
    savedEntity = state.`skyhanni$getEntity`()
    savedEntityRenderState = state
}

fun getEntity(): Entity? {
    return savedEntity
}

fun getEntityRenderState(): EntityRenderStateStore? {
    return savedEntityRenderState
}

fun clearEntity() {
    savedEntity = null
    savedEntityRenderState = null
}
