package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
//#if MC < 1.21
import net.minecraft.client.gui.inventory.GuiEditSign;
//#else
//$$ import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
//#endif

//#if MC < 1.21
@Mixin(GuiEditSign.class)
//#else
//$$ @Mixin(AbstractSignEditScreen.class)
//#endif
public interface AccessorGuiEditSign {
    @Accessor("tileSign")
    TileEntitySign getTileSign();

    @Accessor("editLine")
    int getEditLine();
}
