package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.data.entity.EntityOpacityManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? > 1.21.8 {
/*import org.spongepowered.asm.mixin.injection.ModifyArg;
*///?}

@Mixin(EquipmentLayerRenderer.class)
public class MixinEquipmentRenderer {

    //? < 1.21.9 {
    @WrapOperation(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getArmorFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Z)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer replaceVertexConsumer(MultiBufferSource vertexConsumerProvider, RenderType renderLayer, boolean b, Operation<VertexConsumer> original, @Local(ordinal = 1) ResourceLocation identifier) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
            if (entityAlpha == null) return original.call(vertexConsumerProvider, renderLayer, b);

            RenderType newRenderLayer = RenderType.armorTranslucent(identifier);
            return original.call(vertexConsumerProvider, newRenderLayer, b);
        }
        return original.call(vertexConsumerProvider, renderLayer, b);
    }
    //?} else {
     /*@ModifyArg(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", ordinal = 1), index = 3)
     private RenderType replaceVertexConsumer(RenderType original, @Local(ordinal = 1) ResourceLocation identifier) {
         if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
             Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
             if (entityAlpha == null) return original;

             return RenderType.armorTranslucent(identifier);
         }
         return original;
     }
    *///?}

}
