package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.garden.MouseSensitivityReducer;
import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.compat.MouseCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "onButton", at = @At("HEAD"))
    private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        MouseCompat.INSTANCE.handleMouseButton(input, action);
    }

    @WrapOperation(
        method = "turnPlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void modifyMouseSensitivity(LocalPlayer instance, double x, double y, Operation<Void> original) {
        original.call(instance, MouseSensitivityReducer.remapSensitivity(x), MouseSensitivityReducer.remapSensitivity(y));
    }
}
