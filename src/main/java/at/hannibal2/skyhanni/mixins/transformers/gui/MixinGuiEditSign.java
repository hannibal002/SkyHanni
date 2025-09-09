package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.utils.KeyboardManager;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignEditScreen.class)
public class MixinGuiEditSign {

    // Fixes a Mac issue where keyboard shortcut characters get inserted into signs.
    // Example: "v" gets added to the sign when trying to paste.
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    public void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (KeyboardManager.INSTANCE.isModifierKeyDown()) ci.cancel();
    }
}
