package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.ChatHoverEvent;
import at.hannibal2.skyhanni.features.commands.tabcomplete.TabComplete;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(GuiChat.class)
public class MixinGuiChat {

    @Shadow
    protected GuiTextField inputField;

    @Shadow
    private boolean waitingOnAutocomplete;

    @Shadow
    private boolean playerNamesFound;

    @Shadow
    private List<String> foundPlayerNames = Lists.newArrayList();

    @Inject(method = "onAutocompleteResponse", at = @At(value = "HEAD"), cancellable = true)
    private void renderItemOverlayPost(String[] originalArray, CallbackInfo ci) {

        if (this.waitingOnAutocomplete) {
            String[] result = TabComplete.handleTabComplete(this.inputField.getText(), originalArray);
            if (result == null) return;
            ci.cancel();

            this.playerNamesFound = false;
            this.foundPlayerNames.clear();
            for (String s : result) {
                if (!s.isEmpty()) {
                    this.foundPlayerNames.add(s);
                }
            }

            String s1 = this.inputField.getText().substring(this.inputField.func_146197_a(-1, this.inputField.getCursorPosition(), false));
            String s2 = StringUtils.getCommonPrefix(result);
            s2 = EnumChatFormatting.getTextWithoutFormattingCodes(s2);
            if (!s2.isEmpty() && !s1.equalsIgnoreCase(s2)) {
                this.inputField.deleteFromCursor(this.inputField.func_146197_a(-1, this.inputField.getCursorPosition(), false) - this.inputField.getCursorPosition());
                this.inputField.writeText(s2);
            } else if (!this.foundPlayerNames.isEmpty()) {
                this.playerNamesFound = true;
            }
        }
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;handleComponentHover(Lnet/minecraft/util/IChatComponent;II)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void chatHoverEvent(int mouseX, int mouseY, float partialTicks, CallbackInfo ci, IChatComponent component) {
        new ChatHoverEvent(component.getChatStyle().getChatHoverEvent().getAction(), component.getChatStyle().getChatHoverEvent().getValue()).postAndCatch();
    }
}
