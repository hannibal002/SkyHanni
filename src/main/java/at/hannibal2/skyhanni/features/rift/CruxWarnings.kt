package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import net.minecraft.client.Minecraft
import kotlin.concurrent.fixedRateTimer

class CruxWarnings {
    private val shyNames = arrayOf("I'm ugly! :(", "Eek!", "Don't look at me!", "Look away!")

    init {
        fixedRateTimer(name = "skyhanni-shywarner-timer", period = 250) {
            Minecraft.getMinecraft().thePlayer ?: return@fixedRateTimer
            checkForShy()
        }
    }

    private fun checkForShy() {
        try {
            if (!(RiftAPI.inRift() || !SkyHanniMod.feature.rift.crux.shyWarning)) return
            val world = Minecraft.getMinecraft().theWorld ?: return
            val loadedEntityList = world.getLoadedEntityList() ?: return
            for (entity in loadedEntityList) {
                if (entity.name in shyNames && entity.distanceToPlayer() < 8) {
                    TitleUtils.sendTitle("§eLook away!", 250)
                }
            }
        } catch (e: Throwable) {
            CopyErrorCommand.logError(e, "Check for Shy failed")
        }
    }
}