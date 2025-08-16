package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.HideArmorHookKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
//#if MC > 1.21.7
//$$ import net.minecraft.client.render.entity.command.EntityRenderCommandQueue;
//$$ import net.minecraft.client.render.entity.state.EntityRenderState;
//#endif

@Mixin(HeadFeatureRenderer.class)
public class MixinHeadFeatureRenderer {

    //#if MC < 1.21.9
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderArmor(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, LivingEntityRenderState renderState, float f, float g, CallbackInfo ci) {
        //#else
        //$$ @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
        //$$ private void onRenderArmor(MatrixStack matrices, EntityRenderCommandQueue queue, int light, EntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        //#endif
        if (HideArmorHookKt.shouldHideArmor()) {
            ci.cancel();
        }
    }

}
