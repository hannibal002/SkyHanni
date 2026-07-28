package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//? if < 26.2 {
/*import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}

@Mixin(ModelFeatureRenderer.class)
public abstract class MixinModelFeatureRenderer {

    //? if >= 26.2 {
    @WrapOperation(
        method = "prepareModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer wrapSkyHanniOutlineVertexConsumer(
        ModelFeatureRenderer instance,
        RenderType renderType,
        Operation<VertexConsumer> original,
        @Local(argsOnly = true) ModelFeatureRenderer.Submit<?> submit
    ) {
        if (skyhanni$usesCustomOutline(submit)) {
            SkyHanniOutlineHook.beginCustomOutlineBuild();
            try {
                return original.call(instance, renderType);
            } finally {
                SkyHanniOutlineHook.finishCustomOutlineBuild();
            }
        }
        return original.call(instance, renderType);
    }
    //?} else {
    /*@WrapOperation(
        method = "renderModel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V")
    )
    private <S> void setSkyHanniOutlineColor(
        OutlineBufferSource outlineBufferSource,
        int color,
        Operation<Void> original,
        //~ if < 26.1 '"submit"' -> '"modelSubmit"'
        @Local(argsOnly = true, name = "submit") ModelFeatureRenderer.Submit<S> submit
    ) {
        if (skyhanni$usesCustomOutline(submit)) {
            original.call(SkyHanniOutlineHook.getVertexConsumers(), color);
            return;
        }
        original.call(outlineBufferSource, color);
    }

    @WrapOperation(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private <S> VertexConsumer wrapOutlineVertexConsumer(
        OutlineBufferSource outlineBufferSource,
        RenderType renderType,
        Operation<VertexConsumer> original,
        //~ if < 26.1 '"submit"' -> '"modelSubmit"'
        @Local(argsOnly = true, name = "submit") ModelFeatureRenderer.Submit<S> submit
    ) {
        if (skyhanni$usesCustomOutline(submit)) {
            return original.call(SkyHanniOutlineHook.getVertexConsumers(), renderType);
        }
        return original.call(outlineBufferSource, renderType);
    }
    *///?}

    @Unique
    private boolean skyhanni$usesCustomOutline(ModelFeatureRenderer.Submit<?> submit) {
        Object obj = submit;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) return true;
        return submit.state() instanceof EntityRenderState currentState && currentState.skyhanni$isUsingCustomOutline();
    }
}
