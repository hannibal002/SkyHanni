package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.frogmask.FrogMaskWarningConfig.WarningType
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.BlockCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FrogMaskFeatures {
    private val config get() = SkyHanniMod.feature.misc.frogMaskFeatures

    private var display: Renderable? = null

    private var lastWarning = SimpleTimeMark.farPast()
    private var lastLogClick = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("misc.frogmask")

    /**
     * REGEX-TEST: §7Today's region: §aDark Thicket
     */
    private val activeRegionPattern by patternGroup.pattern(
        "description.active",
        "§7Today's region: (?<region>.+)",
    )

    private val FROG_MASK = "FROG_MASK".toInternalName()
    private val frogMaskRenderable by lazy { Renderable.itemStack(FROG_MASK.getItemStack()) }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.THE_PARK)
    fun onRenderOverlay() {
        if (!isEnabled()) return

        config.position.renderRenderable(display, posLabel = "Frog Mask Display")
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.THE_PARK)
    fun onSecondPassed() {
        if (!isEnabled()) return
        display = null

        val helmet = InventoryUtils.getHelmet() ?: return
        if (helmet.getInternalName() != FROG_MASK) return

        activeRegionPattern.firstMatcher(helmet.getLore()) {
            val helmetRegion = group("region")

            if (config.warning.enabled) handleWarning(helmetRegion)
            if (config.display) handleDisplay(helmetRegion)
        }
    }

    private fun handleWarning(helmetRegion: String) {
        val inWrongArea = IslandAreas.currentAreaName != helmetRegion.removeColor()
        val timeToWarn = lastWarning.passedSince() > config.warning.cooldown.seconds

        if (!inWrongArea || !timeToWarn) return

        when (config.warning.warningType) {
            WarningType.BEING -> {
                remindWrongRegion(helmetRegion)
                lastWarning = SimpleTimeMark.now()
            }

            WarningType.FORAGING -> {
                if (isForaging()) {
                    remindWrongRegion(helmetRegion)
                    lastWarning = SimpleTimeMark.now()
                }
            }

            WarningType.NEVER -> return
        }
    }

    private fun remindWrongRegion(helmetRegion: String) {
        TitleManager.sendTitle("§cWrong Region!")
        ChatUtils.chat("Your Frog Mask currently boosts $helmetRegion§e!")
    }

    private fun handleDisplay(helmetRegion: String) {
        val now = SkyBlockTime.now()
        val nextDay = SkyBlockTime(year = now.year, month = now.month, day = now.day + 1).toTimeMark()
        updateDisplay(helmetRegion, nextDay)
    }

    private fun updateDisplay(helmetRegion: String, nextDay: SimpleTimeMark) {
        val timeRemaining = nextDay.timeUntil()

        display = Renderable.horizontalContainer(
            listOf(
                frogMaskRenderable,
                Renderable.string("§5Frog Mask§6 - $helmetRegion §6for §b${timeRemaining.format()}"),
            ),
            spacing = 1,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(86, "misc.frogMaskDisplay", "misc.frogMaskFeatures.display")
        event.move(86, "misc.frogMaskDisplayPosition", "misc.frogMaskFeatures.position")
    }

    private val logTypes = BlockCompat.getAllLogs()

    @HandleEvent(onlyOnIsland = IslandType.THE_PARK)
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (event.position.getBlockAt() !in logTypes) return

        lastLogClick = SimpleTimeMark.now()
    }

    private fun isForaging() = lastLogClick.passedSince() < 5.seconds

    private fun isEnabled() = config.display || config.warning.enabled
}
