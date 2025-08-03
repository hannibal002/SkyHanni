package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.data.IslandType;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC < 1.21.6
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(SoundSystem.class)
public class MixinSoundSystem {

    //#if MC < 1.21.6
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance sound, CallbackInfo ci) {
        //#else
        //$$ @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
        //$$ public void play(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        //#endif
        if (sound == null) return;
        if (sound.getId() == null) return;
        String soundId = sound.getId().toString();
        if (IslandType.GALATEA.isCurrent() && SkyHanniMod.feature.getForaging().getMutePhantoms()) {
            // for whatever reason canceling our actual sound event doesn't stop phantom noises
            //#if MC < 1.21.6
            if (soundId.contains("entity.phantom.")) ci.cancel();
            //#else
            //$$ if (soundId.contains("entity.phantom.")) cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
            //#endif
        }
    }
}
