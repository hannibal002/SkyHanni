package at.hannibal2.skyhanni.mixins.transformers;

//? if < 26.2 {
/*import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelPartFeatureRenderer.class)
public abstract class MixinModelPartFeatureRenderer {

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V")
    )
    private void setSkyHanniOutlineColor(
        OutlineBufferSource outlineBufferSource,
        int color,
        Operation<Void> original,
        //~ if < 26.1 '@Local(name = "modelPartSubmit")' -> '@Local'
        @Local(name = "modelPartSubmit") SubmitNodeStorage.ModelPartSubmit modelPartSubmit
    ) {
        boolean hasCustomOutline = modelPartSubmit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) {
            original.call(SkyHanniOutlineHook.getVertexConsumers(), color);
            return;
        }
        original.call(outlineBufferSource, color);
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer getSkyHanniOutlineBuffer(
        OutlineBufferSource outlineBufferSource,
        RenderType renderType,
        Operation<VertexConsumer> original,
        //~ if < 26.1 '@Local(name = "modelPartSubmit")' -> '@Local'
        @Local(name = "modelPartSubmit") SubmitNodeStorage.ModelPartSubmit modelPartSubmit
    ) {
        boolean hasCustomOutline = modelPartSubmit.skyhanni$isUsingCustomOutline();

        if (hasCustomOutline) {
            return original.call(SkyHanniOutlineHook.getVertexConsumers(), renderType);
        }
        return original.call(outlineBufferSource, renderType);
    }
}
*///?}
