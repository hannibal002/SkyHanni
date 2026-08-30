package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore.Companion.setUsingCustomOutline
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity

object EntityRendererHook {
    @JvmStatic
    fun shouldAlsoGlow(entity: Entity, state: EntityRenderState, vanillaGlowing: Boolean): Boolean {
        if (RenderLivingEntityHelper.getEntityGlowColor(entity) == null) return vanillaGlowing

        state.setUsingCustomOutline()
        return true
    }
}
