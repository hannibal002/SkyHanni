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

    //? if < 26.2 {
    /*@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V"))
    private void setSkyHanniOutlineColor(OutlineBufferSource outlineConsumer, int color, Operation<Void> original, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model) {
        if (skyhanni$usesCustomOutline(model)) {
            original.call(SkyHanniOutlineHook.getVertexConsumers(), color);
        } else {
            original.call(outlineConsumer, color);
        }
    }
    *///?}

    //~ if < 26.2 'prepareModel' -> 'renderModel'
    //~ if < 26.2 'feature/ModelFeatureRenderer;getVertexBuilder' -> 'OutlineBufferSource;getBuffer'
    @WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    //~ if < 26.2 'ModelFeatureRenderer' -> 'OutlineBufferSource'
    private VertexConsumer getSkyHanniOutlineBuffer(ModelFeatureRenderer instance, RenderType layer, Operation<VertexConsumer> original, @Local(argsOnly = true) ModelFeatureRenderer.Submit<?> model) {
        if (skyhanni$usesCustomOutline(model)) {
            //? if >= 26.2 {
            SkyHanniOutlineHook.beginCustomOutlineBuild();
            try {
                return original.call(instance, layer);
            } finally {
                SkyHanniOutlineHook.finishCustomOutlineBuild();
            }
            //?} else
            //return original.call(SkyHanniOutlineHook.getVertexConsumers(), layer);
        } else {
            return original.call(instance, layer);
        }
    }

    @Unique
    private boolean skyhanni$usesCustomOutline(ModelFeatureRenderer.Submit<?> model) {
        Object obj = model;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) return true;
        return model.state() instanceof EntityRenderState currentState && currentState.skyhanni$isUsingCustomOutline();
    }
}
