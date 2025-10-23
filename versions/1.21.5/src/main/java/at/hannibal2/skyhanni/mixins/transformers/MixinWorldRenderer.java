package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC > 1.21.8
//$$ import net.minecraft.client.render.entity.state.EntityRenderState;
//#endif

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    //#if MC < 1.21.9
    @Inject(method = "getEntitiesToRender", at = @At(value = "HEAD"))
    public void resetRealGlowing(CallbackInfoReturnable<Boolean> cir) {
        //#else
        //$$ @Inject(method = "fillEntityRenderStates", at = @At(value = "HEAD"))
        //$$ public void resetRealGlowing(CallbackInfo ci) {
        //#endif
        RenderLivingEntityHelper.check();
        RenderEntityOutlineEvent noXrayOutlineEvent = new RenderEntityOutlineEvent(RenderEntityOutlineEvent.Type.NO_XRAY, null);
        RenderLivingEntityHelper.setCurrentGlowEvent(noXrayOutlineEvent);
        noXrayOutlineEvent.post();
    }

    //#if MC < 1.21.9
    @WrapOperation(method = {"renderEntities", "getEntitiesToRender"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    public boolean shouldAlsoGlow(MinecraftClient instance, Entity entity, Operation<Boolean> original) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(instance, entity);
        }
        return true;
    }
    //#else
    //$$ @WrapOperation(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;hasOutline()Z"))
    //$$ public boolean shouldAlsoGlow(EntityRenderState instance, Operation<Boolean> original, @Local Entity entity) {
    //$$     Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
    //$$     if (glowColor == null) {
    //$$         return original.call(instance);
    //$$     }
    //$$     return true;
    //$$ }
    //#endif

    //#if MC < 1.21.9
    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    public int changeGlowColour(Entity entity, Operation<Integer> original) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(entity);
        }
        return glowColor;
    }
    //#endif

    //#if MC < 1.21.9
    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilderStorage;getOutlineVertexConsumers()Lnet/minecraft/client/render/OutlineVertexConsumerProvider;"))
    private OutlineVertexConsumerProvider modifyVertexConsumerProvider(BufferBuilderStorage storage, Operation<OutlineVertexConsumerProvider> original, @Local Entity entity) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(storage);
        }
        return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers();
    }
    //#endif

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V", ordinal = 0, shift = At.Shift.AFTER))
    private void setGlowDepth(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.getAreMobsHighlighted()) return;
        SkyHanniOutlineVertexConsumerProvider.checkIfDepthAttachmentNeedsUpdating();
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void renderSkyhanniGlow(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.getAreMobsHighlighted()) return;
        SkyHanniOutlineVertexConsumerProvider.getVertexConsumers().draw();
    }

}
