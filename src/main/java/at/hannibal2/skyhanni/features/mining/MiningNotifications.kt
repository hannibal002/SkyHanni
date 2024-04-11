package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class MiningNotifications {

    enum class NotificationList(val str: String, val notification: String) {
        MINESHAFT_SPAWN("§bGlacite Mineshaft", "§bMineshaft"),
        SCRAP("§9Suspicious Scrap", "§9Suspicious Scrap"),
        GOLDEN_GOBLIN("§6Golden Goblin", "§6Golden Goblin"),
        DIAMOND_GOBLIN("§bDiamond Goblin", "§bDiamond Goblin");

        override fun toString(): String {
            return str
        }
    }

    private val patternGroup = RepoPattern.group("mining.notifications")
    private val mineshaftSpawn by patternGroup.pattern(
        "mineshaft.spawn",
        "§5§lWOW! §r§aYou found a §r§bGlacite Mineshaft §r§aportal!"
    )
    private val scrapDrop by patternGroup.pattern(
        "scrapdrop",
        "§6§lEXCAVATOR! §r§fYou found a §r§9Suspicious Scrap§r§f!"
    )
    private val goldenGoblinSpawn by patternGroup.pattern(
        "goblin.goldspawn",
        "§6A Golden Goblin has spawned!"
    )
    private val diamondGoblinSpawn by patternGroup.pattern(
        "goblin.diamondspawn",
        "§6A §r§bDiamond Goblin §r§6has spawned!"
    )

    private val config get() = SkyHanniMod.feature.mining.notifications

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.inMiningIsland()) return
        val message = event.message
        when {
            mineshaftSpawn.matches(message) -> sendNotification(NotificationList.MINESHAFT_SPAWN)
            scrapDrop.matches(message) -> sendNotification(NotificationList.SCRAP)
            goldenGoblinSpawn.matches(message) -> sendNotification(NotificationList.GOLDEN_GOBLIN)
            diamondGoblinSpawn.matches(message) -> sendNotification(NotificationList.DIAMOND_GOBLIN)
        }
    }

    private fun sendNotification(type: NotificationList) {
        if (!config.notifications.contains(type)) return
        LorenzUtils.sendTitle(type.notification, 1500.milliseconds)
        if (config.playSound) SoundUtils.playPlingSound()
    }
}