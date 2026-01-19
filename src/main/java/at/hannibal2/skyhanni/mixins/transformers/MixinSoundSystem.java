package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.data.IslandType;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? < 1.21.6 {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} else {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

@Mixin(SoundEngine.class)
public class MixinSoundSystem {

    //? < 1.21.6 {
    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance sound, CallbackInfo ci) {
        //?} else {
        /*@Inject(method = "play", at = @At("HEAD"), cancellable = true)
        public void play(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        *///?}
        if (sound == null) return;
        if (sound.getLocation() == null) return;
        String soundId = sound.getLocation().toString();
        if (IslandType.GALATEA.isCurrent() && SkyHanniMod.feature.getForaging().getMutePhantoms()) {
            // for whatever reason canceling our actual sound event doesn't stop phantom noises
            //? < 1.21.6 {
            if (soundId.contains("entity.phantom.")) ci.cancel();
            //?} else {
            /*if (soundId.contains("entity.phantom.")) cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            *///?}
        }
    }
}
