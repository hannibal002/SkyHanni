package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.hours

@SkyHanniModule
object UbikReminder {

    private val config get() = RiftApi.config.area.mountaintop

    private var nextRemindTime = SimpleTimeMark.farPast()
    private val patternGroup = RepoPattern.group("rift.ubik")

    /**
     * REGEX-TEST: §6§lROUND 7 §r§6(§r§lFINAL§r§6)§r§l: §r§eYou chose §r§c§lSTEAL §r§eand gained §r§55,000 Motes§r§e!
     */
    private val ubikRoundPattern by patternGroup.pattern(
        "reminder",
        "§6§lROUND [5-9] §r§6\\(§r§lFINAL§r§6\\)§r§l: §r§eYou chose .*",
    )

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.ubikReminder) return
        if (ubikRoundPattern.matches(event.message)) {
            nextRemindTime = 2.hours.fromNow()
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (nextRemindTime.isInPast()) return
        if (config.ubikReminder) {
            ChatUtils.chat("§aUbik's cube is ready in the rift!")
        }
        nextRemindTime = SimpleTimeMark.farPast()
    }
}
