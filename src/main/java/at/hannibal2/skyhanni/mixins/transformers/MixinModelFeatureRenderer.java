package at.hannibal2.skyhanni.mixins.transformers;

// TODO 26.2
//? if < 26.2 {
/*import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public abstract class MixinModelFeatureRenderer {

    @WrapOperation(
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
        boolean hasCustomOutline = submit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) SkyHanniOutlineHook.beginRendering();
        original.call(outlineBufferSource, color);
        if (hasCustomOutline) SkyHanniOutlineHook.finishRendering();
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
        boolean hasCustomOutline = submit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) SkyHanniOutlineHook.beginRendering();
        VertexConsumer orig = original.call(outlineBufferSource, renderType);
        if (hasCustomOutline) SkyHanniOutlineHook.finishRendering();

        return orig;
    }
}
*///?}
