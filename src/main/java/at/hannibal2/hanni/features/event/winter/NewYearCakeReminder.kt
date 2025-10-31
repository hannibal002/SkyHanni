package at.hannibal2.hanni.features.event.winter

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.ScoreboardData
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.fame.ReminderUtils
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockTime
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@HanniModule
object NewYearCakeReminder {

    private val config get() = HanniMod.feature.event.winter
    private val sidebarDetectionPattern by RepoPattern.pattern(
        "event.winter.newyearcake.reminder.sidebar",
        "§dNew Year Event!§f (?<time>.*)",
    )
    private var lastReminderSend = SimpleTimeMark.farPast()

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (event.message == "§aYou claimed a §r§cNew Year Cake§r§a!") {
            markCakeClaimed()
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        //  cake already claimed
        if (event.inventoryName == "Baker") {
            markCakeClaimed()
        }
    }

    private fun markCakeClaimed() {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        playerSpecific.winter.cakeCollectedYear = SkyBlockTime.now().year
    }

    private fun isClaimed(): Boolean {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return false
        return playerSpecific.winter.cakeCollectedYear == SkyBlockTime.now().year
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.newYearCakeReminder) return
        if (!isCakeTime()) return
        if (ReminderUtils.isBusy()) return
        if (isClaimed()) return

        if (lastReminderSend.passedSince() < 30.seconds) return
        lastReminderSend = SimpleTimeMark.now()
        ChatUtils.clickToActionOrDisable(
            "Reminding you to grab the free New Year Cake. Click here to open the baker menu!",
            config::newYearCakeReminder,
            actionName = "open the baker menu",
            action = { HypixelCommands.openBaker() },
        )
    }

    private fun isCakeTime() = ScoreboardData.sidebarLinesFormatted.any { sidebarDetectionPattern.matches(it) }
}
