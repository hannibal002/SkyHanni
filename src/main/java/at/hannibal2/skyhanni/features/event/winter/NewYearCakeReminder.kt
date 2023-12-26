package at.hannibal2.skyhanni.features.event.winter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class NewYearCakeReminder {
    private val config get() = SkyHanniMod.feature.event.winter
    private val sidebarDetectionPattern by RepoPattern.pattern(
        "event.winter.newyearcake.reminder.sidebar",
        "§dNew Year Event!§f (?<time>.*)"
    )

    private var cakeTime = false
    private var lastReminderSend = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§aYou claimed a §r§cNew Year Cake§r§a!") {
            makedClaimed()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        //  cake already claimed
        if (event.inventoryName == "Baker") {
            makedClaimed()
        }
    }

    private fun makedClaimed() {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        playerSpecific.winter.cakeCollectedYear = SkyBlockTime.now().year
    }

    private fun isClaimed(): Boolean {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return false
        return playerSpecific.winter.cakeCollectedYear == SkyBlockTime.now().year
    }

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        cakeTime = event.newList.any { sidebarDetectionPattern.matches(it) }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.repeatSeconds(1)) {
            check()
        }
    }

    private fun check() {
        if (!cakeTime) return
        if (!config.newYearCakeReminder) return
        if (ReminderUtils.isBusy()) return
        if (isClaimed()) return

        if (lastReminderSend.passedSince() < 30.seconds) return
        lastReminderSend = SimpleTimeMark.now()

        LorenzUtils.clickableChat(
            "Reminding you to grab the free New Year Cake. Click here to open the baker menu!",
            "openbaker"
        )
    }
}
