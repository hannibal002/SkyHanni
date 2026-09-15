package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.2 {
import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.3 {
import net.minecraft.client.renderer.StagedVertexBuffer.ExecuteInfo;
//?} else {
/*import org.spongepowered.asm.mixin.Shadow;
*///?}

@Mixin(PreparedRenderType.class)
public abstract class MixinPreparedRenderType {
    //? if < 26.3 {
    /*@Shadow
    public abstract RenderPipeline pipeline();
    *///?}

    @Inject(
        //? if >= 26.3 {
        method = "draw",
        //?} else
        //method = "drawFromBuffer(Lcom/mojang/renderpearl/api/buffers/GpuBuffer;Lcom/mojang/renderpearl/api/buffers/GpuBuffer;Lcom/mojang/renderpearl/api/pipeline/IndexType;III)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;)V",
            shift = At.Shift.AFTER
        )
    )
    private void bindSkyHanniChromaUniform(
        //? if >= 26.3 {
        ExecuteInfo info,
        RenderPass renderPass,
        RenderPipeline renderPipeline,
        CallbackInfo ci
        //?} else {
        /*CallbackInfo ci,
        @Local RenderPass renderPass
        *///?}
    ) {
        //~ if < 26.3 'renderPipeline' -> 'pipeline()'
        GuiRendererHook.insertChromaSetUniform(renderPass, renderPipeline);
    }
}
//?}
