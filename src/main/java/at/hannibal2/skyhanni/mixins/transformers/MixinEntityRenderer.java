package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.mixins.hooks.MouseSensitivityHook;
import at.hannibal2.skyhanni.utils.compat.DrawContext;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "updateCameraAndRender", at = @At("TAIL"))
    private void onLastRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        GuiEditManager.renderLast(new DrawContext());
    }

    @Redirect(
        method = "updateCameraAndRender(FJ)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/settings/GameSettings;mouseSensitivity:F",
            opcode = Opcodes.GETFIELD
        )
    )
    private float redirectMouseSensitivity(GameSettings settings) {
        return MouseSensitivityHook.INSTANCE.getMouseSensitivity(settings.mouseSensitivity);
    }
}
