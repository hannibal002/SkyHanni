package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.SkyHanniMod;
import net.minecraft.client.gui.GuiNewChat;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    @Inject(method = "getChatOpen", at = @At("HEAD"), cancellable = true)
    public void onIsOpen(CallbackInfoReturnable<Boolean> cir) {
        if (SkyHanniMod.feature.chat.peekChat != Keyboard.KEY_NONE && Keyboard.isKeyDown(SkyHanniMod.feature.chat.peekChat))
            cir.setReturnValue(true);
    }
}
