package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderLayers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderLayerSet.class)
public class MixinTextRenderLayerSet {

    @Unique
    private Identifier skyhanni$identifier;

    @ModifyReturnValue(method = {"of", "ofIntensity"}, at = @At("RETURN"))
    private static TextRenderLayerSet ofMethods(TextRenderLayerSet original, @Local(argsOnly = true) Identifier identifier) {
        ((MixinTextRenderLayerSet) (Object) original).skyhanni$identifier = identifier;
        return original;
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void getRenderLayer(CallbackInfoReturnable<RenderLayer> cir) {
        if (ChromaFontManagerKt.getGlyphIsChroma()) {
            cir.setReturnValue(SkyHanniRenderLayers.INSTANCE.getChromaTexturedWithIdentifier(skyhanni$identifier));
        }
    }

}
