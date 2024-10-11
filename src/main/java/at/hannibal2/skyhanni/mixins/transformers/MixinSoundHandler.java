package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.SoundHandlerHookKt;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundHandler.class)
public class MixinSoundHandler {

	@Shadow
	@Final
	private SoundManager sndManager;

	@Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
	public void playSound(ISound sound, CallbackInfo ci) {
        SoundHandlerHookKt.playSoundHook(sound, ci, sndManager);
	}
}
