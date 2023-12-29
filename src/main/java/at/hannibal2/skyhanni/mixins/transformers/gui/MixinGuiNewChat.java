package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.features.chat.ChatPeek;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    @Inject(method = "getChatOpen", at = @At("HEAD"), cancellable = true)
    public void onIsOpen(CallbackInfoReturnable<Boolean> cir) {
        if (ChatPeek.peek()) cir.setReturnValue(true);
    }
}
