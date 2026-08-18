package at.hannibal2.skyhanni.mixins.transformers;

//? if < 26.2 {
/*import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public abstract class MixinModelFeatureRenderer {

    @WrapOperation(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V"
        )
    )
    private void setSkyHanniOutlineColor(
        OutlineBufferSource instance,
        int color,
        Operation<Void> original,
        @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model
    ) {
        if (skyhanni$usesCustomOutline(model)) {
            original.call(SkyHanniOutlineHook.getVertexConsumers(), color);
        } else {
            original.call(instance, color);
        }
    }

    @WrapOperation(
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
}
*///?}
