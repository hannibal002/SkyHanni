package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.EntityUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Handle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    @Inject(method = "getEntitiesToRender", at = @At(value = "HEAD"))
    public void resetRealGlowing(CallbackInfoReturnable<Boolean> cir) {
        RenderLivingEntityHelper.INSTANCE.setRenderingRealGlow(false);
    }

    @Inject(method = "getEntitiesToRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    public void getIfRealGlowing(CallbackInfoReturnable<Boolean> cir, @Local Entity entity) {
        if (entity.isGlowing()) RenderLivingEntityHelper.INSTANCE.setRenderingRealGlow(true);
    }

    @WrapOperation(method = {"renderEntities", "getEntitiesToRender"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    public boolean shouldAlsoGlow(MinecraftClient instance, Entity entity, Operation<Boolean> original) {
        if (entity instanceof LivingEntity livingEntity) {
            int i = RenderLivingEntityHelper.internalSetColorMultiplier(livingEntity,0);
            if (i == 0) return original.call(instance, entity);
            boolean returnValue;
            if (RenderLivingEntityHelper.INSTANCE.getRenderingRealGlow()) {
                returnValue = EntityUtils.INSTANCE.canBeSeen(entity, 150, .5);
            } else {
                returnValue = true;
            }
            return returnValue;
        }
        return original.call(instance, entity);
    }

    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    public int changeGlowColour(Entity entity, Operation<Integer> original) {
        if (entity instanceof LivingEntity livingEntity) {
            int i = RenderLivingEntityHelper.internalSetColorMultiplier(livingEntity, 0);
            if (i == 0) return original.call(entity);
            return i;
        }
        return original.call(entity);
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V", ordinal = 0, shift = At.Shift.AFTER))
    private void setGlowDepth(CallbackInfo ci, @Share(namespace = "c", value = "copiedOutlineDepth") LocalBooleanRef copiedOutlineDepth) {
        if (RenderLivingEntityHelper.INSTANCE.getAreMobsHighlighted() && !copiedOutlineDepth.get()) {
            Handle<Framebuffer> glowFramebuffer = framebufferSet.entityOutlineFramebuffer;
            if (glowFramebuffer == null) return;
            glowFramebuffer.get().copyDepthFrom(client.getFramebuffer());
            // copiedOutlineDepth is for performance compat with skyblocker
            copiedOutlineDepth.set(true);
        }
    }

}
