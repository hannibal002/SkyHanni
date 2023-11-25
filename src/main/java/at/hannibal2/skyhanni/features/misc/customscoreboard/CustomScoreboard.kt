//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni, and snippets from item tracker features <3
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
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getDummySize
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SpecialColour
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private val config get() = SkyHanniMod.feature.gui.customScoreboard
private var display = emptyList<String>()
private var cache = emptyList<List<Any>>()

// Stats / Numbers
var purse = "0"
var motes = "0"
var bank = "0"
var bits = "0"
var copper = "0"
var gems = "0"
var location = "None"
var lobbyCode = "None"
var heat = "0"
var mithrilPowder = "0"
var gemstonePowder = "0"
var partyCount = 0


class CustomScoreboard {
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isCustomScoreboardEnabled()) return
        if (display.isEmpty()) return
        val position = config.position
        val border = 5

        val x = position.getAbsX()
        val y = position.getAbsY()

        val elementWidth = position.getDummySize().x
        val elementHeight = position.getDummySize().y

        val scaledWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth
        val scaledHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight

        position.set(
            Position(
                when (config.displayConfig.alignRight) {
                    true -> scaledWidth - elementWidth - (border * 2)
                    false -> x
                },
                when (config.displayConfig.alignCenterVertically) {
                    true -> scaledHeight / 2 - elementHeight / 2
                    false -> y
                },
                position.getScale(),
                position.isCenter
            )
        )

        if (config.backgroundConfig.enabled) {
            GuiScreen.drawRect(
                x - border,
                y - border,
                x + elementWidth + border * 2,
                y + elementHeight + border * 2,
                SpecialColour.specialToChromaRGB(config.backgroundConfig.color)
            )
        }

        position.renderStrings(display, posLabel = "Custom Scoreboard")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isCustomScoreboardEnabled()) return

        // Draws the custom scoreboard
        display = createLines()

        // Get Information
        InformationGetter().getInformation()
    }

    private fun createLines() = buildList<String> {
        val lineMap = HashMap<Int, List<Any>>()
        for (element in Elements.entries) {
            lineMap[element.index] = if (element.isVisible()) element.getLine() else listOf("<hidden>")
        }

        cache = lineMap.values.toList()

        return formatLines(lineMap)
    }

    private fun formatLines(lineMap: HashMap<Int, List<Any>>): MutableList<String> {
        val newList = mutableListOf<String>()
        for (index in config.textFormat) {
            lineMap[index]?.let {
                // Hide consecutive empty lines
                if (config.informationFilteringConfig.hideConsecutiveEmptyLines && it[0] == "<empty>" && newList.lastOrNull() == "") {
                    continue
                }

                // Adds empty lines
                if (it[0] == "<empty>") {
                    newList.add("")
                    continue
                }

                // Does not display this line
                if (it.any { i -> i == "<hidden>" }) {
                    continue
                }

                // Multiline and singular line support
                newList.addAll(it.map { i -> i.toString() })
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

    private fun isCustomScoreboardEnabled() = config.enabled && LorenzUtils.inSkyBlock
    private fun isHideVanillaScoreboardEnabled() = config.displayConfig.hideVanillaScoreboard && LorenzUtils.inSkyBlock
}

fun translateMayorNameToColor(input: String): String {
    return when (input) {
        // Normal Mayors
        "Aatrox" -> "§3$input"
        "Cole" -> "§e$input"
        "Diana" -> "§2$input"
        "Diaz" -> "§6$input"
        "Finnegan" -> "§c$input"
        "Foxy" -> "§d$input"
        "Marina" -> "§b$input"
        "Paul" -> "§c$input"

        // Special Mayors
        "Scorpius" -> "§d$input"
        "Jerry" -> "§d$input"
        "Derpy" -> "§d$input"
        "Dante" -> "§d$input"
        else -> "§cUnknown Mayor: §7$input"
    }
}

fun extractLobbyCode(input: String): String? {
    val regex = Regex("§(\\d{3}/\\d{2}/\\d{2}) §([A-Za-z0-9]+)$")
    val matchResult = regex.find(input)
    return matchResult?.groupValues?.lastOrNull()
}

fun getProfileTypeAsSymbol(): String {
    return when {
        HypixelData.ironman -> "§7♲ " // Ironman
        HypixelData.stranded -> "§a☀ " // Stranded
        HypixelData.bingo -> ScoreboardData.sidebarLines.firstOrNull { it.contains("Bingo") }?.substring(
            0,
            3
        ) + "Ⓑ " // Bingo - gets the first 3 chars of " §9Ⓑ §9Bingo" (you are unable to get the Ⓑ for some reason)
        else -> "§e" // Default case
    }
}

fun getTablistFooter(): String {
    val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay
    if (tabList.footer_skyhanni == null) return ""
    return tabList.footer_skyhanni.formattedText
}
