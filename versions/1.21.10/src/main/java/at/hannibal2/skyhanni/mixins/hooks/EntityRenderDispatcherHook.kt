package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.entity.Entity

private var savedEntity: Entity? = null

fun setEntity(state: EntityRenderState?) {
    if (state !is EntityRenderStateStore) return
    savedEntity = state.`skyhanni$getEntity`()
}

fun getEntity(): Entity? {
    return savedEntity
}

fun clearEntity() {
    savedEntity = null
}
