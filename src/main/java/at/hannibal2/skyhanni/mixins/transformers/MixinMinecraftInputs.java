package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MinecraftInputHook;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(Minecraft.class)
public class MixinMinecraftInputs {

    @Shadow
    public MovingObjectPosition objectMouseOver;

    @Shadow
    private int leftClickCounter;

    @Shadow
    @Nullable
    public PlayerControllerMP playerController;

    @Inject(
        at = @At("HEAD"),
        method = "rightClickMouse",
        cancellable = true
    )
    public void handleRightClickMouse(CallbackInfo ci) {
        if (this.playerController.getIsHittingBlock()) return;

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
        if (this.leftClickCounter > 0) return;

        if (MinecraftInputHook.shouldCancelMouseLeftClick(this.objectMouseOver))
            //#if MC < 1.21
            ci.cancel();
        //#else
        //$$ cir.setReturnValue(false);
        //#endif
    }

    @ModifyVariable(
        at = @At(value = "HEAD"),
        method = "sendClickBlockToController",
        argsOnly = true
    )
    public boolean handleBlockClick(boolean isLeftClick) {
        if (isLeftClick && this.leftClickCounter <= 0) {
            if (MinecraftInputHook.shouldCancelContinuedBlockBreak(
                this.objectMouseOver,
                ((AccessorPlayerControllerMP) this.playerController).skyhanni_getCurrentBlock()
            )) return false;
        }
        return isLeftClick;
    }
}
