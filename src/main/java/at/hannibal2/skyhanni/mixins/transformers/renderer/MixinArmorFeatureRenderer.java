package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.HideArmorHookKt;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.model.HumanoidModel;
//#if MC > 1.21.8
//$$ import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//#endif

@Mixin(HumanoidArmorLayer.class)
public class MixinArmorFeatureRenderer {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void onRenderArmor(
        //#if MC < 1.21.9
        PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, ItemStack stack, EquipmentSlot slot, int light, HumanoidModel armorModel, CallbackInfo ci
        //#else
        //$$ MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, ItemStack stack, EquipmentSlot slot, int light, BipedEntityRenderState bipedEntityRenderState, CallbackInfo ci
        //#endif
    ) {
        if (HideArmorHookKt.shouldHideHead(slot)) {
            ci.cancel();
        }
    }
}
