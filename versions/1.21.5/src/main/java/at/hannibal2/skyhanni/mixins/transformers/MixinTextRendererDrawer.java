package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextRenderer.Drawer.class)
public class MixinTextRendererDrawer {

    //#if MC < 1.21.6
    @Inject(method = "drawGlyphs", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/font/BakedGlyph$DrawnGlyph;glyph()Lnet/minecraft/client/font/BakedGlyph;"))
    //#else
    //$$ @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer$GlyphDrawer;drawGlyph(Lnet/minecraft/client/font/BakedGlyph$DrawnGlyph;)V"))
    //#endif
    private void checkIfGlyphIsChroma(CallbackInfo ci, @Local BakedGlyph.DrawnGlyph drawnGlyph) {
        ChromaFontManagerKt.checkIfGlyphIsChroma(drawnGlyph);
    }

    @ModifyVariable(method = "accept", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/text/Style;getColor()Lnet/minecraft/text/TextColor;"))
    private TextColor forceWhiteTextColorForChroma(TextColor color) {
        return ChromaFontManagerKt.forceWhiteTextColorForChroma(color);
    }

    @ModifyArg(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/BakedGlyph$DrawnGlyph;<init>(FFIILnet/minecraft/client/font/BakedGlyph;Lnet/minecraft/text/Style;FF)V"))
    private Style forceChromaIfNecessary(Style style) {
        return ChromaFontManagerKt.forceChromaStyleIfNecessary(style);
    }

}
