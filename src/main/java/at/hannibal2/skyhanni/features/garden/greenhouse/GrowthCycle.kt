package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.timerColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object GrowthCycle {

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val storage get() = ProfileStorageData.profileSpecific?.greenhouse

    val patternGroup = RepoPattern.group("garden.greenhouse.growthcycle")

    /**
     * REGEX-TEST: Crop Diagnostics
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Crop Diagnostics",
    )

    /**
     * REGEX-TEST: §7Next Stage: §a1h 40m 20s
     * REGEX-TEST: §7Next Stage: §a40m 20s
     * REGEX-TEST: §7Next Stage: §a20s
     */
    val nextStagePattern by patternGroup.pattern(
        "nextstage",
        "§7Next Stage: §a(?<time>.*)",
    )

    /**
     * REGEX-TEST: §a§lFULLY GROWN
     */
    val fullyGrownPattern by patternGroup.pattern(
        "fullygrown",
        "§a§lFULLY GROWN",
    )

    val cropDiagnosticInventory = InventoryDetector(inventoryPattern)

    private var display: Renderable? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!cropDiagnosticInventory.isInside()) return
        val item = event.inventoryItemsWithNull[20] ?: return
        val lore = item.getLore()

        nextStagePattern.firstMatcher(lore) {
            val timeString = group("time")
            if (fullyGrownPattern.matches(timeString)) return@firstMatcher
            val duration = TimeUtils.getDurationOrNull(timeString) ?: return
            storage?.nextCycle = duration.fromNow()
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
            return
        }
        display = drawDisplay(nextCycle)
    }

    private fun drawDisplay(nextCycle: SimpleTimeMark) = Renderable.vertical {
        val timeUntil = nextCycle.timeUntil()
        val color = timeUntil.timerColor("§a")
        val formatted = if (nextCycle.passedSince() > 10.minutes) "§cOVERDUE" else "$color${timeUntil.format(maxUnits = 2)}"
        addString("§6Next Growth Stage: $formatted")
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!config.showDisplay) return

        config.position.renderRenderable(display, posLabel = "Growth Cycle Timer")
    }
}
