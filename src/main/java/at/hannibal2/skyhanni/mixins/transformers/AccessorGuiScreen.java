package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;

/**
 * Not needed in 1.21 as that version natively supports emoji input
 */
@Mixin(GuiScreen.class)
public interface AccessorGuiScreen {

    @Invoker("keyTyped")
    void keyTyped_skyhanni(char typedChar, int keyCode) throws IOException;

}
