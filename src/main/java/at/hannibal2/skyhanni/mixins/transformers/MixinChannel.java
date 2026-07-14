package at.hannibal2.skyhanni.mixins.transformers;

import com.mojang.blaze3d.audio.Channel;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public class MixinChannel {

    @Shadow
    @Final
    private int source;

    @Inject(method = "setVolume", at = @At("RETURN"))
    private void check(float volume, CallbackInfo ci) {
        // This check is so that it only for sounds that bypassed the max volume limit.
        // a 1000 was chosen as that is the value AL10.alGetInteger(AL_GAIN_LIMIT_SOFT) gives
        if (volume > 1.0f) {
            AL10.alSourcef(source, AL10.AL_MAX_GAIN, 1000.0f);
        }
    }
}
