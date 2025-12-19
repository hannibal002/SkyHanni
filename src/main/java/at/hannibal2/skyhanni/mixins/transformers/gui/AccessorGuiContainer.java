package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SkyHanniGuiContainer.class)
public interface AccessorGuiContainer {

    @Accessor("guiLeft")
    int getGuiLeft();

    @Invoker("handleMouseClick")
    void handleMouseClick_skyhanni(Slot slotIn, int slotId, int clickedButton, int clickType);

    @Accessor("guiTop")
    int getGuiTop();

    @Invoker("drawGuiContainerBackgroundLayer")
    void invokeDrawGuiContainerBackgroundLayer_skyhanni(float f, int i, int mouseY);

    @Accessor("xSize")
    int getWidth();

    @Accessor("ySize")
    int getHeight();
}
