package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.SpawnParticleEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class RenderGlobalHook {
    companion object {
        fun spawnParticle(
            particleId: Int,
            x: Double,
            y: Double,
            z: Double,
            ci: CallbackInfo,
        ) {
            val callerClass = LorenzUtils.getCallerClass(
                "at.hannibal2.skyhanni.mixins.hooks.RenderGlobalHook\$Companion",
                "at.hannibal2.skyhanni.mixins.hooks.RenderGlobalHook",
                "net.minecraft.client.renderer.RenderGlobal",
                "net.minecraft.world.World",
                "net.minecraft.client.network.NetHandlerPlayClient",
                "net.minecraft.network.play.server.S2APacketParticles",
            ) ?: "null"

            if (SpawnParticleEvent(particleId, callerClass, x, y, z).postAndCatch()) {
                ci.cancel()
                return
            }

        }
    }
}