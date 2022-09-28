package at.hannibal2.skyhanni.mixins.transformers.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {

    @Invoker("getSlotAtPosition")
    Slot doGetSlotAtPosition(int x, int y);

    @Invoker("drawSlot")
    void doDrawSlot(Slot slot);

    @Invoker("isMouseOverSlot")
    boolean doIsMouseOverSlot(Slot slot, int x, int y);

    @Accessor("guiLeft")
    int getGuiLeft();

    @Accessor("guiTop")
    int getGuiTop();

    @Accessor("xSize")
    int getXSize();

    @Accessor("ySize")
    int getYSize();

}
