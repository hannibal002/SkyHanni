package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection<E> {

    @WrapOperation(method = "submitItem", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean onSubmitItem(List<E> list, E itemCommand, Operation<Boolean> original) {
        skyhanni$markCustomOutline(itemCommand);
        return original.call(list, itemCommand);
    }

    @WrapOperation(
        method = "submitModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
        )
    )
    private void onSubmitModel(
        ModelFeatureRenderer.Storage storage,
        RenderType renderType,
        SubmitNodeStorage.ModelSubmit<?> modelSubmit,
        Operation<Void> original
    ) {
        skyhanni$markCustomOutline(modelSubmit);
        original.call(storage, renderType, modelSubmit);
    }

    @WrapOperation(
        method = "submitModelPart",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
        )
    )
    private void onSubmitModelPart(
        ModelPartFeatureRenderer.Storage storage,
        RenderType renderType,
        SubmitNodeStorage.ModelPartSubmit modelPartSubmit,
        Operation<Void> original
    ) {
        skyhanni$markCustomOutline(modelPartSubmit);
        original.call(storage, renderType, modelPartSubmit);
    }

    @Unique
    private void skyhanni$markCustomOutline(Object submit) {
        EntityRenderState currentState = EntityRenderDispatcherHookKt.getEntityRenderState();
        if (submit instanceof GlowingStateStore casted && currentState != null && currentState.skyhanni$isUsingCustomOutline()) {
            casted.skyhanni$setUsingCustomOutline();
        }
    }
}
