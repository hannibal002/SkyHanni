package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.PlaySoundEvent;
import at.hannibal2.skyhanni.mixins.hooks.SoundEngineHook;
import at.hannibal2.skyhanni.utils.LorenzVec;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
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
            ).post().isCancelled()
        ) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @ModifyExpressionValue(
        method = "play",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"
        )
    )
    public float overrideSkyHanniSoundVolume(float original, @Local(argsOnly = true) SoundInstance soundInstance) {
        return SoundEngineHook.modifyVolume(soundInstance, original);
    }

    @ModifyReturnValue(
        method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
        at = @At("RETURN")
    )
    public float overrideSkyHanniSoundVolumeOnRefresh(float original, @Local(argsOnly = true) SoundInstance soundInstance) {
        return SoundEngineHook.modifyVolume(soundInstance, original);
    }
}
