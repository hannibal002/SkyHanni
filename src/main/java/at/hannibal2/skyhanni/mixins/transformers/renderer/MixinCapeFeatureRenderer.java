package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.data.entity.EntityOpacityManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CapeLayer.class)
public class MixinCapeFeatureRenderer {

    @ModifyArg(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"), index = 3)
    private RenderType replaceRenderLayer(RenderType original, @Local PlayerSkin skinTextures) {
        if (skinTextures.cape() != null && EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
            if (entityAlpha == null) return original;
            return RenderType.itemEntityTranslucentCull(skinTextures.cape().texturePath());
        }
        return original;
    }

}
