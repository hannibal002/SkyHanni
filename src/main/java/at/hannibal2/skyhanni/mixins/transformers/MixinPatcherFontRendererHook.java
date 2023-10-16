package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "club.sk1er.patcher.hooks.FontRendererHook")
public class MixinPatcherFontRendererHook {

    @Inject(method = "renderStringAtPos(Ljava/lang/String;Z)Z", at = @At("HEAD"), cancellable = true)
    public void overridePatcherFontRenderer(String string, boolean shadow, CallbackInfoReturnable<Boolean> cir) {
        if (SkyHanniMod.getFeature().chroma.enabled) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }
}
