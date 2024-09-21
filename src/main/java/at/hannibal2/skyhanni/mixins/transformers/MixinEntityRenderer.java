package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.features.inventory.HarpFeatures;
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

    @Inject(method = "renderWorld", at = @At("HEAD"), cancellable = true)
    private void renderWorld(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        HarpFeatures.tryBlockRender(ci);
    }
}
