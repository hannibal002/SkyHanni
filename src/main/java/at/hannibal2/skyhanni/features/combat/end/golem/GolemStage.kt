package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration

/**
 * Tracks how far the End Stone Protector has awoken and turns Hypixel's wording into a plain
 * stage number. Rendered by [GolemDisplay].
 */
@SkyHanniModule
object GolemStage {

    private val repoGroup = RepoPattern.group("combat.boss.protector.2.stage")

    /**
     * WRAPPED-REGEX-TEST: " Protector: Resting"
     * WRAPPED-REGEX-TEST: " Protector: Disturbed"
     */
    private val stagePattern by repoGroup.pattern(
        "tablist",
        "\\s*Protector: (?<stage>.*)",
    )

    /**
     * Hypixel's wording in awakening order, with how it is shown and how urgent it looks.
     * The colour ramps from white through green and yellow to red as the protector wakes up.
     * Plain RGB rather than the 16 legacy codes, because those have no real orange.
     */
    enum class Stage(private val hypixelName: String, val display: String, val rgb: Int) {
        RESTING("Resting", "0", 0xFFFFFF),
        DORMANT("Dormant", "1", 0x55E066),
        AGITATED("Agitated", "2", 0x1E8A2E),
        DISTURBED("Disturbed", "3", 0xF2D024),
        AWAKENING("Awakening", "4", 0xD1650A),
        SUMMONED("Summoned", "Summoning", 0xE02020),
        ;

        companion object {
            fun byHypixelName(name: String) = entries.firstOrNull { it.hypixelName == name }
        }
    }

    /** Current stage, or null while unknown. */
    var current: Stage? = null
        private set

    /** Raw stage text as sent by Hypixel, kept for wordings we do not know yet. */
    var stageName: String? = null
        private set

    /** The protector has fully awoken and is about to rise. */
    val isFullyAwoken get() = current == Stage.SUMMONED

    /**
     * When the awakening stage was first seen. Starts on joining an already awakening protector
     * too, in which case the elapsed time is measured from that moment rather than from the
     * stage actually beginning - the server does not tell us when that was.
     */
    private var awakeningSince: SimpleTimeMark? = null

    /** How long the protector has been awakening, or null outside that stage. */
    fun timeInAwakening(): Duration? = awakeningSince?.passedSince()

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onTabList(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PROTECTOR)) return
        if (event.isClear()) {
            reset()
            return
        }

        val line = event.cleanLines.firstOrNull() ?: return
        stagePattern.matchMatcher(line) {
            val name = group("stage").trim()
            stageName = name
            val newStage = Stage.byHypixelName(name)
            if (newStage != current) {
                current = newStage
                // Only the awakening stage is timed; every other stage clears the counter.
                awakeningSince = if (newStage == Stage.AWAKENING) SimpleTimeMark.now() else null
            }
        }
    }

    private fun reset() {
        current = null
        stageName = null
        awakeningSince = null
    }

    @HandleEvent
    private fun onIslandChange(event: IslandChangeEvent) {
        reset()
    }
}
