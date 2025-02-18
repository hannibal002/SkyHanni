package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.frogmask.FrogMaskWarningConfig.WarningType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillProgress.updateSkillInfo
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FrogMaskFeatures {
    private val config get() = SkyHanniMod.feature.misc.frogMaskFeatures

    private var display: Renderable? = null

    private var lastWarning = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("misc.frogmask")

    /**
     * REGEX-TEST: §7Today's region: §aDark Thicket
     */
    private val activeRegionPattern by patternGroup.pattern(
        "description.active",
        "§7Today's region: (?<region>.+)",
    )

    /**
     * REGEX-TEST:  §7⏣ §aSpruce Woods
     */
    private val currentAreaPattern by patternGroup.pattern(
        "scoreboard.current",
        " §7⏣ (?<area>.+)"
    )

    private val frogMask by lazy { "FROG_MASK".toInternalName().getItemStack() }

    @HandleEvent(onlyOnIsland = IslandType.THE_PARK)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.displayPosition.renderRenderable(display, posLabel = "Frog Mask Display")
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_PARK)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        display = null

        val helmet = InventoryUtils.getHelmet() ?: return
        if (helmet.getInternalName() != "FROG_MASK".toInternalName()) return

        activeRegionPattern.firstMatcher(helmet.getLore()) {
            val currentRegion = group("region")

            if (config.warning.enabled) lastWarning = handleWarning(currentRegion)
            if (config.display) display = handleDisplay(currentRegion)
        }
    }

    private fun handleWarning(currentRegion: String): SimpleTimeMark {
        if (config.warning.warningType == WarningType.NEVER) return lastWarning

        currentAreaPattern.firstMatcher(ScoreboardData.sidebarLinesFormatted) {
            val needsToWarn =
                group("area") != currentRegion && lastWarning.passedSince() > config.warning.cooldown.seconds

            if (!needsToWarn) return lastWarning

            when (config.warning.warningType) {
                WarningType.BEING -> LorenzUtils.sendTitle("§cWrong Region!", 3.seconds)
                WarningType.FORAGING -> if (isForaging()) LorenzUtils.sendTitle("§cWrong Region!", 3.seconds)
                else -> return lastWarning
            }

            return SimpleTimeMark.now()
        }

        return lastWarning
    }

    private fun handleDisplay(currentRegion: String): Renderable {
        val now = SkyBlockTime.now()
        val timeRemaining = SkyBlockTime(year = now.year, month = now.month, day = now.day + 1).asTimeMark()
        return updateDisplay(currentRegion, timeRemaining)
    }

    private fun updateDisplay(currentRegion: String, timeRemaining: SimpleTimeMark): Renderable {
        val until = timeRemaining.timeUntil()
        val timeString = until.format()

        return Renderable.horizontalContainer(
            listOf(
                Renderable.itemStack(frogMask),
                Renderable.string(
                    "§5Frog Mask§6 - $currentRegion §6for §b$timeString",
                ),
            ),
            spacing = 1,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(74, "misc.frogMaskDisplay", "misc.frogMaskFeatures.frogMaskDisplay")
        event.move(74, "misc.frogMaskDisplayPosition", "misc.frogMaskFeatures.frogMaskDisplayPosition")
    }

    private fun isForaging(): Boolean {
        if (SkillApi.activeSkill != SkillType.FORAGING) return false
        updateSkillInfo()
        val info = SkillApi.skillXPInfoMap[SkillType.FORAGING] ?: return false
        return info.lastUpdate.passedSince() < 10.seconds
    }

    private fun isEnabled() = config.display || config.warning.enabled
}
