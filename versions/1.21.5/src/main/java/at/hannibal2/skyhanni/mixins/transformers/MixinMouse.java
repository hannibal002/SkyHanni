package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.compat.MouseCompat;
import com.llamalad7.mixinextras.sugar.Local;
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
        } else {
            DelayedRun.INSTANCE.runNextTick(() -> {
                MouseCompat.INSTANCE.setLastEventButton(-1);
                return null;
            });
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isWindowFocused()Z"))
    private void onMouseButtonHead(CallbackInfo ci, @Local(ordinal = 0) double timeDelta) {
        MouseCompat.INSTANCE.setTimeDelta(timeDelta * 10000);
    }
}
