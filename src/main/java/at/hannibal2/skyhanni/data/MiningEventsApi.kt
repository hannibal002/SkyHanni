package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hotx.HotxPatterns.asPatternId
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.mining.MiningEventEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
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
            ChatUtils.debug("[Mining Events API] Event started: ${eventType.name}")
            val newEvent = MiningEvent(type = eventType, timeLeft = timeLeft)
            activeMiningEvent = newEvent
            MiningEventEvent.Started(newEvent).post()
            return
        }

        // edge case that I think can only happen if you swap servers to something with a different running event.
        if (current.type != eventType) {
            ChatUtils.debug("[Mining Events API] Event changed: ${current.type.name} -> ${eventType.name}")
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
            ChatUtils.debug("[Mining Events API] Event ended: ${current.type.name}")
            MiningEventEvent.Ended(current).post()
            activeMiningEvent = null
        }
    }


    @HandleEvent(onlyOnIslands = [IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES])
    fun onChat(event: SkyHanniChatEvent) {
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
            for ((line) in TabWidget.EVENT.lines.withIndex()) {
                ChatUtils.consoleLog("[Mining Event Api] Tab Widget: $line")
            }


            for (eventType in MiningEventType.entries) {
                for ((index, line) in TabWidget.EVENT.lines.withIndex()) {
                    MiningEventType.widgetEventNotAnnouncedPattern.matchMatcher(line) {
                        clearActiveMiningEvent()
                        return
                    }
                    eventType.widgetEventPattern.matchMatcher(line) {
                        ChatUtils.debug("[Mining Event Api] Widget Event found event: ${eventType.name}")
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
            ChatUtils.consoleLog("[Mining Event Api] Bossbar: ${BossbarData.getBossbar()}")
            for (eventType in MiningEventType.entries) {
                eventType.bossBarPattern.matchMatcher(BossbarData.getBossbar()) {
                    val durationString = group("time")
                    val newDuration = TimeUtils.getDuration(durationString)

                    if (newDuration > Duration.ZERO) {
                        setActiveMiningEvent(eventType, newDuration)
                    }

                    return
                }
            }
            // if it gets through the matcher that means there is nothing there so set it to null
            // this might introduce a bug where you have the tablist widget for events disabled + get the hypixel bug for no boss bar would
            // result in this getting set to null. I will think of something better when im not brain-dead.

            // this does in fact introduce a bug if you have the boss bar not appearing glitch.

            // clearActiveMiningEvent()
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
