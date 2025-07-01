package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.features.misc.HideArmor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;

@Mixin(HeadFeatureRenderer.class)
public class MixinHeadFeatureRenderer {
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderArmor(
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int light,
        LivingEntityRenderState renderState,
        float f,
        float g,
        CallbackInfo ci
    ) {
        Entity current = HideArmor.getCurrentEntity();
        if (current == null) {
            return;
        }
        if (current instanceof PlayerEntity && HideArmor.INSTANCE.shouldHideArmor(((PlayerEntity) current))) {
            ci.cancel();
        }
    }
}
