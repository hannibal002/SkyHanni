package at.hannibal2.skyhanni.features.rift.area.wyldwoods

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class ShyCruxWarnings {

    private val config get() = RiftAPI.config.area.wyldWoods

    private val patternGroup = RepoPattern.group("shycruxwarnings")
    private val shyNamesPatterns by patternGroup.list(
        "names",
        "I'm ugly! :\\(",
        "Eek!",
        "Don't look at me!",
        "Look away!",
    )

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!RiftAPI.inRift() || !config.shyWarning) return
        checkForShy()
    }

    private fun isShyName(name: String): Boolean {
        return shyNamesPatterns.any { it.matches(name) }
    }

    private fun checkForShy() {
        if (EntityUtils.getAllEntities().any { it.distanceToPlayer() < 8 && isShyName(it.name) }) {
            LorenzUtils.sendTitle("§eLook away!", 150.milliseconds)
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.wyldWoodsConfig", "rift.area.wyldWoods")
    }
}
