package at.hannibal2.skyhanni.mixins.transformers

import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(RenderManager::class)
class MixinRenderManager {
    @Inject(method = ["shouldRender"], at = [At("HEAD")], cancellable = true)
    private fun shouldRender(
        entityIn: Entity,
        camera: ICamera,
        camX: Double,
        camY: Double,
        camZ: Double,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (CheckRenderEntityEvent(entityIn, camera, camX, camY, camZ).postAndCatch()) cir.returnValue = false
    }
}