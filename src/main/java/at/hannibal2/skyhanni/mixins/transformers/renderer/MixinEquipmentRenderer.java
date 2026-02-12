package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.data.entity.EntityOpacityManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//? if > 1.21.10
//import net.minecraft.client.renderer.rendertype.RenderTypes;

@Mixin(EquipmentLayerRenderer.class)
public class MixinEquipmentRenderer {

     @ModifyArg(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", ordinal = 1), index = 3)
     private RenderType replaceVertexConsumer(RenderType original, @Local(ordinal = 1) Identifier identifier) {
         if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
             Integer entityAlpha = EntityOpacityManager.getEntityOpacity(livingEntity);
             if (entityAlpha == null) return original;

             //? if < 1.21.11 {
             return RenderType.armorTranslucent(identifier);
             //?} else
             //return RenderTypes.armorTranslucent(identifier);
         }
         return original;
     }

}
