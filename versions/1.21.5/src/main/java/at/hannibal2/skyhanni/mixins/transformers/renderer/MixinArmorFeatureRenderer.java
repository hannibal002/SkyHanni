package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.HideArmorHookKt;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.client.render.entity.model.BipedEntityModel;
//#if MC > 1.21.8
//$$ import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//#endif

@Mixin(ArmorFeatureRenderer.class)
public class MixinArmorFeatureRenderer {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void onRenderArmor(
        //#if MC < 1.21.9
        MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ItemStack stack, EquipmentSlot slot, int light, BipedEntityModel armorModel, CallbackInfo ci
        //#else
        //$$ MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, ItemStack stack, EquipmentSlot slot, int light, BipedEntityRenderState bipedEntityRenderState, CallbackInfo ci
        //#endif
    ) {
        if (HideArmorHookKt.shouldHideHead(slot)) {
            ci.cancel();
        }
    }
}
