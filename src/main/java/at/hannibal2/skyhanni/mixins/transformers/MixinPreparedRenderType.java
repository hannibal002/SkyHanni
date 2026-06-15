package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.2 {
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PreparedRenderType.class)
public abstract class MixinPreparedRenderType {

    @ModifyExpressionValue(
        method = "drawFromBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/IndexType;III)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;pipeline:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    private RenderPipeline useSkyHanniCustomDepthOutlinePipeline(RenderPipeline pipeline) {
        return SkyHanniOutlineHook.getCustomOutlinePipeline(pipeline);
    }
}
//?}
