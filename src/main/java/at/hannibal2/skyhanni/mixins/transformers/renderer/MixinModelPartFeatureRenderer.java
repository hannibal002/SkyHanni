package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelPartFeatureRenderer.class)
public abstract class MixinModelPartFeatureRenderer {

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V")
    )
    private void setSkyHanniOutlineColor(
        OutlineBufferSource outlineConsumer,
        int color,
        Operation<Void> original,
        //~ if < 26.1 '@Local(argsOnly = true)' -> '@Local'
        @Local(argsOnly = true) SubmitNodeStorage.ModelPartSubmit modelPart
    ) {
        if (skyhanni$usesCustomOutline(modelPart)) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), color);
        } else {
            original.call(outlineConsumer, color);
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer getSkyHanniOutlineBuffer(
        OutlineBufferSource outlineConsumer,
        RenderType layer,
        Operation<VertexConsumer> original,
        @Local SubmitNodeStorage.ModelPartSubmit modelPart
    ) {
        if (skyhanni$usesCustomOutline(modelPart)) {
            return original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), layer);
        } else {
            return original.call(outlineConsumer, layer);
        }
    }

    @Unique
    private boolean skyhanni$usesCustomOutline(SubmitNodeStorage.ModelPartSubmit modelPart) {
        Object obj = modelPart;
        return obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline();
    }
}
