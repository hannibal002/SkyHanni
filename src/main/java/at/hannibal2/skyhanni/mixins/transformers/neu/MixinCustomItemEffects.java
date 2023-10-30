package at.hannibal2.skyhanni.mixins.transformers.neu;

import at.hannibal2.skyhanni.mixins.hooks.neu.CustomItemEffectsHook;
import io.github.moulberry.notenoughupdates.miscfeatures.CustomItemEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CustomItemEffects.class, remap = false)
public class MixinCustomItemEffects {

    @Redirect(method = "getSensMultiplier", at = @At(value = "INVOKE"))
    private float getSensMultiplier_skyhanni() {
        return CustomItemEffectsHook.getSensMultiplier();
    }
}
