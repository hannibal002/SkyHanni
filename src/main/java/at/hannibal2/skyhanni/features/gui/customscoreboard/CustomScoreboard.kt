//
// TODO LIST
// V2 RELEASE
//  - Soulflow API
//  - Bank API (actually maybe not, I like the current design)
//  - beacon power
//  - skyblock level
//  - more bg options (round, blurr, outline)
//  - countdown events like fishing festival + fiesta when its not on tablist
//  - CookieAPI https://discord.com/channels/997079228510117908/1162844830360146080/1195695210433351821
//  - Rng meter display
//  - option to hide coins earned
//  - color options in the purse etc lines
//  - choose the amount of decimal places in shorten nums
//  - more anchor points (alignment enums in renderutils)
//

package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiPositionMovedEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAlignedWidth
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

typealias ScoreboardElementType = Pair<String, HorizontalAlignment>

class CustomScoreboard {

    private var display = emptyList<ScoreboardElementType>()
    private var cache = emptyList<ScoreboardElementType>()
    private val guiName = "Custom Scoreboard"

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        RenderBackground().renderBackground()

        val render =
            if (!TabListData.fullyLoaded && config.displayConfig.cacheScoreboardOnIslandSwitch && cache.isNotEmpty()) {
                cache
            } else {
                display
            }
        config.position.renderStringsAlignedWidth(
            render,
            posLabel = guiName,
            extraSpace = displayConfig.lineSpacing - 10
        )
    }

    @SubscribeEvent
    fun onGuiPositionMoved(event: GuiPositionMovedEvent) {
        if (event.guiName == guiName) {
            val alignmentConfig = config.displayConfig.alignment
            if (alignmentConfig.alignRight || alignmentConfig.alignCenterVertically) {
                alignmentConfig.alignRight = false
                alignmentConfig.alignCenterVertically = false
                ChatUtils.chat("Disabled Custom Scoreboard auto-alignment.")
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        // Creating the lines
        if (event.isMod(5)) {
            display = createLines().removeEmptyLinesFromEdges()
            if (TabListData.fullyLoaded) {
                cache = display.toList()
            }
        }

        // Remove Known Lines, so we can get the unknown ones
        UnknownLinesHandler.handleUnknownLines()
    }

    companion object {
        internal val config get() = SkyHanniMod.feature.gui.customScoreboard
        internal val displayConfig get() = config.displayConfig
        internal val eventsConfig get() = displayConfig.eventsConfig
        internal val informationFilteringConfig get() = config.informationFilteringConfig
        internal val backgroundConfig get() = config.backgroundConfig
    }

    private fun createLines() = buildList<ScoreboardElementType> {
        for (element in config.scoreboardEntries) {
            val lines = element.getVisiblePair()
            if (lines.isEmpty()) continue

            // Hide consecutive empty lines
            if (
                informationFilteringConfig.hideConsecutiveEmptyLines &&
                lines.first().first == "<empty>" && lastOrNull()?.first?.isEmpty() == true
            ) {
                continue
            }

            // Adds empty lines
            if (lines.first().first == "<empty>") {
                add("" to HorizontalAlignment.LEFT)
                continue
            }

            // Does not display this line
            if (lines.any { it.first == "<hidden>" }) {
                continue
            }

            addAll(lines)
        }
    }

    private fun List<ScoreboardElementType>.removeEmptyLinesFromEdges(): List<ScoreboardElementType> {
        if (config.informationFilteringConfig.hideEmptyLinesAtTopAndBottom) {
            return this
                .dropWhile { it.first.isEmpty() }
                .dropLastWhile { it.first.isEmpty() }
        }
        return this
    }

    // Thank you Apec for showing that the ElementType of the stupid scoreboard is FUCKING HELMET WTF
    @SubscribeEvent
    fun onRenderScoreboard(event: RenderGameOverlayEvent.Post) {
        if (event.type == RenderGameOverlayEvent.ElementType.HELMET) {
            GuiIngameForge.renderObjective = !isHideVanillaScoreboardEnabled()
        }
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Custom Scoreboard")
        event.addIrrelevant {
            if (!config.enabled) {
                add("Custom Scoreboard disabled.")
            } else {
                ScoreboardElement.entries.map { element ->
                    add(
                        "${element.name.firstLetterUppercase()} - " +
                            "${element.showWhen.invoke()} - " +
                            "${element.getVisiblePair().map { it.first }}"
                    )
                }
            }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    private fun isHideVanillaScoreboardEnabled() = isEnabled() && config.displayConfig.hideVanillaScoreboard

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(28, "gui.customscoreboard.displayConfig.showAllActiveEvents", "gui.customscoreboard.displayConfig.eventsConfig.showAllActiveEvents")
    }
}
