package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.features.misc.HideArmor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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

@Mixin(ArmorFeatureRenderer.class)
public class MixinArmorFeatureRenderer {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void onRenderArmor(
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        ItemStack stack,
        EquipmentSlot slot,
        int light,
        BipedEntityModel armorModel,
        CallbackInfo ci
    ) {
        Entity current = HideArmor.INSTANCE.get();
        if (current instanceof PlayerEntity && HideArmor.INSTANCE.shouldHideArmor(((PlayerEntity) current))) {
            if (!HideArmor.INSTANCE.getConfig().onlyHelmet || slot == EquipmentSlot.HEAD) {
                ci.cancel();
            }
        }
    }
}
