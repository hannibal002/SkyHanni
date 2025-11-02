package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC < 1.21
//$$ @Mixin(net.minecraft.client.gui.GuiPlayerTabOverlay.class)
//#else
@Mixin(net.minecraft.client.gui.hud.PlayerListHud.class)
//#endif
public interface AccessorGuiPlayerTabOverlay {
    @Accessor("footer")
    Text getFooter_skyhanni();

    @Accessor("header")
    Text getHeader_skyhanni();
}
