package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hotx.HotxPatterns.asPatternId
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.mining.MiningEventEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.intellij.lang.annotations.Language
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object MiningEventsApi {
    init {
        MiningEventType.entries.forEach {
            it.widgetEventPattern
            it.bossBarPattern
            it.chatPattern
        }
    }
    data class MiningEvent(
        val type: MiningEventType,
        var timeLeft: Duration,
    )

    var activeMiningEvent: MiningEvent? = null

    fun getActiveEvent(type: MiningEventType? = null): MiningEvent? {
        val event = activeMiningEvent ?: return null

        return if (type == null || event.type == type) event else null
    }

    fun isMiningEventActive(type: MiningEventType): Boolean =
        getActiveEvent(type) != null

    private fun setActiveMiningEvent(eventType: MiningEventType, timeLeft: Duration) {
        val current = activeMiningEvent

        if (current == null) {
            val newEvent = MiningEvent(type = eventType, timeLeft = timeLeft)
            activeMiningEvent = newEvent
            MiningEventEvent.Started(newEvent).post()
            return
        }

        // edge case that I think can only happen if you swap servers to something with a different running event.
        if (current.type != eventType) {
            MiningEventEvent.Ended(current).post()

            val newEvent = MiningEvent(type = eventType, timeLeft = timeLeft)
            activeMiningEvent = newEvent
            MiningEventEvent.Started(newEvent).post()
            return
        }

        current.timeLeft = timeLeft
    }

    private fun clearActiveMiningEvent() {
        val current = activeMiningEvent
        if (current != null) {
            MiningEventEvent.Ended(current).post()
            activeMiningEvent = null
        }
    }


    @HandleEvent(onlyOnIslands = [IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES])
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val msg = event.message

        for (eventType in MiningEventType.entries) {
            eventType.chatPattern.matchMatcher(msg) {
                if (msg.contains("STARTED")) {
                    setActiveMiningEvent(eventType, eventType.duration)
                } else if (msg.contains("ENDED")) {
                    clearActiveMiningEvent()
                }
                return
            }
        }
    }

    @HandleEvent(onlyOnIslands = [IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES])
    fun onSecondPassed() {

        if (TabWidget.EVENT.isActive) {


            for (eventType in MiningEventType.entries) {
                for ((index, line) in TabWidget.EVENT.lines.withIndex()) {
                    MiningEventType.widgetEventNotAnnouncedPattern.matchMatcher(line) {
                        clearActiveMiningEvent()
                        return
                    }
                    eventType.widgetEventPattern.matchMatcher(line) {
                        val durationLine = TabWidget.EVENT.lines.getOrNull(index + 1) ?: return

                        MiningEventType.widgetDurationPattern.matchMatcher(durationLine) {
                            val durationString = group("duration")
                            setActiveMiningEvent(eventType, TimeUtils.getDuration(durationString))
                        }

                        return
                    }
                }
            }
        } else {

            val bossbar = BossbarData.getBossbar()
            if (bossbar.isEmpty()) {
                // There is two approaches here:
                // A: Clear the active mining event when the bossbar is empty, this could be problematic because if you have the no bossbar
                // glitch appear then no events would be detected if you are relying on just the bossbar
                // B: Don't clear the bossbar. This would keep the event set as the active event until it gets swapped over for the next
                // event. Pretty much it could be detected as double powder until a new event happens even when the double powder event is
                // actually inactive.

                // There might be a third approach that I am unaware of. But that is what I have for now.

                // clearActiveMiningEvent()
                return
            }

            for (eventType in MiningEventType.entries) {
                eventType.bossBarPattern.matchMatcher(bossbar) {

                    val durationString = group("time")
                    val newDuration = TimeUtils.getDuration(durationString)

                    if (newDuration > Duration.ZERO) {
                        setActiveMiningEvent(eventType, newDuration)
                    }

                    return
                }
            }
        }
    }

    enum class MiningEventType(
        val duration: Duration,
        @Language("RegExp") val chatFallback: String,
        @Language("RegExp") val widgetEventFallback: String,
        @Language("RegExp") val bossbarFallback: String,
    ) {
        // all work
        // §e§lEVENT §C§LGOBLIN RAID §e§lACTIVE IN §b§lGOBLIN BURROWS §e§lfor §a§l02:00§r
        GOBLIN_RAID(
            duration = 5.minutes,
            chatFallback = ".*§r§c§lGOBLIN RAID (?:STARTED|ENDED)!.*",
            widgetEventFallback = "§9§lMining Event: §r§a§r§cGoblin Raid",
            bossbarFallback = "§e§lEVENT §C§LGOBLIN RAID §e§lACTIVE IN §b§l(?<location>.*) §e§lfor §a§l(?<time>.*)§r",
        ),
        // all works
        // §e§lEVENT §6§LRAFFLE §e§lACTIVE IN §b§lDIVAN'S GATEWAY §e§lfor §a§l01:06§r
        RAFFLE(
            duration = 3.minutes,
            chatFallback = ".*§r§6§lRAFFLE (?:STARTED|ENDED)!.*",
            widgetEventFallback = "§9§lMining Event: §r§a§r§6Raffle",
            bossbarFallback = "§e§lEVENT §6§LRAFFLE §e§lACTIVE IN §b§l(?<location>.*) §e§lfor §a§l(?<time>.*)§r",
        ),
        // all works
        // like bro hypixel can you just have the color codes and text be all normal ts pmo
        // §e§lEVENT §B§LMITHRIL GOURMAND §e§lACTIVE IN §b§lGOBLIN BURROWS §e§lfor §a§l09:46§r
        MITHRIL_GOURMAND(
            duration = 10.minutes,
            chatFallback = ".*§r§b§lMITHRIL GOURMAND (?:STARTED|ENDED)!.*",
            widgetEventFallback = "§9§lMining Event: §r§a§r§bMithril Gourmand",
            bossbarFallback = "§e§lEVENT §B§LMITHRIL GOURMAND §e§lACTIVE IN §b§l(?<location>.*) §e§lfor §a§l(?<time>.*)§r",
        ),
        // all work
        BETTER_TOGETHER(
            duration = 20.minutes,
            chatFallback = ".*§r§d§lBETTER TOGETHER (?:STARTED|ENDED)!.*",
            widgetEventFallback = "§9§lMining Event: §r§a§r§dBetter Together",
            bossbarFallback = "§e§lPASSIVE EVENT §b§l§D§LBETTER TOGETHER §e§lRUNNING FOR §a§l(?<time>.*)§r",
        ),
        // all work
        GONE_WITH_THE_WIND(
            duration = 20.minutes,
            chatFallback = ".*§r§9§lGONE WITH THE WIND (?:STARTED|ENDED)!.*",
            widgetEventFallback = "§9§lMining Event: §r§a§r§9Gone with the Wind",
            bossbarFallback = "§e§lPASSIVE EVENT §9§LGONE WITH THE WIND §e§lRUNNING FOR §a§l(?<time>.*)§r",
        ),
        // all work.
        DOUBLE_POWDER(
            duration = 15.minutes,
            chatFallback = ".*§r§b§l2X POWDER (?:STARTED|ENDED)!.*",
            widgetEventFallback = "§9§lMining Event: §r§a§r§b2x Powder",
            bossbarFallback = "§e§lPASSIVE EVENT §b§l2X POWDER §e§lRUNNING FOR §a§l(?<time>.*)§r",
        ),

        ;

        private val basePath = "mining.mining.event"
        val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        val widgetEventPattern by RepoPattern.pattern("$basePath.widget.event.${asPatternId()}", widgetEventFallback)
        val bossBarPattern by RepoPattern.pattern("$basePath.bossbar.${asPatternId()}", bossbarFallback)

        companion object {
            /**
             * REGEX-TEST: Ends in: §r§c5m 30s
             */
            val widgetDurationPattern by RepoPattern.pattern(
                "mining.mining.event.widget.duration",
                "Ends in: §r§.(?<duration>.*)"
            )
            /**
             * REGEX-TEST: §9§lMining Event: §r§7Not announced
             */
            val widgetEventNotAnnouncedPattern by RepoPattern.pattern(
                "mining.mining.event.widget.not.announced",
                "§9§lMining Event: §r§7Not announced"
            )
        }
    }
}
