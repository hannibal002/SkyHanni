package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.font.TextRenderable;

@Mixin(Font.PreparedTextBuilder.class)
public class MixinTextRendererDrawer {

    @Inject(method = "visit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font$GlyphVisitor;acceptGlyph(Lnet/minecraft/client/gui/font/TextRenderable;)V"))
    private void checkIfGlyphIsChroma(CallbackInfo ci, @Local TextRenderable textDrawable) {
        if (textDrawable instanceof BakedSheetGlyph.GlyphInstance drawnGlyph) {
            ChromaFontManagerKt.checkIfGlyphIsChroma(drawnGlyph);
        }
    }

    @WrapOperation(method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;getColor()Lnet/minecraft/network/chat/TextColor;"))
    private TextColor wrapGetColor(Style original, Operation<TextColor> operation) {
        return ChromaFontManagerKt.forceWhiteTextColorForChroma(original.getColor());
    }

    @ModifyArg(method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;createGlyph(FFIILnet/minecraft/network/chat/Style;FF)Lnet/minecraft/client/gui/font/TextRenderable;"))
    private Style forceChromaIfNecessary(Style style) {
        return ChromaFontManagerKt.forceChromaStyleIfNecessary(style);
    }

}
