package at.hannibal2.skyhanni.mixins.transformers.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AccessorHandledScreen {

    @Accessor("leftPos")
    int getGuiLeft();

    @Invoker("slotClicked")
    void handleMouseClick_skyhanni(Slot slot, int slotId, int button, ClickType actionType);

    @Accessor("topPos")
    int getGuiTop();

    @Invoker("renderBg")
    void invokeDrawGuiContainerBackgroundLayer_skyhanni(GuiGraphics context, float deltaTicks, int mouseX, int mouseY);

    @Accessor("imageWidth")
    int getWidth();

    @Accessor("imageHeight")
    int getHeight();
}
