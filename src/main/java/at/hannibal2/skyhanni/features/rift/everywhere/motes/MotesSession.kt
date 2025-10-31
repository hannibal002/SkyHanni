package at.hannibal2.hanni.features.rift.everywhere.motes

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.WidgetUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.inPartialHours
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object MotesSession {

    private val config get() = HanniMod.feature.rift.motes.motesPerSession

    private var initialMotes: Long? = null
    private var currentMotes: Long? = null
    private var enterRiftTime = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("rift.everywhere.motes")

    /**
     * REGEX-TEST:  Lifetime Motes: §r§d593,922
     */
    private val lifetimeMotesPattern by patternGroup.pattern(
        "lifetime",
        "\\s+Lifetime Motes: §r§d(?<motes>[\\d,.]+)",
    )

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.RIFT_INFO)) return
        lifetimeMotesPattern.firstMatcher(event.widget.lines) {
            val amount = group("motes").formatLong()
            if (initialMotes == null) {
                initialMotes = amount
                enterRiftTime = SimpleTimeMark.now()
            }
            // TODO move into RiftAPI, rename to lifetimeMotes, reuse in custom scoreboard maybe?
            currentMotes = amount
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.oldIsland == IslandType.THE_RIFT) {
            sendMotesInfo()
            initialMotes = null
            currentMotes = null
        }
    }

    private fun sendMotesInfo() {
        if (!config) return
        val initial = initialMotes ?: return
        val current = currentMotes ?: return
        val gained = current - initial
        if (gained < 1) return
        val timeInRift = enterRiftTime.passedSince()
        val motesPerHour = (gained / timeInRift.inPartialHours).toLong()
        val hover = buildList {
            add("§7Gained: §d${gained.addSeparators()} motes")
            add("§7Time spent: §d${timeInRift.format()}")
            add("§7Motes/h: §d${motesPerHour.addSeparators()}")
        }
        ChatUtils.hoverableChat(
            "Gained §d${gained.addSeparators()} motes §ethis Rift session! (§d${motesPerHour.shortFormat()}/h§e)",
            hover,
        )
    }
}
