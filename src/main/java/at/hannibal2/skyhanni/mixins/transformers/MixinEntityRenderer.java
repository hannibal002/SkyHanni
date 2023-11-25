package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRendererHook;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "updateCameraAndRender", at = @At("TAIL"))
    private void onLastRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        GuiEditManager.renderLast();
    }


    // Our target is to get the line 'GlStateManager.translate(0.0F, 0.0F, (float)(-d3));'
    // This line is in class EntityRenderer on line 615

    @Inject(method = "orientCamera", at = @At(value = "Lnet/minecraft/client/GlStateManager;translate(FFF)V", ordinal = 2))
    private void onTranslateCamera(float x, float y, float z, CallbackInfo ci) {
        EntityRendererHook.onTranslateCamera(x, y, z);
    }
}
