package at.hannibal2.skyhanni.mixins.transformers.renderer

import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase
import org.objectweb.asm.Opcodes
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(RendererLivingEntity::class)
abstract class MixinRendererLivingEntity<T : EntityLivingBase?> protected constructor(renderManager: RenderManager?) :
    Render<T>(renderManager) {
    @Inject(method = ["getColorMultiplier"], at = [At("HEAD")], cancellable = true)
    private fun setColorMultiplier(
        entity: T,
        lightBrightness: Float,
        partialTickTime: Float,
        cir: CallbackInfoReturnable<Int>
    ) {
        val event = RenderMobColoredEvent(entity as EntityLivingBase, 0)
        event.postAndCatch()
        cir.returnValue = event.color
    }

    @Redirect(
        method = ["setBrightness"],
        at = At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/EntityLivingBase;hurtTime:I",
            opcode = Opcodes.GETFIELD
        )
    )
    private fun changeHurtTime(entity: EntityLivingBase): Int {
        val event = ResetEntityHurtEvent(entity, false)
        event.postAndCatch()
        return if (event.shouldReset) 0 else entity.hurtTime
    }
}