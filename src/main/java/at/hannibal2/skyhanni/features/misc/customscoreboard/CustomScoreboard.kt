//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni and more contribs <3
// Also big thanks to the people that tested it before it released, saved me some time <3
//

//
// TODO LIST
// V2 RELEASE
//  - Soulflow API
//  - Bank API (actually maybe not, I like the current design)
//  - beacon power
//  - skyblock level
//  - more bg options (round, blurr, outline)
//  - countdown events like fishing festival + fiesta when its not on tablist
//

package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAlignedWidth
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

typealias ScoreboardElement = Pair<String, AlignmentEnum>

private val config get() = SkyHanniMod.feature.gui.customScoreboard
private var display = emptyList<ScoreboardElement>()

class CustomScoreboard {
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isCustomScoreboardEnabled()) return
        if (display.isEmpty()) return
        val position = config.position

        RenderBackground().renderBackground()

        position.renderStringsAlignedWidth(display, posLabel = "Custom Scoreboard")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isCustomScoreboardEnabled()) return

        // Creates the lines
        display = createLines()

        // Get Information
        InformationGetter.getInformation()
    }

    private fun createLines() = buildList<ScoreboardElement> {
        val lineMap = HashMap<Int, List<Pair<String, AlignmentEnum>>>()
        for (element in ScoreboardElements.entries) {
            lineMap[element.ordinal] =
                if (element.isVisible()) element.getPair() else listOf("<hidden>" to AlignmentEnum.LEFT)
        }

        return formatLines(lineMap)
    }

    private fun formatLines(lineMap: HashMap<Int, List<ScoreboardElement>>): MutableList<ScoreboardElement> {
        val newList = mutableListOf<ScoreboardElement>()
        for (element in config.scoreboardEntries) {
            lineMap[element.ordinal]?.let {
                // Hide consecutive empty lines
                if (config.informationFilteringConfig.hideConsecutiveEmptyLines && it[0].first == "<empty>" && newList.last().first.isEmpty()) {
                    continue
                }

                // Adds empty lines
                if (it[0].first == "<empty>") {
                    newList.add("" to AlignmentEnum.LEFT)
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

    private fun isCustomScoreboardEnabled() = LorenzUtils.inSkyBlock && config.enabled
    private fun isHideVanillaScoreboardEnabled() =
        LorenzUtils.inSkyBlock && config.enabled && config.displayConfig.hideVanillaScoreboard
}
