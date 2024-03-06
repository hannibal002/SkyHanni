package at.hannibal2.skyhanni.mixins.transformers.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {

    @Accessor("guiLeft")
    int getGuiLeft();

    @Invoker("handleMouseClick")
    void handleMouseClick_skyhanni(Slot slotIn, int slotId, int clickedButton, int clickType);

    @Accessor("guiTop")
    int getGuiTop();

}
