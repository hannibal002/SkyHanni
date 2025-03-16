package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.EmojiReplacer;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiTextField.class)
public class MixinGuiTextField {
    /**
     * Replace emojis with the name when pasted from an external source
     */
    @ModifyArg(method = "writeText", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatAllowedCharacters;filterAllowedCharacters(Ljava/lang/String;)Ljava/lang/String;"), index = 0)
    public String replaceInvalidEmojis(String input) {
        return EmojiReplacer.INSTANCE.replaceEmojis(input);
    }
}
