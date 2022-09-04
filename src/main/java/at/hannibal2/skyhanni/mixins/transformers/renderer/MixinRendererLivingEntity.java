package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.events.RenderMobColoredEvent;
import at.hannibal2.skyhanni.events.ResetEntityHurtTimeEvent;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {
    protected MixinRendererLivingEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "getColorMultiplier", at = @At("HEAD"), cancellable = true)
    private void setColorMultiplier(T entity, float lightBrightness, float partialTickTime, CallbackInfoReturnable<Integer> cir) {
        RenderMobColoredEvent event = new RenderMobColoredEvent(entity, 0);
        event.postAndCatch();
        cir.setReturnValue(event.getColor());
    }

    @Redirect(method = "setBrightness", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;hurtTime:I", opcode = Opcodes.GETFIELD))
    private int changeHurtTime(EntityLivingBase entity) {
        ResetEntityHurtTimeEvent event = new ResetEntityHurtTimeEvent(entity, false);
        event.postAndCatch();
        return event.getShouldReset() ? 0 : entity.hurtTime;
    }
}