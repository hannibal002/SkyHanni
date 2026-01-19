package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent;
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent;
import at.hannibal2.skyhanni.mixins.hooks.MouseSensitivityHook;
import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.compat.MouseCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? > 1.21.8 {
/*import net.minecraft.client.input.MouseButtonInfo;
*///?}

@Mixin(MouseHandler.class)
public class MixinMouse {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "onMove", at = @At("RETURN"))
    private void onMouseButton(long window, double x, double y, CallbackInfo ci) {
        MouseCompat.INSTANCE.setDeltaMouseX(this.accumulatedDX);
        MouseCompat.INSTANCE.setDeltaMouseY(this.accumulatedDY);
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseCompat.INSTANCE.setScroll(vertical);
        DelayedRun.INSTANCE.runNextTickOld(() -> {
            MouseCompat.INSTANCE.setScroll(0);
            return null;
        });
    }

    //? < 1.21.10 {
    @Inject(method = "onPress", at = @At("HEAD"))
    //?} else
    //@Inject(method = "onButton", at = @At("HEAD"))
    //? < 1.21.9 {
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        //?} else {
        /*private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
            int button = input.button();
        *///?}
        if (action == 1) {
            MouseCompat.INSTANCE.setLastEventButton(button);
            new KeyDownEvent(button).post();
            new KeyPressEvent(button).post();
        } else {
            new KeyPressEvent(button).post();
            DelayedRun.INSTANCE.runNextTickOld(() -> {
                MouseCompat.INSTANCE.setLastEventButton(-1);
                return null;
            });
        }
    }

    @Inject(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWindowActive()Z"))
    private void onMouseButtonHead(CallbackInfo ci, @Local(ordinal = 0) double timeDelta) {
        MouseCompat.INSTANCE.setTimeDelta(timeDelta * 10000);
    }

    @ModifyVariable(method = "turnPlayer", at = @At("STORE"), ordinal = 1)
    private double modifyMouseX(double value) {
        return MouseSensitivityHook.INSTANCE.remapSensitivity((float) value);
    }
}
