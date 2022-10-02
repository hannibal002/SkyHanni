package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderParticleEvent
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HideBlazeParticles {

    @SubscribeEvent
    fun onSpawnParticle(event: RenderParticleEvent) {
        val particleId = event.particleId
        if (!SkyHanniMod.feature.misc.hideBlazeParticles) return

        val particleType = EnumParticleTypes.values().find { it.particleID == particleId }
        if (particleType != EnumParticleTypes.SMOKE_LARGE) return

        val location = LorenzVec(event.x, event.y, event.z)
        val clazz = EntityBlaze::class.java
        if (Minecraft.getMinecraft().theWorld.getEntitiesNearby(clazz, location, 3.0).isNotEmpty()) {
            event.isCanceled = true
        }
    }
}