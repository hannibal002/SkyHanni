package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Sets a synchronous flag around the entire SkullBlockRenderer.submitSkull call.
 * MixinSubmitNodeCollection.onSubmitModelHead reads this flag at the HEAD of every
 * SubmitNodeCollection.submitModel call to register the skull's state object in a WeakHashMap.
 * MixinModelFeatureRenderer Case 2 then routes those nodes through the depth-aware outline buffer.
 */
@Mixin(SkullBlockRenderer.class)
public class MixinSkullBlockRenderer {

    @Inject(
        method = "submitSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("HEAD")
    )
    private static void onSubmitSkullHead(
        Direction direction, float f, float g, PoseStack poseStack, SubmitNodeCollector collector,
        int i, SkullModelBase skullModel, RenderType renderType, int j,
        ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo ci
    ) {
        EntityRenderState entityState = EntityRenderDispatcherHookKt.getEntityRenderState();
        if (entityState instanceof EntityRenderStateStore stateStore && stateStore.skyhanni$isUsingCustomOutline()) {
            RenderLivingEntityHelper.setSkullOutlineActive(true);
        }
    }

    @Inject(
        method = "submitSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("RETURN")
    )
    private static void onSubmitSkullReturn(
        Direction direction, float f, float g, PoseStack poseStack, SubmitNodeCollector collector,
        int i, SkullModelBase skullModel, RenderType renderType, int j,
        ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo ci
    ) {
        RenderLivingEntityHelper.setSkullOutlineActive(false);
    }
}
