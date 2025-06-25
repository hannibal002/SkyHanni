package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import net.minecraft.client.gui.hud.InGameHud;
    import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    public void disableEffects(CallbackInfo ci) {
        if (SkyHanniMod.feature.misc.hideStatusEffects) ci.cancel();
    }
}
