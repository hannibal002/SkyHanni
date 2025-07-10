package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.data.IslandType;
import at.hannibal2.skyhanni.utils.SkyBlockUtils;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class MixinSoundSystem {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance sound, CallbackInfo ci) {
        if (sound == null) return;
        if (sound.getId() == null) return;
        String soundId = sound.getId().toString();
        if (SkyHanniMod.feature.getForaging().getMutePhantoms() && IslandType.GALATEA.isCurrent()) {
            // for whatever reason canceling our actual sound event doesnt stop phantom noises
            if (soundId.contains("entity.phantom.")) ci.cancel();
        }
    }
}
