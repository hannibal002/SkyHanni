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
//

package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
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

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        RenderBackground().renderBackground()

        if (!TabListData.fullyLoaded && config.displayConfig.cacheScoreboardOnIslandSwitch && cache.isNotEmpty()) {
            config.position.renderStringsAlignedWidth(cache, posLabel = "Custom Scoreboard")
        } else {
            config.position.renderStringsAlignedWidth(display, posLabel = "Custom Scoreboard")
            cache = display
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
        val lineMap = HashMap<Int, List<Pair<String, HorizontalAlignment>>>()
        for (element in ScoreboardElement.entries) {
            lineMap[element.ordinal] =
                if (element.isVisible()) element.getPair() else listOf("<hidden>" to HorizontalAlignment.LEFT)
        }

        return formatLines(lineMap)
    }

    private fun formatLines(lineMap: HashMap<Int, List<ScoreboardElementType>>): MutableList<ScoreboardElementType> {
        val newList = mutableListOf<ScoreboardElementType>()
        for (element in config.scoreboardEntries) {
            lineMap[element.ordinal]?.let {
                // Hide consecutive empty lines
                if (config.informationFilteringConfig.hideConsecutiveEmptyLines && it[0].first == "<empty>" && newList.last().first.isEmpty()) {
                    continue
                }

                // Adds empty lines
                if (it[0].first == "<empty>") {
                    newList.add("" to HorizontalAlignment.LEFT)
                    continue
                }

                // Does not display this line
                if (it.any { i -> i.first == "<hidden>" }) {
                    continue
                }

                // Multiline and singular line support
                newList.addAll(it.map { i -> i })
            }
        }

        return newList
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
