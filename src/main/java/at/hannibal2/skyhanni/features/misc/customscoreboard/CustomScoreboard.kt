//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni, and snippets from item tracker features <3
//

//
// TODO LIST
// V2 RELEASE
//  - icons maybe
//  - Soulflow API
//  - Bank API
//  - Custom Scoreboard Background
//  - quiver
//  - beacon power
//  - skyblock level
//

package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private val config get() = SkyHanniMod.feature.gui.customScoreboard
private var display = emptyList<String>()
private var cache = emptyList<List<Any>>()
var partyCount = 0

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


class CustomScoreboard {
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isCustomScoreboardEnabled()) return
        config.position.renderStrings(display, posLabel = "Custom Scoreboard")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isCustomScoreboardEnabled()) return

        // Draws the custom scoreboard
        display = drawScoreboard()

        // Resets Party count
        partyCount = 0

        // Gets some values from the tablist
        for (line in TabListData.getTabList()) {
            when {
                line.startsWith(" Gems: §r§a") -> gems = line.removePrefix(" Gems: §r§a")
                line.startsWith(" Bank: §r§6") -> bank = line.removePrefix(" Bank: §r§6")
                line.startsWith(" §r§fMithril Powder: §r§2") -> mithrilPowder =
                    line.removePrefix(" §r§fMithril Powder: §r§2")

                line.startsWith(" §r§fGemstone Powder: §r§d") -> gemstonePowder =
                    line.removePrefix(" §r§fGemstone Powder: §r§d")
            }
        }

        // Gets some values from the scoreboard
        for (line in ScoreboardData.sidebarLinesFormatted) {
            when {
                line.startsWith(" §7⏣ ") || line.startsWith(" §5ф ") -> location = line
                line.startsWith("Motes: §d") -> motes = line.removePrefix("Motes: §d")
                extractLobbyCode(line) is String -> lobbyCode =
                    extractLobbyCode(line)!!.substring(1) //removes first char (number of color code)
                line.startsWith("Heat: ") -> heat = line.removePrefix("Heat: ")
                line.startsWith("Bits: §b") -> bits = line.removePrefix("Bits: §b")
                line.startsWith("Copper: §c") -> copper = line.removePrefix("Copper: §c")
            }
        }
        purse = LorenzUtils.formatInteger(PurseAPI.currentPurse.toInt())
    }

    private fun drawScoreboard() = buildList<String> {
        val lineMap = HashMap<Int, List<Any>>()
        for (element in Elements.entries) {
            lineMap[element.index] = if (element.isVisible()) element.getLine() else listOf("<hidden>")
        }

        cache = lineMap.values.toList()

        return formatDisplay(lineMap)
    }

    private fun formatDisplay(lineMap: HashMap<Int, List<Any>>): MutableList<String> {
        val newList = mutableListOf<String>()
        for (index in config.textFormat) {
            lineMap[index]?.let {
                // Hide consecutive empty lines
                if (config.hideConsecutiveEmptyLines && it[0] == "<empty>" && newList.lastOrNull() == "") {
                    continue
                }

                // Adds empty lines
                if (it[0] == "<empty>") {
                    newList.add("")
                    continue
                }

                // Does not display this line
                if (it.any { i-> i == "<hidden>"}) {
                    continue
                }

                // Multiline support
                if (it.size > 1) {
                    for (item in it) {
                        newList.add(item.toString())
                    }
                    continue
                }

                newList.add(it[0].toString())
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

    private fun isHideVanillaScoreboardEnabled() = config.hideVanillaScoreboard && LorenzUtils.inSkyBlock

    companion object {
        fun copyScoreboard(args: Array<String>) {
            var string = ""

            for (index in config.textFormat) {
                cache[index].let {
                    for (line in it) {
                        string = string + line + "\n"
                    }
                }
            }

            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("§e[SkyHanni] Custom Scoreboard copied into your clipboard!")
        }
    }
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

private fun extractLobbyCode(input: String): String? {
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

fun getFooter(): String {
    val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay
    if (tabList.footer_skyhanni == null) return ""
    return tabList.footer_skyhanni.formattedText
}
