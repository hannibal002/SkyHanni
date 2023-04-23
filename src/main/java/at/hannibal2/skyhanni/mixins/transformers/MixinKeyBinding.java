package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.KeyBindingHookKt;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyBinding.class, remap = false)
public class MixinKeyBinding {

    private KeyBinding keyBinding = (KeyBinding) (Object) this;

    @Inject(method = "isKeyDown", at = @At(value = "HEAD"), cancellable = true)
    private void isKeyDown(CallbackInfoReturnable<Boolean> cir) {
        KeyBindingHookKt.isKeyDown(keyBinding, cir);
    }

    @Inject(method = "isPressed", at = @At(value = "HEAD"), cancellable = true)
    private void isPressed(CallbackInfoReturnable<Boolean> cir) {
        KeyBindingHookKt.isPressed(keyBinding, cir);
    }
}
