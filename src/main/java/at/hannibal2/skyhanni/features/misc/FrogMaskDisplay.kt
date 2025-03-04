package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.MiscConfig.FrogMaskCondition
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
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
import kotlin.time.Duration

@SkyHanniModule
object FrogMaskDisplay {
    private val config get() = SkyHanniMod.feature.misc
    private var display: Renderable? = null
    private val patternGroup = RepoPattern.group("misc.frogmask")

    private val frogMask by lazy { internalMaskName.getItemStack() }
    private val internalMaskName = "FROG_MASK".toInternalName()

    /**
     * REGEX-TEST: §7Today's region: §aDark Thicket
     */
    private val activeRegionPattern by patternGroup.pattern(
        "description.active",
        "§7Today's region: (?<region>.+)",
    )

    private var lastUpdate: SimpleTimeMark? = null
    private var activeRegion: String = ""

    private fun isEnabled(): Boolean = when (config.frogMaskDisplay) {
        FrogMaskCondition.DISABLED -> false
        FrogMaskCondition.INVENTORY -> InventoryUtils.isItemInInventory(internalMaskName) || InventoryUtils.getHelmet()
            ?.getInternalName() == internalMaskName

        FrogMaskCondition.PARK -> IslandType.THE_PARK.isInIsland()
        FrogMaskCondition.WORN -> InventoryUtils.getHelmet()?.getInternalName() == internalMaskName
        FrogMaskCondition.WORN_IN_PARK -> InventoryUtils.getHelmet()
            ?.getInternalName() == internalMaskName && IslandType.THE_PARK.isInIsland()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (config.frogMaskDisplay == FrogMaskCondition.DISABLED) return
        config.frogMaskDisplayPosition.renderRenderable(display, posLabel = "Frog Mask Display")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) {
            display = null
            return
        }

        val now = SkyBlockTime.now()
        if (lastUpdate == null || (lastUpdate?.timeUntil() ?: -Duration.INFINITE) < Duration.ZERO || activeRegion.isEmpty()) {
            lastUpdate = SkyBlockTime(year = now.year, month = now.month, day = now.day + 1).asTimeMark()
            val helmet = InventoryUtils.getHelmet()
            val mask = helmet?.takeIf { it.getInternalNameOrNull() == internalMaskName } ?: InventoryUtils.getItemsInOwnInventory()
                .find { it.getInternalName() == internalMaskName } ?: return

            val lore = mask.getLore()
            activeRegionPattern.firstMatcher(lore) {
                activeRegion = group("region")
            }
        }

        val timeRemaining = SkyBlockTime(year = now.year, month = now.month, day = now.day + 1).asTimeMark()
        val newDisplay = updateDisplay(timeRemaining)

        if (display != newDisplay) display = newDisplay
    }


    private fun updateDisplay(timeRemaining: SimpleTimeMark): Renderable {
        val until = timeRemaining.timeUntil()
        val timeString = until.format()

        return Renderable.horizontalContainer(
            listOf(
                Renderable.itemStack(frogMask),
                Renderable.string("§5Frog Mask§6 - $activeRegion §6for §b$timeString"),
            ),
            spacing = 1,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(75, "misc.frogmask.frogMaskDisplay") {
            ConfigUtils.migrateBooleanToEnum(it, FrogMaskCondition.DISABLED, FrogMaskCondition.WORN_IN_PARK)
        }
    }
}
