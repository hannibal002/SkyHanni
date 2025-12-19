package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.data.entity.EntityOpacityManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//#if MC > 1.21.8
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//#endif

@Mixin(CapeLayer.class)
public class MixinCapeFeatureRenderer {

    //#if MC < 1.21.9
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer replaceVertexConsumer(MultiBufferSource instance, RenderType renderLayer, Operation<VertexConsumer> original, @Local PlayerSkin skinTextures) {
        if (skinTextures.capeTexture() != null && EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
            if (entityAlpha == null) return original.call(instance, renderLayer);

            RenderType newRenderLayer = RenderType.itemEntityTranslucentCull(skinTextures.capeTexture());
            return original.call(instance, newRenderLayer);
        }
        return original.call(instance, renderLayer);
    }
    //#else
    //$$ @ModifyArg(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"), index = 3)
    //$$ private RenderLayer replaceRenderLayer(RenderLayer original, @Local SkinTextures skinTextures) {
    //$$     if (skinTextures.cape() != null && EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
    //$$         Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
    //$$         if (entityAlpha == null) return original;
    //$$         return RenderLayer.getItemEntityTranslucentCull(skinTextures.cape().texturePath());
    //$$     }
    //$$     return original;
    //$$ }
    //#endif

}
