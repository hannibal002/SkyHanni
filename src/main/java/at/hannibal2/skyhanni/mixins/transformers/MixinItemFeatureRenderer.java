package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineHook;
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

import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;

@Mixin(ItemFeatureRenderer.class)
public abstract class MixinItemFeatureRenderer {

    @WrapOperation(
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V")
    )
    private void setSkyHanniOutlineColor(
        OutlineBufferSource instance,
        int color,
        Operation<Void> original,
        @Local(
            //? if >= 26.1
            argsOnly = true, name = "submit"
            //? else
            //name = "itemSubmit"
        ) SubmitNodeStorage.ItemSubmit submit
    ) {
        boolean hasCustomOutline = submit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) {
            original.call(SkyHanniOutlineHook.getVertexConsumers(), color);
            return;
        }
        original.call(instance, color);
    }

    @WrapOperation(
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer wrapOutlineVertexConsumer(
        OutlineBufferSource instance,
        RenderType renderType,
        Operation<VertexConsumer> original,
        @Local(argsOnly = true, name = "submit") SubmitNodeStorage.ItemSubmit submit
    ) {
        boolean hasCustomOutline = submit.skyhanni$isUsingCustomOutline();

        return hasCustomOutline
            ? SkyHanniOutlineHook.getVertexConsumers().getBuffer(renderType)
            : original.call(instance, renderType);
    }

    //? if >= 26.1 {
    @ModifyArg(
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
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

    @ModifyExpressionValue(
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType modifyRenderLayer(RenderType layer) {
        if (!(EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity)) return layer;
        if (EntityTransparencyManager.getEntityTransparency(livingEntity) == null) return layer;
        return RenderTypes.glintTranslucent();
    }
    //?}
}
