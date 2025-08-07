package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.features.misc.EmojiReplacer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @WrapMethod(
        method = "accept"
    )
    private boolean emojiWrapAccept(int i, Style style, int j, Operation<Boolean> original) {
        if (!EmojiReplacer.INSTANCE.handleEmojiRender(j, style, i, (TextRenderer.Drawer) (Object) this)) original.call(i, style, j);
        return true;
    }

    //#if MC < 1.21.6
    @Inject(method = "drawLayer", at = @At("HEAD"))
    public void emojiFinishDraw(float x, CallbackInfoReturnable<Float> cir) {
    //#else
    //$$ @Inject(method = "draw", at = @At("HEAD"))
    //$$ public void emojiFinishDraw(TextRenderer.GlyphDrawer glyphDrawer, CallbackInfo ci) {
    //#endif
        EmojiReplacer.INSTANCE.handleEnd((TextRenderer.Drawer) (Object) this);
    }

    @WrapOperation(
        method = "accept",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getFontStorage(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/font/FontStorage;")
    )
    public FontStorage wrapFontStorage(TextRenderer instance, Identifier id, Operation<FontStorage> original) {
        if (id == EmojiReplacer.INSTANCE.getEMOJI_IDENTIFIER()) return EmojiReplacer.INSTANCE.getFontStorage();
        return original.call(instance, id);
    }

}
