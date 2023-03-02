package at.hannibal2.skyhanni.mixins.transformers.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {

    @Accessor("guiLeft")
    int getGuiLeft();

    @Accessor("guiTop")
    int getGuiTop();

}
