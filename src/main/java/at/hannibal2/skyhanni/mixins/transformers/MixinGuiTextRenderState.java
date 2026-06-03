package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiTextRenderState.class)
public interface MixinGuiTextRenderState {

    @Accessor("font")
    Font skyhanni$getFont();

    @Accessor("text")
    FormattedCharSequence skyhanni$getText();

    @Accessor("x")
    int skyhanni$getX();

    @Accessor("y")
    int skyhanni$getY();

    @Accessor("color")
    int skyhanni$getColor();

    @Accessor("backgroundColor")
    int skyhanni$getBackgroundColor();

    @Accessor("dropShadow")
    boolean skyhanni$getDropShadow();
}
