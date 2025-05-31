package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.compat.MouseCompat;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Shadow
    private double cursorDeltaX;

    @Shadow
    private double cursorDeltaY;

    @Inject(method = "onCursorPos", at = @At("RETURN"))
    private void onMouseButton(long window, double x, double y, CallbackInfo ci) {
        MouseCompat.INSTANCE.setDeltaMouseX(this.cursorDeltaX);
        MouseCompat.INSTANCE.setDeltaMouseY(this.cursorDeltaY);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseCompat.INSTANCE.setScroll(vertical);
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) {
            MouseCompat.INSTANCE.setLastEventButton(button);
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void onMouseButtonHead(double timeDelta, CallbackInfo ci) {
        MouseCompat.INSTANCE.setTimeDelta(timeDelta);
    }
}
