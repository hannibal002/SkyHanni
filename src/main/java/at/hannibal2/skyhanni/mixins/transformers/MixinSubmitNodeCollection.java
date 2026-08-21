package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHook;
import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniRenderStateData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.2 {
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
//?} else {
/*import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.List;
*///?}

@Mixin(SubmitNodeCollection.class)
public abstract class MixinSubmitNodeCollection<E> {
    //? if >= 26.2 {
    @Shadow
    @Final
    public SimpleFeatureRenderPhase outline;

    @WrapOperation(
        method = {"submitItem", "submitModel"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"
        )
    )
    private void markCustomOutline(
        SimpleFeatureRenderPhase phase,
        SubmitNode submit,
        Operation<Void> original
    ) {
        if (phase == this.outline) {
            skyhanni$markCustomOutline(submit);
        }
        original.call(phase, submit);
    }
    //?} else {
    /*@WrapOperation(method = "submitItem", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
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
    *///?}

    @Unique
    private void skyhanni$markCustomOutline(Object submit) {
        EntityRenderState currentState = EntityRenderDispatcherHook.getEntityRenderState();
        if (submit instanceof GlowingStateStore casted && currentState != null && SkyHanniRenderStateData.isUsingCustomOutline(currentState)) {
            casted.skyhanni$setUsingCustomOutline();
        }
    }
}
