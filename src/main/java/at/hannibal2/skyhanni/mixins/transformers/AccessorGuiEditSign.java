package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.inventory.GuiEditSign;

//#if MC < 1.21
@Mixin(GuiEditSign.class)
public interface AccessorGuiEditSign {
    @Accessor("tileSign")
    TileEntitySign getTileSign();

    @Accessor("editLine")
    int getCurrentRow();
}
//#endif
