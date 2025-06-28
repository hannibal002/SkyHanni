package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.features.misc.HideArmor;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC > 1.21
//$$ import net.minecraft.client.util.math.MatrixStack;
//$$ import net.minecraft.client.render.VertexConsumerProvider;
//#endif


@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (EntityData.onRenderCheck(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
    //#if MC > 1.21
    //$$ @Inject(method = "render", at = @At("HEAD"))
    //$$ private void onRenderStart(
    //$$     Entity entity,
    //$$     double x, double y, double z,
    //$$     float tickDelta,
    //$$     MatrixStack matrices,
    //$$     VertexConsumerProvider vertexConsumers,
    //$$     int light,
    //$$     CallbackInfo ci
    //$$ ) {
    //$$     HideArmor.INSTANCE.set(entity);
    //$$ }
    //$$
    //$$ @Inject(method = "render", at = @At("RETURN"))
    //$$ private void onRenderEnd(
    //$$     Entity entity,
    //$$     double x, double y, double z,
    //$$     float tickDelta,
    //$$     MatrixStack matrices,
    //$$     VertexConsumerProvider vertexConsumers,
    //$$     int light,
    //$$     CallbackInfo ci
    //$$ ) {
    //$$     HideArmor.INSTANCE.clear();
    //$$ }
    //#endif
}
