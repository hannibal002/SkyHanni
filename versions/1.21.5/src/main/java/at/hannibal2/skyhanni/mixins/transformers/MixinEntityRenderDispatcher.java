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

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher<E extends Entity, S extends EntityRenderState>  {

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"), cancellable = true)
    public void onRenderPre(E entity, double x, double y, double z, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            if (new SkyHanniRenderEntityEvent.Pre(livingEntity, x, y, z).post()) {
                ci.cancel();
            }
        }
        EntityRenderDispatcherHookKt.setEntity(entity);
    }

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", shift = At.Shift.AFTER))
    public void onRenderPost(E entity, double x, double y, double z, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            new SkyHanniRenderEntityEvent.Post(livingEntity, x, y, z).post();
        }
    }

}
