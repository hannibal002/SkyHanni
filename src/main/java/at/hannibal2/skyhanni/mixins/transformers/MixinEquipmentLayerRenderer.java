package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EquipmentLayerRenderer.class)
public abstract class MixinEquipmentLayerRenderer {
     @ModifyArg(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(
            value = "INVOKE",
            //? if >= 26.3 {
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V",
            ordinal = 0
            //?} else {
            /*target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            ordinal = 1
            *///?}
        ),
        index = 3
    )
     private RenderType replaceVertexConsumer(RenderType original, @Local(ordinal = 1) Identifier identifier) {
         if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
             Integer entityAlpha = EntityTransparencyManager.getEntityTransparency(livingEntity);
             if (entityAlpha == null) return original;
             //~ if < 26.3 'wolfArmorCracks' -> 'armorTranslucent'
             return RenderTypes.wolfArmorCracks(identifier);
         }
         return original;
     }
}
