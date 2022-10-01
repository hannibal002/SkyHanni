package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderGlobalHook;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Inject(method = "spawnParticle(IZDDDDDD[I)V", at = @At("HEAD"), cancellable = true)
    private void spawnParticle(int particleId, boolean ignoreRange, double x, double y, double z, double xOffset, double yOffset, double zOffset, int[] p_180442_15_, CallbackInfo ci) {
        RenderGlobalHook.spawnParticle(particleId, x, y, z, ci);
    }
}
