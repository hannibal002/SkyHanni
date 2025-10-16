package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MinecraftInputHook;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif


@Mixin(Minecraft.class)
public class MixinMinecraftInputs {

    @Shadow
    public MovingObjectPosition objectMouseOver;

    @Inject(
        at = @At("HEAD"),
        method = "rightClickMouse",
        cancellable = true
    )
    public void handleRightClickMouse(CallbackInfo ci) {
        if (MinecraftInputHook.shouldCancelMouseRightClick(this.objectMouseOver)) ci.cancel();
    }

    @Inject(
        at = @At("HEAD"),
        method = "clickMouse",
        cancellable = true
    )
    public void handleLeftClickMouse(
        //#if MC < 1.21
        CallbackInfo ci
        //#else
        //$$ CallbackInfoReturnable<Boolean> cir
        //#endif
    ) {
        if (MinecraftInputHook.shouldCancelMouseLeftCLick(this.objectMouseOver))
            //#if MC < 1.21
            ci.cancel();
            //#else
            //$$ cir.setReturnValue(false);
            //#endif
    }
}
