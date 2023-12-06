//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni and more contribs <3
// Also big thanks to the people that tested it before it released, saved me some time <3
//

//
// TODO LIST
// V2 RELEASE
//  - Soulflow API
//  - Bank API
//  - quiver
//  - beacon power
//  - skyblock level
//  - more bg options (round, blurr, outline)
//

package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAlignedWidth
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private val config get() = SkyHanniMod.feature.gui.customScoreboard
private var display = emptyList<Pair<String, AlignmentEnum>>()

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

        // Draws the custom scoreboard
        display = createLines()

        // Get Information
        InformationGetter().getInformation()
    }

    private fun createLines() = buildList<Pair<String, AlignmentEnum>> {
        val lineMap = HashMap<Int, List<Pair<String, AlignmentEnum>>>()
        for (element in Elements.entries) {
            lineMap[element.ordinal] =
                if (element.isVisible()) element.getPair() else listOf("<hidden>" to AlignmentEnum.LEFT)
        }

        return formatLines(lineMap)
    }

    private fun formatLines(lineMap: HashMap<Int, List<Pair<String, AlignmentEnum>>>): MutableList<Pair<String, AlignmentEnum>> {
        val newList = mutableListOf<Pair<String, AlignmentEnum>>()
        for (index in config.scoreboardEntries) {
            lineMap[index.ordinal]?.let {
                // Hide consecutive empty lines
                if (config.informationFilteringConfig.hideConsecutiveEmptyLines && it[0].first == "<empty>" && newList.last().first == "") {
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

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        // Reset Bits - We need this bc if another profile has 0 bits, it won't show the bits line
        if (event.message.startsWith("§aYour profile was changed to:")) {
            bits = "0"
        }
    }

    private fun isCustomScoreboardEnabled() = config.enabled && LorenzUtils.inSkyBlock
    private fun isHideVanillaScoreboardEnabled() = config.displayConfig.hideVanillaScoreboard && LorenzUtils.inSkyBlock
}
