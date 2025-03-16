package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Not needed in 1.21 as that version natively supports emoji input
 */
@Mixin(GuiChat.class)
public interface AccessorMixinGuiChat {

    @Accessor("inputField")
    GuiTextField getInputField_skyhanni();

}
