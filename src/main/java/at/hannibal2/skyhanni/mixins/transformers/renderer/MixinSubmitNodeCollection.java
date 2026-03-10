package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection<E> {

    @WrapOperation(method = "submitItem", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean onSubmitItem(List<E> list, E itemCommand, Operation<Boolean> original) {
        EntityRenderState currentState = EntityRenderDispatcherHookKt.getEntityRenderState();
        if (itemCommand instanceof GlowingStateStore casted && currentState instanceof EntityRenderStateStore stateStore && stateStore.skyhanni$isUsingCustomOutline()) {
            casted.skyhanni$setUsingCustomOutline();
        }
        return original.call(list, itemCommand);
    }

    /**
     * When we are inside a SkullBlockRenderer.submitSkull call for a custom-outlined entity
     * (signalled by the synchronous isSubmittingCustomOutlineSkull flag set by
     * MixinHeadFeatureRenderer), tag the skull state object in a WeakHashMap-backed set.
     * MixinModelFeatureRenderer Case 2 checks this set at deferred-render time via
     * model.state() — the same object passed here as the state parameter.
     * This avoids needing to know the concrete List/ArrayList type used internally.
     */
    @Inject(method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("HEAD"))
    private void onSubmitModelHead(Model model, Object state, PoseStack poseStack, RenderType renderType, int i, int j, int k, TextureAtlasSprite sprite, int light, ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo ci) {
        EntityRenderState currentState = EntityRenderDispatcherHookKt.getEntityRenderState();
        if (currentState instanceof EntityRenderStateStore store && store.skyhanni$isUsingCustomOutline() && state != null) {
            RenderLivingEntityHelper.markModelSubmitAsCustomOutline(state);
        }
    }
}
