package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.LorenzUtils;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NotEnoughUpdates.class, remap = false)
public class MixinNotEnoughUpdates {

    @Shadow
    private boolean hasSkyblockScoreboard;

    @Inject(method = "updateSkyblockScoreboard", at = @At(value = "TAIL"))
    private void onLivingUpdate(CallbackInfo ci) {
        hasSkyblockScoreboard = LorenzUtils.INSTANCE.getInSkyBlock();
    }
}
