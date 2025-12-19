package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21.8
//$$ import net.minecraft.client.renderer.state.CameraRenderState;
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//#endif

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher<E extends Entity, S extends EntityRenderState> {

    //#if MC < 1.21.9
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V"), cancellable = true)
    public void onRenderPre(E entity, double x, double y, double z, float tickProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            if (new SkyHanniRenderEntityEvent.Pre<>(livingEntity, x, y, z).post()) {
                ci.cancel();
            }
        }
        EntityRenderDispatcherHookKt.setEntity(entity);
        //#else
        //$$ @Inject(method = "submit", at = @At(value = "HEAD"), cancellable = true)
        //$$ public void onRenderPre(EntityRenderState renderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        //$$     EntityRenderDispatcherHookKt.setEntity(renderState);
        //$$     Entity entity = EntityRenderDispatcherHookKt.getEntity();
        //$$     if (entity instanceof LivingEntity livingEntity) {
        //$$         // TODO confirm these are the right values for position
        //$$         if (new SkyHanniRenderEntityEvent.Pre<>(livingEntity, d, e, f).post()) {
        //$$             ci.cancel();
        //$$         }
        //$$     }
        //#endif
    }

    //#if MC < 1.21.9
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V", shift = At.Shift.AFTER))
    public void onRenderPost(E entity, double x, double y, double z, float tickProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            new SkyHanniRenderEntityEvent.Post<>(livingEntity, x, y, z).post();
        }
        //#else
        //$$ @Inject(method = "submit", at = @At(value = "RETURN"))
        //$$ public void onRenderPost(EntityRenderState entityRenderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        //$$     Entity entity = EntityRenderDispatcherHookKt.getEntity();
        //$$     if (entity instanceof LivingEntity livingEntity) {
        //$$         // TODO confirm these are the right values for position
        //$$         new SkyHanniRenderEntityEvent.Post<>(livingEntity, d, e, f).post();
        //$$     }
        //#endif
        EntityRenderDispatcherHookKt.clearEntity();
    }
}
