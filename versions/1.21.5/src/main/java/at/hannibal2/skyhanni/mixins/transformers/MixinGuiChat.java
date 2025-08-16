package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chat.CopyChat;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class MixinGuiChat {

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    //#if MC < 1.21.9
    public void mouseClicked(double mouseX, double mouseY, int mouseButton, CallbackInfoReturnable<Boolean> cir) {
        //#else
        //$$ public void mouseClicked(double mouseX, double mouseY, int mouseButton, boolean bl,CallbackInfoReturnable<Boolean> cir) {
        //#endif
        if (mouseButton != 1) return;
        CopyChat.handleCopyChat((int) mouseX, (int) mouseY);
    }

}
