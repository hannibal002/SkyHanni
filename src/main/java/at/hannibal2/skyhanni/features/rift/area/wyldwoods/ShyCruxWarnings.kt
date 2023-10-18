package at.hannibal2.skyhanni.features.rift.area.wyldwoods

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class ShyCruxWarnings {
    private val config get() = RiftAPI.config.area.wyldWoodsConfig
    private val shyNames = arrayOf("I'm ugly! :(", "Eek!", "Don't look at me!", "Look away!")

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!RiftAPI.inRift() || !config.shyWarning) return
        if (event.isMod(2)) {
            checkForShy()
        }
    }

    private fun checkForShy() {
        if (EntityUtils.getAllEntities().any { it.name in shyNames && it.distanceToPlayer() < 8 }) {
            LorenzUtils.sendTitle("§eLook away!", 150.milliseconds)
        }
    }
}