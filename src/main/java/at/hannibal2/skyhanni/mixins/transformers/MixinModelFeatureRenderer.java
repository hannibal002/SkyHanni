package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderAlphaStore;
import at.hannibal2.skyhanni.utils.render.AlphaVertexConsumer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if < 26.2 {
/*import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Unique;
*///?}

@Mixin(ModelFeatureRenderer.class)
public abstract class MixinModelFeatureRenderer {
    //~ if < 26.2 'prepareModel' -> 'renderModel'
    @WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", ordinal = 0))
    private void applyRenderAlpha(
        net.minecraft.client.model.Model<?> instance,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        VertexConsumer vertexConsumer,
        int light,
        int overlay,
        int color,
        Operation<Void> original,
        //~ if < 26.2 'ModelFeatureRenderer.Submit' -> 'SubmitNodeStorage.ModelSubmit'
        @Local(argsOnly = true) ModelFeatureRenderer.Submit<?> model
    ) {
        Object object = model;
        int alpha = object instanceof RenderAlphaStore alphaStore ? alphaStore.skyhanni$getRenderAlpha() : 255;
        original.call(instance, poseStack, alpha < 255 ? new AlphaVertexConsumer(vertexConsumer, alpha) : vertexConsumer, light, overlay, color);
    }

    //? if < 26.2 {
    /*@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V"))
    private void setSkyHanniOutlineColor(OutlineBufferSource instance, int color, Operation<Void> original, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model) {
        if (skyhanni$usesCustomOutline(model)) {
            original.call(SkyHanniOutlineHook.getVertexConsumers(), color);
        } else {
            original.call(instance, color);
        }
    }
    *///?}

    // Beta's legacy outline hooks are 26.1-only; 26.2 uses RenderChest.
    //? if < 26.2 {
    /*@WrapOperation(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer getSkyHanniOutlineBuffer(
        OutlineBufferSource instance,
        RenderType layer,
        Operation<VertexConsumer> original,
        @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model
    ) {
        if (skyhanni$usesCustomOutline(model)) {
            return original.call(SkyHanniOutlineHook.getVertexConsumers(), layer);
        }

        return original.call(instance, layer);
    }

    @Unique
    private boolean skyhanni$usesCustomOutline(SubmitNodeStorage.ModelSubmit<?> model) {
        Object obj = model;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return true;
        }

        return model.state() instanceof EntityRenderState currentState &&
            currentState.skyhanni$isUsingCustomOutline();
    }
    *///?}
}
