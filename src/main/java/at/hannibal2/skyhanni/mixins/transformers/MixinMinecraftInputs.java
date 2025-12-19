package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MinecraftInputHook;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(MinecraftClient.class)
public class MixinMinecraftInputs {

    @Shadow
    public HitResult crosshairTarget;

    @Shadow
    private int attackCooldown;

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Inject(
        at = @At("HEAD"),
        method = "doItemUse",
        cancellable = true
    )
    public void handleRightClickMouse(CallbackInfo ci) {
        if (this.interactionManager.isBreakingBlock()) return;

        if (MinecraftInputHook.shouldCancelMouseRightClick(this.crosshairTarget)) ci.cancel();
    }

    @Inject(
        at = @At("HEAD"),
        method = "doAttack",
        cancellable = true
    )
    public void handleLeftClickMouse(
        //#if MC < 1.21
        //$$ CallbackInfo ci
        //#else
        CallbackInfoReturnable<Boolean> cir
        //#endif
    ) {
        if (this.attackCooldown > 0) return;

        if (MinecraftInputHook.shouldCancelMouseLeftClick(this.crosshairTarget))
            //#if MC < 1.21
            //$$ ci.cancel();
        //#else
        cir.setReturnValue(false);
        //#endif
    }

    @ModifyVariable(
        at = @At(value = "HEAD"),
        method = "handleBlockBreaking",
        argsOnly = true
    )
    public boolean handleBlockClick(boolean isLeftClick) {
        if (isLeftClick && this.attackCooldown <= 0) {
            if (MinecraftInputHook.shouldCancelContinuedBlockBreak(
                this.crosshairTarget,
                ((AccessorPlayerControllerMP) this.interactionManager).skyhanni_getCurrentBlock()
            )) return false;
        }
        return isLeftClick;
    }
}
