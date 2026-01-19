package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? > 1.21.8 {
/*import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
*///?}

//? < 1.21.6 {
@Mixin(Font.StringRenderOutput.class)
//?} else
//@Mixin(Font.StringRenderOutput.class)
public class MixinTextRendererDrawer {

    //? < 1.21.9 {
    //? < 1.21.6 {
    @Inject(method = "renderCharacters", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph$GlyphInstance;glyph()Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;"))
    //?} else {
    /*@Inject(method = "visit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font$GlyphVisitor;acceptGlyph(Lnet/minecraft/client/gui/font/glyphs/BakedGlyph$GlyphInstance;)V"))
    *///?}
    private void checkIfGlyphIsChroma(CallbackInfo ci, @Local BakedGlyph.GlyphInstance drawnGlyph) {
        ChromaFontManagerKt.checkIfGlyphIsChroma(drawnGlyph);
    }
    //?} else {
    /*@Inject(method = "visit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font$GlyphVisitor;acceptGlyph(Lnet/minecraft/client/gui/font/TextRenderable;)V"))
    private void checkIfGlyphIsChroma(CallbackInfo ci, @Local TextRenderable textDrawable) {
        if (textDrawable instanceof BakedSheetGlyph.GlyphInstance drawnGlyph) {
            ChromaFontManagerKt.checkIfGlyphIsChroma(drawnGlyph);
        }
    }
    *///?}

    //? < 1.21.9 {
    @WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;getColor()Lnet/minecraft/network/chat/TextColor;"))
    //?} else {
    /*@WrapOperation(method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;getColor()Lnet/minecraft/network/chat/TextColor;"))
    *///?}
    private TextColor wrapGetColor(Style original, Operation<TextColor> operation) {
        return ChromaFontManagerKt.forceWhiteTextColorForChroma(original.getColor());
    }

    //? < 1.21.9 {
    @ModifyArg(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph$GlyphInstance;<init>(FFIILnet/minecraft/client/gui/font/glyphs/BakedGlyph;Lnet/minecraft/network/chat/Style;FF)V"))
    //?} else {
    /*@ModifyArg(method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;createGlyph(FFIILnet/minecraft/network/chat/Style;FF)Lnet/minecraft/client/gui/font/TextRenderable;"))
    *///?}
    private Style forceChromaIfNecessary(Style style) {
        return ChromaFontManagerKt.forceChromaStyleIfNecessary(style);
    }

}
