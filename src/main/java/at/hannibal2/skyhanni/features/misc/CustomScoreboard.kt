//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni, and snippets from item tracker features <3
//
// I'm also like really sorry for anyone who has to look at this code, it looks kinda bad
//

//
// TODO LIST
// V1 RELEASE
//  - Hide default scoreboard
//  - the things that arent done yet
//
// V2 RELEASE
//  - Soulflow API
//  - Bank API
//  - Custom Scoreboard Background
//  - quiver
//  - icons
//  - beacon power
//  - skyblock level
//  - commissions
//

package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.misc.customScoreboard
private var display = emptyList<List<Any>>()
private var partyCount = 0

// Stats / Numbers
private var purse = "0"
private var motes = "0"
private var bank = "0"
private var bits = "0"
private var copper = "0"
private var gems = "0"
private var location = "None"
private var lobbyCode = "None"
private var heat = "0"
private var mithrilPowder = "0"
private var gemstonePowder = "0"


enum class CustomScoreboardElements (
    // displayLine: The line that is displayed on the scoreboard
    val displayLine: Supplier<List<String>>?,

    // islands: The islands that this line is displayed on
    val islands: List<IslandType>,

    // visibilityOption: The option that is used to hide this line - use 0 to only display on the listed islands, 1 to hide on the listed islands
    val visibilityOption : Int,

    // index: The index of the line
    val index: Int
){
    SKYBLOCK(
        { listOf(config.customTitle.get().toString().replace("&", "§")) },
        listOf(),
        0,
        0
    ),
    PROFILE(
        { listOf(getProfileTypeAsSymbol() + HypixelData.profileName.firstLetterUppercase()) },
        listOf(),
        0,
        1
    ),
    PURSE(
        {
            when {
                config.hideEmptyLines && purse == "0" -> listOf("<hidden>")
                config.displayNumbersFirst -> listOf("§6$purse Purse")
                else -> listOf("Purse: §6$purse")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        2
    ),
    MOTES(
        {
            when {
                motes == "0" -> listOf("<hidden>")
                config.displayNumbersFirst -> listOf("§d$motes Motes")
                else -> listOf("Motes: §d$motes")
            }
        },
        listOf(IslandType.THE_RIFT),
        0,
        3
    ),
    BANK(
        {
            when {
                bank == "0" -> listOf("<hidden>")
                config.displayNumbersFirst -> listOf("§6$bank Bank")
                else -> listOf("Bank: §6$bank")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        4
    ),
    BITS(
        {
            when {
                bits == "0" -> listOf("<hidden>")
                config.displayNumbersFirst -> listOf("§b$bits Bits")
                else -> listOf("Bits: §b$bits")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        5
    ),
    COPPER(
        {
            when {
                copper == "0" -> listOf("<hidden>")
                config.displayNumbersFirst -> listOf("§c$copper Copper")
                else -> listOf("Copper: §c$copper")
            }
        },
        listOf(IslandType.GARDEN),
        0,
        6
    ),
    GEMS(
        {
            when {
                gems == "0" -> listOf("<hidden>")
                config.displayNumbersFirst -> listOf("§a$gems Gems")
                else -> listOf("Gems: §a$gems")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        7
    ),
    EMPTY_LINE(
        { listOf("<empty>") },
        listOf(),
        0,
        8
    ),
    LOCATION(
        { listOf(location) },
        listOf(),
        0,
        9
    ),
    SKYBLOCK_TIME(
        { listOf(SkyBlockTime.now().formatted(false)) },
        listOf(),
        0,
        10
    ),
    LOBBY_CODE(
        { listOf("§8$lobbyCode") },
        listOf(),
        0,
        11
    ),
    POWDER(
        {
            when (config.displayNumbersFirst){
                true -> listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone")
                false -> listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder")
            }
        },
        listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES),
        0,
        12
    ),
    EMPTY_LINE2(
        { listOf("<empty>") },
        listOf(),
        0,
        13
    ),
    SLAYER(
        { listOf("§7Slayer") },
        listOf(IslandType.HUB, IslandType.SPIDER_DEN, IslandType.THE_PARK, IslandType.THE_END, IslandType.CRIMSON_ISLE),
        0,
        14
    ),
    CURRENT_EVENT(
        { listOf("§cCurrent Event") },
        listOf(),
        0,
        15
    ),
    MAYOR(
        {
            listOf(
                MayorElection.currentCandidate?.name?.let { translateMayorNameToColor(it) } ?: "<hidden>"
            ) + (if (config.showMayorPerks) {
                MayorElection.currentCandidate?.perks?.map { " §7- §e${it.name}" } ?: emptyList()
            } else {
                emptyList()
            })
        },
        listOf(IslandType.THE_RIFT),
        1,
        16
    ),
    EMPTY_LINE3(
        { listOf("<empty>") },
        listOf(),
        0,
        17
    ),
    HEAT(
        {
            when {
                heat == "0" -> listOf("<hidden>")
                config.displayNumbersFirst -> listOf(if (heat == "0") "§c♨ 0 Heat" else "§c♨ $heat Heat")
                else -> listOf(if (heat == "0") "Heat: §c♨ 0" else "Heat: $heat")
            }
        },
        listOf(IslandType.CRYSTAL_HOLLOWS),
        0,
        18
    ),
    PARTY(
        {
            listOf(
                "§9Party",
                *PartyAPI.partyMembers
                    .takeWhile { partyCount < config.maxPartyList.get() }
                    .map {
                        partyCount++
                        " §7- §7$it"
                    }
                    .toTypedArray()
            )
        },
        listOf(IslandType.CATACOMBS, IslandType.DUNGEON_HUB, IslandType.KUUDRA_ARENA, IslandType.CRIMSON_ISLE),
        0,
        19
    ),
    MAXWELL(
        { listOf("§7Maxwell Power") },
        listOf(IslandType.THE_RIFT),
        1,
        20
    ),
    WEBSITE(
        { listOf(config.customFooter.get().toString().replace("&", "§")) },
        listOf(),
        0,
        21
    );

    fun getLine(): List<String> {
        return displayLine?.get() ?: emptyList()
    }
}

class CustomScoreboard {
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStringsAndItems(display, posLabel = "Custom Scoreboard")
    }

    @SubscribeEvent
    fun onTick(event: TickEvent) {
        // Draws the custom scoreboard
        display = drawScoreboard()

        // Resets Party count
        partyCount = 0

        // Gets some values for the tablist
        for (line in TabListData.getTabList()){
            if (line.startsWith(" Gems: §r§a")){
                gems = line.removePrefix(" Gems: §r§a")
            }
            if (line.startsWith(" Bank: §r§6")){
                bank = line.removePrefix(" Bank: §r§6")
            }
            if (line.startsWith(" §r§fMithril Powder: §r§2")){
                mithrilPowder = line.removePrefix(" §r§fMithril Powder: §r§2")
            }
            if (line.startsWith(" §r§fGemstone Powder: §r§d")){
                gemstonePowder = line.removePrefix(" §r§fGemstone Powder: §r§d")
            }
        }

        // Gets some values for the scoreboard
        for (line in ScoreboardData.sidebarLinesFormatted){
            if (line.startsWith(" §7⏣ ") || line.startsWith(" §5ф ")){
                location = line
            }
            if (line.startsWith("Motes: §d")){
                motes = line.removePrefix("Motes: §d")
            }
            if (extractLobbyCode(line) is String ){
                lobbyCode = extractLobbyCode(line)!!.substring(1) //removes first char (number of color code)
            }
            if (line.startsWith("Heat: ")){
                heat = line.removePrefix("Heat: ")
            }
            if (line.startsWith("Bits: §b")){
                bits = line.removePrefix("Bits: §b")
            }
            if (line.startsWith("Copper: §c")){
                copper = line.removePrefix("Copper: §c")
            }
        }
        purse = LorenzUtils.formatInteger(PurseAPI.currentPurse.toInt())
    }

    private fun drawScoreboard() = buildList<List<Any>> {
        val lineMap = HashMap<Int, List<Any>>()
        for (element in CustomScoreboardElements.entries) {
            lineMap[element.index] = element.getLine()
        }

        return formatDisplay(lineMap)
    }

    private fun formatDisplay(lineMap: HashMap<Int, List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            lineMap[index]?.let {

                // Multiline support
                if (it[0] == "§9Party"
                    || it[0].toString().contains(MayorElection.currentCandidate?.name ?: "")
                    || it[0].toString().contains("Powder")
                ) {
                    for (item in it) {
                        newList.add(listOf(item))
                    }
                    continue
                }

                // Adds empty lines
                if(it[0] == "<empty>"){
                    newList.add(listOf(""))
                    continue
                }

                // Does not display this line
                if(it[0] == "<hidden>"){
                    continue
                }

                newList.add(it)
            }
        }

        return newList
    }

    private fun isEnabled() : Boolean{
        return config.enabled && LorenzUtils.inSkyBlock
    }
}

private fun translateMayorNameToColor(input: String) : String {
    return when (input) {
        "Aatrox"    ->  "§3$input"
        "Cole"      ->  "§e$input"
        "Diana"     ->  "§2$input"
        "Diaz"      ->  "§6$input"
        "Finnegan"  ->  "§c$input"
        "Foxy"      ->  "§d$input"
        "Marina"    ->  "§b$input"
        "Paul"      ->  "§c$input"
        else        ->  "§7$input"
    }
}

private fun extractLobbyCode(input: String): String? {
    val regex = Regex("§(\\d{3}/\\d{2}/\\d{2}) §([A-Za-z0-9]+)$")
    val matchResult = regex.find(input)
    return matchResult?.groupValues?.lastOrNull()
}

private fun getProfileTypeAsSymbol(): String {
    return when {
        HypixelData.ironman -> "§7♲ "  // Ironman
        HypixelData.stranded -> "§a☀ " // Stranded
        HypixelData.bingo -> "§cⒷ "    // Bingo - TODO: Consider using colors from BingoAPI
        else -> "§e"                   // Default case
    }
}
