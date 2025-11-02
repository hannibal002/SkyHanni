package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.utils.EntityOutlineRenderer
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.Frustum
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

class RenderGlobalHook {

    fun renderEntitiesOutlines(camera: Frustum?, partialTicks: Float): Boolean {
        val vec = MinecraftClient.getInstance().getCameraEntity()?.let {
            WorldRenderUtils.exactLocation(it, partialTicks)
        } ?: return false
        return EntityOutlineRenderer.renderEntityOutlines(camera!!, partialTicks, vec)
    }

    fun shouldRenderEntityOutlines(cir: CallbackInfoReturnable<Boolean?>) {
        if (EntityOutlineRenderer.shouldRenderEntityOutlines()) {
            cir.returnValue = true
        }
    }
}
