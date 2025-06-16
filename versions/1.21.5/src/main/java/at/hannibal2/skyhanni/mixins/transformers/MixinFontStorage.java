package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.RenderableGlyph;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontStorage.class)
public class MixinFontStorage {

    @Inject(method = "bake(Lnet/minecraft/client/font/RenderableGlyph;)Lnet/minecraft/client/font/BakedGlyph;", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void onTextRenderLayerSetCreation(RenderableGlyph glyph, CallbackInfoReturnable<BakedGlyph> cir, @Local Identifier identifier, @Local TextRenderLayerSet textRenderLayerSet) {
        ChromaFontManagerKt.addTextRenderLayerSet(identifier, textRenderLayerSet);
    }

}
