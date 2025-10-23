package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21.8
//$$ import net.minecraft.client.render.state.CameraRenderState;
//$$ import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//#endif

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher<E extends Entity, S extends EntityRenderState> {

    //#if MC < 1.21.9
    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"), cancellable = true)
    public void onRenderPre(E entity, double x, double y, double z, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            if (new SkyHanniRenderEntityEvent.Pre<>(livingEntity, x, y, z).post()) {
                ci.cancel();
            }
        }
        EntityRenderDispatcherHookKt.setEntity(entity);
        //#else
        //$$ @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
        //$$ public void onRenderPre(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
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
    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", shift = At.Shift.AFTER))
    public void onRenderPost(E entity, double x, double y, double z, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            new SkyHanniRenderEntityEvent.Post<>(livingEntity, x, y, z).post();
        }
        //#else
        //$$ @Inject(method = "render", at = @At(value = "RETURN"))
        //$$ public void onRenderPost(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
        //$$     Entity entity = EntityRenderDispatcherHookKt.getEntity();
        //$$     if (entity instanceof LivingEntity livingEntity) {
        //$$         // TODO confirm these are the right values for position
        //$$         new SkyHanniRenderEntityEvent.Post<>(livingEntity, d, e, f).post();
        //$$     }
        //#endif
        EntityRenderDispatcherHookKt.clearEntity();
    }
}
