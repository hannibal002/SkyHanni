package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.SkyBlockUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    public void onRenderLabelHead(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            //noinspection deprecation
            if (new SkyHanniRenderEntityEvent.Specials.Pre<>(livingEntity, state.x, state.y, state.z).post().isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    public void onRenderLabelTail(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            //noinspection deprecation
            new SkyHanniRenderEntityEvent.Specials.Post<>(livingEntity, state.x, state.y, state.z).post();
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean shouldAlsoGlow(Minecraft client, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) EntityRenderState state) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(client, entity);
        }
        state.skyhanni$setUsingCustomOutline();
        return true;
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    public int getCustomGlowColor(Entity entity, Operation<Integer> original) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(entity);
        }
        return glowColor;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void setEntity(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        state.skyhanni$setEntity(entity);
    }

    // See modifyRenderLabelIfPresentArgs in MixinPlayerEntityRenderer.
    @ModifyArg(
        method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/level/CameraRenderState;)V", ordinal = 0),
        index = 3
    )
    private Component modifyRenderLabelIfPresentArgs(Component text) {
        if (SkyBlockUtils.getInSkyBlock()) {
            return EntityData.getHealthDisplay(text);
        }
        return text;
    }
}
