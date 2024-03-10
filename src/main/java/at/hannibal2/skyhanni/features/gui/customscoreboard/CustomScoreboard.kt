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
//  - shorten time till next mayor https://discord.com/channels/997079228510117908/1162844830360146080/1216440046320746596
//  - option to hide coins earned
//  - color options in the purse etc lines
//

package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiPositionMovedEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAlignedWidth
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

typealias ScoreboardElementType = Pair<String, HorizontalAlignment>

class CustomScoreboard {
    private val config get() = SkyHanniMod.feature.gui.customScoreboard
    private var display = emptyList<ScoreboardElementType>()
    private var cache = emptyList<ScoreboardElementType>()
    private val guiName = "Custom Scoreboard"

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        RenderBackground().renderBackground()

        if (!TabListData.fullyLoaded && config.displayConfig.cacheScoreboardOnIslandSwitch && cache.isNotEmpty()) {
            config.position.renderStringsAlignedWidth(cache, posLabel = guiName)
        } else {
            config.position.renderStringsAlignedWidth(display, posLabel = guiName)

            if (cache != display) cache = display
        }
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
            display = createLines()
        }

        // Remove Known Lines, so we can get the unknown ones
        UnknownLinesHandler.handleUnknownLines()
    }

    private fun createLines() = buildList<ScoreboardElementType> {
        val lineMap = ScoreboardElement.entries.associate {
            it.ordinal to
                if (it.isVisible()) it.getPair() else listOf("<hidden>" to HorizontalAlignment.LEFT)
        }

        return formatLines(lineMap)
    }

    private fun formatLines(lineMap: Map<Int, List<ScoreboardElementType>>): List<ScoreboardElementType> {
        return buildList {
            for (element in config.scoreboardEntries) {
                val line = lineMap[element.ordinal] ?: continue

                // Hide consecutive empty lines
                if (
                    config.informationFilteringConfig.hideConsecutiveEmptyLines &&
                    line.isNotEmpty() && line[0].first == "<empty>" && lastOrNull()?.first?.isEmpty() == true
                ) {
                    continue
                }

                // Adds empty lines
                if (line[0].first == "<empty>") {
                    add("" to HorizontalAlignment.LEFT)
                    continue
                }

                // Does not display this line
                if (line.any { it.first == "<hidden>" }) {
                    continue
                }

                // Multiline and singular line support
                addAll(line)
            }
        }
    }

    // Thank you Apec for showing that the ElementType of the stupid scoreboard is FUCKING HELMET WTF
    @SubscribeEvent
    fun onRenderScoreboard(event: RenderGameOverlayEvent.Post) {
        if (event.type == RenderGameOverlayEvent.ElementType.HELMET) {
            GuiIngameForge.renderObjective = !isHideVanillaScoreboardEnabled()
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    private fun isHideVanillaScoreboardEnabled() = isEnabled() && config.displayConfig.hideVanillaScoreboard
}
