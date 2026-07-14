package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.PlaySoundEvent;
import at.hannibal2.skyhanni.utils.BypassMaximumVolumeSound;
import at.hannibal2.skyhanni.utils.LorenzVec;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
    @Shadow
    @Final
    private Options options;

    @Shadow
    @Final
    private Object2FloatMap<SoundSource> gainBySource;

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

    @WrapOperation(
        method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"
        )
    )
    private float skyhanni$useRealVolume(
        SoundEngine instance,
        float volume,
        SoundSource source,
        Operation<Float> original,
        SoundInstance soundInstance
    ) {
        // Skips the clamping from 0 to 1
        if (soundInstance instanceof BypassMaximumVolumeSound sound) {
            return sound.getVolume()
                * Mth.clamp(this.options.getFinalSoundSourceVolume(source), 0.0F, 1.0F)
                * this.gainBySource.getFloat(source);
        }

        return original.call(instance, volume, source);
    }
}
