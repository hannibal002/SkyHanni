package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "Config", remap = false)
public class MixinOptifineConfig {

    @Inject(method = "isConnectedTextures", at = @At("HEAD"), cancellable = true)
    private static void isConnectedTextures(CallbackInfoReturnable<Boolean> cir) {
        if (MiningCommissionsBlocksColor.INSTANCE.getActive()) cir.setReturnValue(false);
    }

    @Inject(method = "isConnectedTexturesFancy", at = @At("HEAD"), cancellable = true)
    private static void isConnectedTexturesFancy(CallbackInfoReturnable<Boolean> cir) {
        if (MiningCommissionsBlocksColor.INSTANCE.getActive()) cir.setReturnValue(false);
    }
}
