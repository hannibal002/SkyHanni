package at.hannibal2.skyhanni.mixins.transformers.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface AccessorHandledScreen {

    @Accessor("x")
    int getGuiLeft();

    @Invoker("onMouseClick")
    void handleMouseClick_skyhanni(Slot slot, int slotId, int button, SlotActionType actionType);

    @Accessor("y")
    int getGuiTop();

    @Invoker("drawBackground")
    void invokeDrawGuiContainerBackgroundLayer_skyhanni(DrawContext context, float deltaTicks, int mouseX, int mouseY);

    @Accessor("backgroundWidth")
    int getWidth();

    @Accessor("backgroundHeight")
    int getHeight();
}
