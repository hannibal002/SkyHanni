package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import kotlin.concurrent.fixedRateTimer

class CruxWarnings {
    private val shyNames = arrayOf("I'm ugly! :(", "Eek!", "Don't look at me!", "Look away!")

    init {
        fixedRateTimer(name = "skyhanni-shywarner-timer", period = 250) {
            checkForShy()
        }
    }

    private fun checkForShy() {
        try {
            if (!(RiftAPI.inRift() || !SkyHanniMod.feature.rift.crux.shyWarning)) return
            val world = Minecraft.getMinecraft().theWorld ?: return
            for (entity in world.getLoadedEntityList()) {
                val name = entity.name
                if (shyNames.any { it == name }) {
                    if (entity.getLorenzVec().distanceToPlayer() < 8) {
                        TitleUtils.sendTitle("§eLook away!", 250)
                    }
                }
            }
        } catch (_: Throwable) {}
    }
}