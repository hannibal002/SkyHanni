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
//#if MC > 1.21.8
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//#endif

@Mixin(EquipmentLayerRenderer.class)
public class MixinEquipmentRenderer {

    //#if MC < 1.21.9
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
    //#else
    //$$ @ModifyArg(method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V", ordinal = 1), index = 3)
    //$$ private RenderLayer replaceVertexConsumer(RenderLayer original, @Local(ordinal = 1) Identifier identifier) {
    //$$     if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
    //$$         Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
    //$$         if (entityAlpha == null) return original;
    //$$
    //$$         return RenderLayer.createArmorTranslucent(identifier);
    //$$     }
    //$$     return original;
    //$$ }
    //#endif

}
