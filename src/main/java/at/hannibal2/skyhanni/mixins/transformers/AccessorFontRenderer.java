package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FontRenderer.class)
public interface AccessorFontRenderer {

    @Accessor("red")
    float getRed();

    @Accessor("green")
    float getGreen();

    @Accessor("blue")
    float getBlue();

    @Accessor("alpha")
    float getAlpha();
}
