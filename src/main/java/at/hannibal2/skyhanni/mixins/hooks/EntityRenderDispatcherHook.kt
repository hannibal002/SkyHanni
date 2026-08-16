package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity

private var savedEntityRenderState: EntityRenderState? = null

fun setEntity(state: EntityRenderState?) {
    if (state == null) return
    savedEntityRenderState = state
}

fun getEntity(): Entity? = savedEntityRenderState?.let { SkyHanniRenderStateData.getEntity(it) }

fun getEntityTransparency(): Int? = savedEntityRenderState?.let { SkyHanniRenderStateData.getEntityTransparency(it) }

fun getEntityRenderState(): EntityRenderState? = savedEntityRenderState

fun clearEntity() {
    savedEntityRenderState = null
}
