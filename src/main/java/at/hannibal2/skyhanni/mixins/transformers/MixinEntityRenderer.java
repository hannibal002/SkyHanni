package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? > 1.21.8 {
/*import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
*///?}

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    //? < 1.21.10 {
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    //?} else
    //@Inject(method = "submitNameTag", at = @At("HEAD"), cancellable = true)
    //? < 1.21.9 {
    public void onRenderLabelHead(EntityRenderState state, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        //?} else {
        /*public void onRenderLabelHead(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        *///?}
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            if (new SkyHanniRenderEntityEvent.Specials.Pre<>(livingEntity, state.x, state.y, state.z).post()) {
                ci.cancel();
            }
        }
    }

    //? < 1.21.10 {
    @Inject(method = "renderNameTag", at = @At("TAIL"))
    //?} else
    //@Inject(method = "submitNameTag", at = @At("TAIL"))
    //? < 1.21.9 {
    public void onRenderLabelTail(EntityRenderState state, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        //?} else {
        /*public void onRenderLabelTail(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        *///?}
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            new SkyHanniRenderEntityEvent.Specials.Post<>(livingEntity, state.x, state.y, state.z).post();
        }
    }

    //? > 1.21.8 {
    /*@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean shouldAlsoGlow(Minecraft client, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) EntityRenderState state) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        if (glowColor == null) {
            return original.call(client, entity);
        }
        ((EntityRenderStateStore) state).skyhanni$setUsingCustomOutline();
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
        ((EntityRenderStateStore) state).skyhanni$setEntity(entity);
    }
    *///?}

}
