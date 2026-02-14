package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.PlaySoundEvent;
import at.hannibal2.skyhanni.utils.LorenzVec;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    @Inject(
        method = "play",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"
        ),
        cancellable = true
    )
    public void handleSound(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (
            new PlaySoundEvent(
                StringUtils.removeStart(soundInstance.getIdentifier().toString(), "minecraft:"),
                new LorenzVec(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ()),
                soundInstance.getPitch(),
                soundInstance.getVolume()
            ).post()
        ) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

}
