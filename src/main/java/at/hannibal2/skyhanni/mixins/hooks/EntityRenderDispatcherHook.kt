package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity

object EntityRenderDispatcherHook {
    private var savedEntityRenderState: EntityRenderState? = null

    @JvmStatic
    fun setEntity(state: EntityRenderState?) {
        if (state == null) return
        savedEntityRenderState = state
    }

    @JvmStatic
    fun getEntity(): Entity? = savedEntityRenderState?.let { SkyHanniRenderStateData.getEntity(it) }

    @JvmStatic
    fun getEntityTransparency(): Int? = savedEntityRenderState?.let { SkyHanniRenderStateData.getEntityTransparency(it) }

    @JvmStatic
    fun getEntityRenderState(): EntityRenderState? = savedEntityRenderState

    @JvmStatic
    fun clearEntity() {
        savedEntityRenderState = null
    }
}
