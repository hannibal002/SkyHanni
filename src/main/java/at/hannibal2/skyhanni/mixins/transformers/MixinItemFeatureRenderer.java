package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//? if >= 26.2 {
import com.mojang.blaze3d.vertex.QuadInstance;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}

@Mixin(ItemFeatureRenderer.class)
public abstract class MixinItemFeatureRenderer {

    //? if >= 26.1 {
    @ModifyArg(
        //~ if < 26.2 'prepareOutlineSubmit' -> 'renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V'
        method = "prepareOutlineSubmit",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"), index = 0)
    private int modifyAlpha(int originalColor) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            Integer entityAlpha = EntityTransparencyManager.getEntityTransparency(livingEntity);
            if (entityAlpha == null) return originalColor;
            int newAlpha = Math.min(ARGB.alpha(originalColor), entityAlpha);
            return ARGB.color(newAlpha, ARGB.red(originalColor), ARGB.green(originalColor), ARGB.blue(originalColor));
        }
        return originalColor;
    }

    @ModifyExpressionValue(
        //~ if < 26.2 'prepareOutlineSubmit' -> 'renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V'
        method = "prepareOutlineSubmit",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType modifyRenderLayer(RenderType layer) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            if (EntityTransparencyManager.getEntityTransparency(livingEntity) == null) return layer;
            return RenderTypes.glintTranslucent();
        }
        return layer;
    }
    //?}

    //? if < 26.2 {
    /*@WrapOperation(
        //? if >= 26.1 {
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        //?} else {
        /^method = "render",
        ^///?}
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V")
    )
    private void setSkyHanniOutlineColor(OutlineBufferSource outlineConsumer, int i, Operation<Void> original, @Local SubmitNodeStorage.ItemSubmit itemCommand) {
        Object obj = itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), i);
        } else {
            original.call(outlineConsumer, i);
        }
    }
    *///?}

    //? if >= 26.1 {
    @WrapOperation(
        //~ if < 26.2 'prepareOutlineSubmit' -> 'renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V'
        method = "prepareOutlineSubmit",
        at = @At(
            value = "INVOKE",
            //~ if < 26.2 'renderer/feature/ItemFeatureRenderer;getVertexBuilder' -> 'renderer/OutlineBufferSource;getBuffer'
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    //~ if < 26.2 'ItemFeatureRenderer' -> 'OutlineBufferSource'
    private VertexConsumer modifyOutlineVertexConsumerProvider(ItemFeatureRenderer instance, RenderType renderType, Operation<VertexConsumer> original, /*? if < 26.2 {*//*@Local(argsOnly = true) *//*?}*/ItemFeatureRenderer.Submit itemCommand) {
        Object obj = (Object) itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            //? if >= 26.2 {
            SkyHanniOutlineVertexConsumerProvider.beginCustomOutlineBuild();
            try {
                return original.call(instance, renderType);
            } finally {
                SkyHanniOutlineVertexConsumerProvider.finishCustomOutlineBuild();
            }
            //?} else
            //return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers().getBuffer(renderType);
        }
        return original.call(instance, renderType);
    }
    //?} else {
    /*@ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            ordinal = 1
        ),
        index = 2
    )
    private MultiBufferSource modifyOutlineVertexConsumerProvider(MultiBufferSource outlineConsumer, @Local SubmitNodeStorage.ItemSubmit itemCommand) {
        Object obj = itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers();
        }
        return outlineConsumer;
    }
    *///?}
}
