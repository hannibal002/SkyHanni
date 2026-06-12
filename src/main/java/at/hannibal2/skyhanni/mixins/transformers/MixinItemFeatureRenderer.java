package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.2 {
import com.mojang.blaze3d.vertex.QuadInstance;
//?} else {
/*import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}

//? if >= 26.1 {
import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import java.util.List;
*///?}

@Mixin(ItemFeatureRenderer.class)
public abstract class MixinItemFeatureRenderer {

    @WrapOperation(
        //? if >= 26.2 {
        method = "prepareOutlineSubmit",
        //?} elif >= 26.1 {
        /*method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        *///?} else
        //method = "render",
        //~ if < 26.2 'com/mojang/blaze3d/vertex/QuadInstance' -> 'net/minecraft/client/renderer/OutlineBufferSource'
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V")
    )
    private void setSkyHanniOutlineColor(
        //~ if < 26.2 'QuadInstance' -> 'OutlineBufferSource'
        QuadInstance instance,
        int color,
        Operation<Void> original,
        //~ if < 26.1 '"submit"' -> '"itemSubmit"'
        @Local(
            //? if >= 26.1
            argsOnly = true,
            //~ if < 26.1 'submit' -> 'itemSubmit'
            name = "submit"
        ) ItemFeatureRenderer.Submit submit
    ) {
        boolean hasCustomOutline = submit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) SkyHanniOutlineHook.beginRendering();
        original.call(instance, color);
        if (hasCustomOutline) SkyHanniOutlineHook.finishRendering();
    }

    //? if >= 26.1 {
    @WrapOperation(
        //? if >= 26.2 {
        method = "prepareOutlineSubmit",
        //?} else
        //method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(
            value = "INVOKE",
            //? if >= 26.2 {
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            //?} else
            //target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer wrapOutlineVertexConsumer(
        //~ if < 26.2 'ItemFeatureRenderer' -> 'OutlineBufferSource'
        ItemFeatureRenderer instance,
        RenderType renderType,
        Operation<VertexConsumer> original,
        /*? if < 26.2 {*//*@Local(argsOnly = true, name = "submit") *//*?}*/ItemFeatureRenderer.Submit submit
    ) {
        boolean hasCustomOutline = submit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) SkyHanniOutlineHook.beginRendering();
        VertexConsumer orig = original.call(instance, renderType);
        if (hasCustomOutline) SkyHanniOutlineHook.finishRendering();

        return orig;
    }
    //?} else {
    /*@WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            ordinal = 1
        )
    )
    private void wrapOutlineVertexConsumer(
        ItemDisplayContext type,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int lightCoords,
        int overlayCoords,
        int[] tintLayers,
        List<BakedQuad> quads,
        RenderType renderType,
        ItemStackRenderState.FoilType foilType,
        Operation<Void> original,
        @Local(name = "itemSubmit") ItemFeatureRenderer.Submit itemSubmit
    ) {
        boolean hasCustomOutline = itemSubmit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) SkyHanniOutlineHook.beginRendering();
        original.call(
            type, poseStack, bufferSource, lightCoords, overlayCoords, tintLayers, quads, renderType, foilType
        );
        if (hasCustomOutline) SkyHanniOutlineHook.finishRendering();
    }
    *///?}

    //? if >= 26.1 {
    @ModifyArg(
        //? if >= 26.2 {
        method = "prepareOutlineSubmit",
        //?} else
        //method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"),
        index = 0
    )
    private int modifyAlpha(int originalColor) {
        if (!(EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity)) return originalColor;

        Integer entityAlpha = EntityTransparencyManager.getEntityTransparency(livingEntity);
        if (entityAlpha == null) return originalColor;
        int newAlpha = Math.min(ARGB.alpha(originalColor), entityAlpha);
        return ARGB.color(newAlpha, ARGB.red(originalColor), ARGB.green(originalColor), ARGB.blue(originalColor));
    }
    //?}

    //? if >= 26.1 {
    @ModifyExpressionValue(
        //? if >= 26.2 {
        method = "prepareOutlineSubmit",
        //?} else
        //method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType modifyRenderLayer(RenderType layer) {
        if (!(EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity)) return layer;
        if (EntityTransparencyManager.getEntityTransparency(livingEntity) == null) return layer;
        return RenderTypes.glintTranslucent();
    }
    //?}
}
