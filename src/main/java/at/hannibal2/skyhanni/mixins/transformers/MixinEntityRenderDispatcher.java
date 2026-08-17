package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher<E extends Entity, S extends EntityRenderState> {

    @Inject(method = "submit", at = @At(value = "HEAD"), cancellable = true)
    public void onRenderPre(EntityRenderState renderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        EntityRenderDispatcherHookKt.setEntity(renderState);
        Entity entity = EntityRenderDispatcherHookKt.getEntity();
        if (entity instanceof LivingEntity livingEntity
            && !EntityRenderDispatcherHookKt.getActiveHolographicEntities().contains(livingEntity)) {
            //noinspection deprecation
            if (new SkyHanniRenderEntityEvent.Pre<>(livingEntity, d, e, f).post().isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "submit", at = @At(value = "RETURN"))
    public void onRenderPost(EntityRenderState entityRenderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        Entity entity = EntityRenderDispatcherHookKt.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            //noinspection deprecation
            new SkyHanniRenderEntityEvent.Post<>(livingEntity, d, e, f).post();
        }
        EntityRenderDispatcherHookKt.clearEntity();
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(Entity entity, Frustum camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (!EntityData.shouldRender(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
}
