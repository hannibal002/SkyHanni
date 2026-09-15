package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tracks how far the End Stone Protector has awoken and turns Hypixel's wording into a plain
 * stage number. Rendered by [GolemDisplay].
 */
@SkyHanniModule
object GolemStage {

    private val patternGroup = RepoPattern.group("combat.boss.protector.2.stage")

    /**
     * WRAPPED-REGEX-TEST: " Protector: Resting"
     * WRAPPED-REGEX-TEST: " Protector: Disturbed"
     */
    private val stagePattern by patternGroup.pattern(
        "tablist",
        "\\s*Protector: (?<stage>.*)",
    )

    /**
     * Hypixel's wording in awakening order, with how it is shown and how urgent it looks.
     * The colour ramps from white through green and yellow to red as the protector wakes up.
     * Plain RGB rather than the 16 legacy codes, because those have no real orange.
     */
    enum class Stage(
        private val hypixelName: String,
        val display: String,
        val rgb: Int,
        /** Nearest legacy code, for titles - those are plain strings and cannot carry RGB. */
        val legacyColor: String,
    ) {
        RESTING("Resting", "0", 0xFFFFFF, "§f"),
        DORMANT("Dormant", "1", 0x55E066, "§a"),
        AGITATED("Agitated", "2", 0x1E8A2E, "§2"),
        DISTURBED("Disturbed", "3", 0xF2D024, "§e"),
        AWAKENING("Awakening", "4", 0xD1650A, "§6"),
        SUMMONED("Summoned", "Summoning", 0xE02020, "§c"),
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

    /**
     * Whether the stage seen next is the first one after joining. Only then is the title worth
     * showing - during normal play you watched the protector get there.
     */
    private var firstStageAfterJoin = true

    /** Stages far enough along that arriving at one is worth interrupting the screen for. */
    private val LATE_STAGES = setOf(Stage.AWAKENING, Stage.SUMMONED)

    private val TITLE_DURATION = 5.seconds

    /** How long the protector has been awakening, or null outside that stage. */
    fun timeInAwakening(): Duration? = awakeningSince?.passedSince()

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
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
            if (firstStageAfterJoin && newStage != null) {
                firstStageAfterJoin = false
                announceLateStage(newStage)
            }
        }
    }

    /** Joining a server that already has a protector well on its way is easy to walk past. */
    private fun announceLateStage(stage: Stage) {
        val enabled = SkyHanniMod.feature.combat.endIsland.golem.joinStageTitle
        ChatUtils.consoleLog(
            "[GolemStage] first stage after join: $stage, enabled=$enabled, late=${stage in LATE_STAGES}",
        )
        if (!enabled) return
        if (stage !in LATE_STAGES) return
        // Part of the alert rather than a switch of its own: if the title is wanted, so is the
        // sound that makes you look at it.
        SoundUtils.createSound("entity.player.levelup", 1f, isWarning = false).playSound()
        TitleManager.sendTitle("${stage.legacyColor}§lSTAGE ${stage.display}", duration = TITLE_DURATION)
    }

    private fun reset() {
        current = null
        stageName = null
        awakeningSince = null
    }

    /**
     * Every server hop starts over, which covers joining the island as well: the next stage seen
     * is then the first one after arriving, and worth a title.
     */
    @HandleEvent
    private fun onWorldChange() {
        firstStageAfterJoin = true
        reset()
    }
}
