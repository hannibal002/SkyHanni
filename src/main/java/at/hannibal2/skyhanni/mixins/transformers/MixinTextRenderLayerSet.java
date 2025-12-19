package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderLayers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlyphRenderTypes.class)
public class MixinTextRenderLayerSet {

    @Unique
    private ResourceLocation skyhanni$identifier;

    @ModifyReturnValue(method = {"createForColorTexture", "createForIntensityTexture"}, at = @At("RETURN"))
    private static GlyphRenderTypes ofMethods(GlyphRenderTypes original, @Local(argsOnly = true) ResourceLocation identifier) {
        ((MixinTextRenderLayerSet) (Object) original).skyhanni$identifier = identifier;
        return original;
    }

    @Inject(method = "select", at = @At("HEAD"), cancellable = true)
    private void getRenderLayer(CallbackInfoReturnable<RenderType> cir) {
        if (ChromaFontManagerKt.getGlyphIsChroma()) {
            cir.setReturnValue(SkyHanniRenderLayers.INSTANCE.getChromaTexturedWithIdentifier(skyhanni$identifier));
        }
    }

}
