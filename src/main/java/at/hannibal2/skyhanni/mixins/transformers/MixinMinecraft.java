package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MinecraftInputHook;
import at.hannibal2.skyhanni.utils.system.PlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if < 26.2 {
/*import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;
*///?}

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public HitResult hitResult;

    @Shadow
    protected int missTime;

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    public abstract void stop();

    //? if < 26.2 {
    /*@ModifyExpressionValue(
        method = "addInitialScreens",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Options;onboardAccessibility:Z",
            opcode = Opcodes.GETFIELD
        )
    )
    public boolean onboardAccessibility(boolean original) {
        if (PlatformUtils.isDevEnvironment() && !Boolean.getBoolean("skyhanni.accessibilityOnboarding")) return false;
        return original;
    }
    *///?}

    @Inject(
        method = "onGameLoadFinished",
        at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", shift = At.Shift.AFTER)
    )
    private void autoQuitAfterInitialScreen(CallbackInfo ci) {
        if (PlatformUtils.isDevEnvironment() && Boolean.getBoolean("skyhanni.autoQuit")) this.stop();
    }

    @Inject(
        at = @At("HEAD"),
        method = "startUseItem",
        cancellable = true
    )
    public void handleRightClickMouse(CallbackInfo ci) {
        if (this.gameMode == null) return;
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
        argsOnly = true,
        //~ if < 26.1 'down' -> 'bl'
        name = "down"
    )
    public boolean handleBlockClick(boolean down) {
        if (down && this.missTime <= 0) {
            if (this.gameMode != null && MinecraftInputHook.shouldCancelContinuedBlockBreak(
                this.hitResult,
                this.gameMode.destroyBlockPos
            )) return false;
        }
        return down;
    }

    //? if < 26.2 {
    /*@Inject(
        method = "setScreen",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        new GuiScreenOpenEvent(screen).post();
    }
    *///?}
}
