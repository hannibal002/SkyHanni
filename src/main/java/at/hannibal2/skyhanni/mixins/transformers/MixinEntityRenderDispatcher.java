package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher<E extends Entity, S extends EntityRenderState> {

    @Inject(method = "submit", at = @At(value = "HEAD"), cancellable = true)
    public void onRenderPre(EntityRenderState renderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        EntityRenderDispatcherHookKt.setEntity(renderState);
        Entity entity = EntityRenderDispatcherHookKt.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            if (new SkyHanniRenderEntityEvent.Pre<>(livingEntity, d, e, f).post()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "submit", at = @At(value = "RETURN"))
    public void onRenderPost(EntityRenderState entityRenderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        Entity entity = EntityRenderDispatcherHookKt.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            new SkyHanniRenderEntityEvent.Post<>(livingEntity, d, e, f).post();
        }
        EntityRenderDispatcherHookKt.clearEntity();
    }
}
