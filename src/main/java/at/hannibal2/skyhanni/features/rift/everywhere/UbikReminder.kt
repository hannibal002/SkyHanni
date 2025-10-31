package at.hannibal2.hanni.features.rift.everywhere

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.hours

@HanniModule
object UbikReminder {

    private val config get() = RiftApi.config.area.mountaintop

    private var nextRemindTime = SimpleTimeMark.farFuture()
    private val patternGroup = RepoPattern.group("rift.ubik")

    /**
     * REGEX-TEST: §6§lROUND 7 §r§6(§r§lFINAL§r§6)§r§l: §r§eYou chose §r§c§lSTEAL §r§eand gained §r§55,000 Motes§r§e!
     */
    private val ubikRoundPattern by patternGroup.pattern(
        "reminder",
        "§6§lROUND [5-9] §r§6\\(§r§lFINAL§r§6\\)§r§l: §r§eYou chose .*",
    )

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChat(event: HanniChatEvent) {
        if (!config.ubikReminder) return
        if (ubikRoundPattern.matches(event.message)) {
            nextRemindTime = 2.hours.fromNow()
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (nextRemindTime.isInFuture()) return
        if (config.ubikReminder) {
            ChatUtils.chat("§aUbik's Cube is ready in the Rift!")
        }
        nextRemindTime = SimpleTimeMark.farFuture()
    }
}
