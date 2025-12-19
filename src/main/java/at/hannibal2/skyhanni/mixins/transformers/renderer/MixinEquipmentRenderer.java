package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.data.entity.EntityOpacityManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//#if MC > 1.21.8
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//#endif

@Mixin(EquipmentRenderer.class)
public class MixinEquipmentRenderer {

    //#if MC < 1.21.9
    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;getArmorGlintConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/RenderLayer;Z)Lnet/minecraft/client/render/VertexConsumer;"))
    private VertexConsumer replaceVertexConsumer(VertexConsumerProvider vertexConsumerProvider, RenderLayer renderLayer, boolean b, Operation<VertexConsumer> original, @Local(ordinal = 1) Identifier identifier) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
            if (entityAlpha == null) return original.call(vertexConsumerProvider, renderLayer, b);

            RenderLayer newRenderLayer = RenderLayer.createArmorTranslucent(identifier);
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
