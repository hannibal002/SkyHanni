package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.2 {
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class MixinRenderTypeFeatureRenderer {

    @WrapOperation(
        method = "executeGroup",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;drawFromBuffer(Lnet/minecraft/client/renderer/StagedVertexBuffer$ExecuteInfo;)V"
        )
    )
    private void drawSkyHanniOutlineWithCustomDepth(
        PreparedRenderType renderType,
        StagedVertexBuffer.ExecuteInfo executeInfo,
        Operation<Void> original
    ) {
        boolean hasCustomOutline = SkyHanniOutlineHook.isCustomOutlinePipeline(renderType.pipeline());
        if (hasCustomOutline) SkyHanniOutlineHook.beginRendering();
        try {
            original.call(renderType, executeInfo);
        } finally {
            if (hasCustomOutline) SkyHanniOutlineHook.finishRendering();
        }
    }
}
//?}
