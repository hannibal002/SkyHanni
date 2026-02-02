package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.HideArmorHookKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Mixin(CustomHeadLayer.class)
public class MixinHeadFeatureRenderer {

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderArmor(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, EntityRenderState entityRenderState, float f, float g, CallbackInfo ci) {
        if (HideArmorHookKt.shouldHideArmor()) {
            ci.cancel();
        }
    }

}
