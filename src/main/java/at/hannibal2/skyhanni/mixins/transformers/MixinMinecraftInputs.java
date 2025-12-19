package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MinecraftInputHook;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraftInputs {

    @Shadow
    public HitResult hitResult;

    @Shadow
    private int missTime;

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Inject(
        at = @At("HEAD"),
        method = "startUseItem",
        cancellable = true
    )
    public void handleRightClickMouse(CallbackInfo ci) {
        if (this.gameMode.isDestroying()) return;

        if (MinecraftInputHook.shouldCancelMouseRightClick(this.hitResult)) ci.cancel();
    }

    @Inject(
        at = @At("HEAD"),
        method = "startAttack",
        cancellable = true
    )
    public void handleLeftClickMouse(CallbackInfoReturnable<Boolean> cir) {
        if (this.missTime > 0) return;

        if (MinecraftInputHook.shouldCancelMouseLeftClick(this.hitResult)) cir.setReturnValue(false);
    }

    @ModifyVariable(
        at = @At(value = "HEAD"),
        method = "continueAttack",
        argsOnly = true
    )
    public boolean handleBlockClick(boolean isLeftClick) {
        if (isLeftClick && this.missTime <= 0) {
            if (MinecraftInputHook.shouldCancelContinuedBlockBreak(
                this.hitResult,
                ((AccessorPlayerControllerMP) this.gameMode).skyhanni_getCurrentBlock()
            )) return false;
        }
        return isLeftClick;
    }
}
