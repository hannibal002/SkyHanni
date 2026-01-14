package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.timerColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object GrowthCycle {

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val storage get() = ProfileStorageData.profileSpecific?.garden?.greenhouse

    val patternGroup = RepoPattern.group("garden.greenhouse.growthcycle")

    /**
     * REGEX-TEST: Crop Diagnostics
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Crop Diagnostics",
    )

    /**
     * REGEX-TEST: Next Stage: 1h 40m 20s
     * REGEX-TEST: Next Stage: 40m 20s
     * REGEX-TEST: Next Stage: 20m 1s
     * REGEX-TEST: Next Stage: 20s
     */
    val nextStagePattern by patternGroup.pattern(
        "nextstage",
        "Next Stage: (?<time>(?:\\d\\d?[hms] ?)+)",
    )

    private val cropDiagnosticInventory = InventoryDetector(inventoryPattern)

    private var display: Renderable? = null
    private var beep = true

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!cropDiagnosticInventory.isInside()) return
        val item = event.inventoryItemsWithNull[20] ?: return
        val lore = item.getLoreComponent()

        nextStagePattern.firstMatcher(lore.map { it.string }) {
            val timeString = group("time")
            val duration = TimeUtils.getDurationOrNull(timeString) ?: return
            storage?.nextCycle = duration.fromNow()
            beep = false
            updateDisplay()
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.showDisplay) return

        updateDisplay()
    }

    private fun updateDisplay() {
        val nextCycle = storage?.nextCycle ?: return
        if (nextCycle.isFarPast() || nextCycle.passedSince() > 60.minutes) {
            display = null
            beep = false
            return
        }
        display = drawDisplay(nextCycle)
    }

    private fun drawDisplay(nextCycle: SimpleTimeMark): Renderable? {
        val timeUntil = nextCycle.timeUntil()
        val color = timeUntil.timerColor("§a")
        val formatted = if (nextCycle.isInFuture()) {
            if (!beep) {
                SoundUtils.playPlingSound()
                ChatUtils.chat("§aGreenhouse Growth Stage is ready in the Garden")
                beep = true
            }
            "§cOVERDUE"
        } else {
            if (config.onlyShowWhenOverdue) return null
            "$color${timeUntil.format(maxUnits = 2)}"
        }
        return Renderable.text("§6Next Greenhouse Growth Stage: $formatted")
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!config.showDisplay) return

        config.position.renderRenderable(display, posLabel = "Growth Cycle Timer")
    }
}
