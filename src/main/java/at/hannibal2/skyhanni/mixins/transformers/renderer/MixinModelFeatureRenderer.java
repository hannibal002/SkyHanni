package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public class MixinModelFeatureRenderer {

    /**
     * Returns true if this model submit should be routed through the SkyHanni depth-aware
     * (NO_XRAY) outline buffer instead of the vanilla x-ray outline buffer.
     *
     * Two cases:
     *  1. Normal entity body parts — the entity render state carries skyhanni$isUsingCustomOutline
     *     (original behaviour, must be preserved for normal zombies etc.).
     *  2. Skull head geometry (armor-stand rat mobs) — submitted via SkullBlockRenderer which
     *     creates ModelSubmit nodes with a block-entity state, not an EntityRenderState.
     *     MixinModelCommand mixes GlowingStateStore into ModelSubmit and
     *     MixinSubmitNodeCollection tags the node at submission time via the synchronous
     *     isSubmittingCustomOutlineSkull flag set by MixinHeadFeatureRenderer.
     */
    @Unique
    private static boolean shouldUseCustomOutline(SubmitNodeStorage.ModelSubmit<?> model) {
        // Case 1 – normal entity body (original check, must not be removed).
        if (model.state() instanceof EntityRenderStateStore currentState && currentState.skyhanni$isUsingCustomOutline()) {
            return true;
        }
        // Case 2 – skull/block-entity model: state object was registered in a WeakHashMap at
        // submission time by MixinSubmitNodeCollection.onSubmitModelHead when the synchronous
        // isSubmittingCustomOutlineSkull flag (set by MixinHeadFeatureRenderer) was active.
        Object state = model.state();
        return state != null && RenderLivingEntityHelper.isModelSubmitCustomOutline(state);
    }

    @WrapOperation(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V"))
    private void setSkyHanniOutlineColor(OutlineBufferSource outlineConsumer, int color, Operation<Integer> original, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model) {
        if (shouldUseCustomOutline(model)) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), color);
        } else {
            original.call(outlineConsumer, color);
        }
    }

    @WrapOperation(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer getSkyHanniOutlineBuffer(OutlineBufferSource outlineConsumer, RenderType layer, Operation<VertexConsumer> original, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model) {
        if (shouldUseCustomOutline(model)) {
            return original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), layer);
        } else {
            return original.call(outlineConsumer, layer);
        }
    }

}
