package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
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

    private val frogMask by lazy { "FROG_MASK".toInternalName().getItemStack() }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.frogMaskDisplayPosition.renderRenderable(display, posLabel = "Frog Mask Display")
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        display = null

        val helmet = InventoryUtils.getHelmet() ?: return
        if (helmet.getInternalName() != "FROG_MASK".toInternalName()) return

        activeRegionPattern.firstMatcher(helmet.getLore()) {
            val currentRegion = group("region")

            when {
                config.frogMaskDisplay -> {
                    val now = SkyBlockTime.now()
                    val timeRemaining = SkyBlockTime(year = now.year, month = now.month, day = now.day + 1).asTimeMark()
                    display = updateDisplay(currentRegion, timeRemaining)
                }

                config.frogMaskWarning -> {
                    if (LorenzUtils.skyBlockArea == currentRegion) {
                        lastWarning = SimpleTimeMark.farPast()
                    } else if (lastWarning.passedSince() > 30.seconds) {
                        LorenzUtils.sendTitle("§cWrong Region!", 3.seconds)
                        lastWarning = SimpleTimeMark.now()
                    }
                }
            }
        }
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

    private fun isEnabled() = IslandType.THE_PARK.isInIsland() && (config.frogMaskDisplay || config.frogMaskWarning)
}
