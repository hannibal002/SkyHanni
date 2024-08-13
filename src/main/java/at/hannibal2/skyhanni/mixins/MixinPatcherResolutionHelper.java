package at.hannibal2.skyhanni.mixins;

import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "club.sk1er.patcher.screen.ResolutionHelper")
public class MixinPatcherResolutionHelper {

    @Inject(method = "Lclub/sk1er/patcher/screen/ResolutionHelper;getScaleOverride()I", at = @At("HEAD"), cancellable = true)
    private void getScaleOverride(CallbackInfoReturnable<Integer> cir) {
        if (CustomWardrobe.INSTANCE.isEnabled()) cir.setReturnValue(-1);
    }

}
