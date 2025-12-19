package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.client.gui.components.PlayerTabOverlay.class)
public interface AccessorGuiPlayerTabOverlay {
    @Accessor("footer")
    Component getFooter_skyhanni();

    @Accessor("header")
    Component getHeader_skyhanni();
}
