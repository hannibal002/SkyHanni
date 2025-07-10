package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextColor.class)
public class MixinTextColor {

    @Inject(
        method = "equals",
        at = @At("TAIL"),
        cancellable = true
    )
    public void chromaEqualityFix(Object o, CallbackInfoReturnable<Boolean> cir) {
        if (ChromaFontManagerKt.isNotActuallyEqualBecauseOfChroma((TextColor)(Object)this, o)) {
            cir.setReturnValue(false);
        }
    }
}
