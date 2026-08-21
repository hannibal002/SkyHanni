package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHook;
import at.hannibal2.skyhanni.mixins.hooks.HideArmorHook;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CapeLayer.class)
public abstract class MixinCapeLayer {
    // Vanilla shifts the cape onto the chestplate to stop it from clipping. With the chestplate hidden,
    // that offset leaves the cape floating, so it is skipped to put the cape back onto the shoulders.
    @WrapWithCondition(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V")
    )
    private boolean wrapChestplateCapeOffset(PoseStack instance, float x, float y, float z) {
        return !HideArmorHook.shouldHideSlot(EquipmentSlot.CHEST);
    }

    @ModifyArg(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"), index = 3)
    private RenderType replaceRenderLayer(RenderType original, @Local PlayerSkin skinTextures) {
        if (skinTextures.cape() != null && EntityRenderDispatcherHook.getEntityTransparency() != null) {
            return RenderTypes.entityTranslucentCullItemTarget(skinTextures.cape().texturePath());
        }
        return original;
    }
}
