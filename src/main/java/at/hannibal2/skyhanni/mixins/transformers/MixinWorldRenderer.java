package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? > 1.21.8 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
*///?}

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    //? < 1.21.9 {
    @Inject(method = "collectVisibleEntities", at = @At(value = "HEAD"))
    public void resetRealGlowing(CallbackInfoReturnable<Boolean> cir) {
        //?} else {
        /*@Inject(method = "extractVisibleEntities", at = @At(value = "HEAD"))
        public void resetRealGlowing(CallbackInfo ci) {
        *///?}
        RenderLivingEntityHelper.check();
        RenderEntityOutlineEvent noXrayOutlineEvent = new RenderEntityOutlineEvent(RenderEntityOutlineEvent.Type.NO_XRAY, null);
        RenderLivingEntityHelper.setCurrentGlowEvent(noXrayOutlineEvent);
        noXrayOutlineEvent.post();
    }

    //? < 1.21.9 {
    @WrapOperation(method = {"renderEntities", "collectVisibleEntities"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean shouldAlsoGlow(Minecraft instance, Entity entity, Operation<Boolean> original) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(instance, entity);
        }
        return true;
    }
    //?} else {
     /*@WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;appearsGlowing()Z"))
     public boolean shouldAlsoGlow(EntityRenderState instance, Operation<Boolean> original, @Local Entity entity) {
         Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
         if (glowColor == null) {
             return original.call(instance);
         }
         return true;
     }
    *///?}

    //? < 1.21.9 {
    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    public int changeGlowColour(Entity entity, Operation<Integer> original) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(entity);
        }
        return glowColor;
    }
    //?}

    //? < 1.21.9 {
    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;outlineBufferSource()Lnet/minecraft/client/renderer/OutlineBufferSource;"))
    private OutlineBufferSource modifyVertexConsumerProvider(RenderBuffers storage, Operation<OutlineBufferSource> original, @Local Entity entity) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(storage);
        }
        return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers();
    }
    //?}

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V", ordinal = 0, shift = At.Shift.AFTER))
    private void setGlowDepth(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.getAreMobsHighlighted()) return;
        SkyHanniOutlineVertexConsumerProvider.checkIfDepthAttachmentNeedsUpdating();
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void renderSkyhanniGlow(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.getAreMobsHighlighted()) return;
        SkyHanniOutlineVertexConsumerProvider.getVertexConsumers().endOutlineBatch();
    }

}
