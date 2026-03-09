package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.skyhanni.mixins.hooks.HideArmorHookKt;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(CustomHeadLayer.class)
public class MixinHeadFeatureRenderer {

    @WrapWithCondition(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;submitSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private boolean onRenderArmor(Direction direction, float f, float g, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int i, SkullModelBase skullModelBase, RenderType renderType, int j, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        return !HideArmorHookKt.shouldHideArmor();
    }

    /**
     * Wraps SkullBlockRenderer.submitSkull, setting the synchronous isSubmittingCustomOutlineSkull
     * flag for its entire synchronous execution. MixinSubmitNodeCollection.onSubmitModelHead reads
     * this flag at HEAD of every submitModel call to mark the skull state object in a WeakHashMap.
     * MixinModelFeatureRenderer Case 2 checks that map at deferred-render time via model.state().
     */
    @WrapOperation(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;submitSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private void wrapSubmitSkull(
        Direction direction, float f, float g, PoseStack matrices, SubmitNodeCollector submitNodeCollector,
        int i, SkullModelBase skullModelBase, RenderType renderType, int j,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original
    ) {
        EntityRenderState currentState = EntityRenderDispatcherHookKt.getEntityRenderState();
        boolean usingCustomOutline = currentState instanceof EntityRenderStateStore stateStore
            && stateStore.skyhanni$isUsingCustomOutline();
        if (usingCustomOutline) {
            RenderLivingEntityHelper.setSkullOutlineActive(true);
        }
        try {
            original.call(direction, f, g, matrices, submitNodeCollector, i, skullModelBase, renderType, j, crumblingOverlay);
        } finally {
            if (usingCustomOutline) {
                RenderLivingEntityHelper.setSkullOutlineActive(false);
            }
        }
    }

    @WrapWithCondition(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V")
    )
    private boolean onRenderItemstackOnHead(ItemStackRenderState instance, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int i, int j, int k) {
        return !HideArmorHookKt.shouldHideArmor();
    }

}
