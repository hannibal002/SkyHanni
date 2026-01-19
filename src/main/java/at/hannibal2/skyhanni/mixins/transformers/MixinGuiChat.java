package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chat.CopyChat;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? > 1.21.8 {
/*import net.minecraft.client.input.MouseButtonEvent;
*///?}

@Mixin(ChatScreen.class)
public class MixinGuiChat {

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    //? < 1.21.9 {
    public void mouseClicked(double mouseX, double mouseY, int mouseButton, CallbackInfoReturnable<Boolean> cir) {
        if (mouseButton != 1) return;
        CopyChat.handleCopyChat((int) mouseX, (int) mouseY);
    }
    //?} else {
    /*public void mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
       if (click.button() != 1) return;
       CopyChat.handleCopyChat((int) click.x(), (int) click.y());
    }
    *///?}

}
