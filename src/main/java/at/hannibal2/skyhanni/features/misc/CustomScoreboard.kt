//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni, and snippets from item tracker features <3
//
// I'm also like really sorry for anyone who has to look at this code, it looks kinda bad
//

//
// TODO LIST
// V1 RELEASE
//  - enums prob (why)
//  - toggle between "<name> <count>" and "<count> <name>"
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
    val displayLine: List<String>,

    // alternativeLine: The line that is displayed on the scoreboard when "displayNumbersFirst" is enabled
    val alternativeLine: List<String>,

    // islands: The islands that this line is displayed on
    val islands: List<IslandType>,

    // visibilityOption: The option that is used to hide this line - use 0 to only display on the listed islands, 1 to hide on the listed islands
    val visibilityOption : Int,

    // index: The index of the line
    val index: Int,

    // data: The data that is used for this line
    val data: String = ""
){
    SKYBLOCK(
        listOf("§6§lSKYBLOCK"),
        listOf(),
        listOf(),
        0,
        0
    ),
    PROFILE(
        listOf(getProfileTypeAsSymbol() + HypixelData.profileName.firstLetterUppercase()),
        listOf(),
        listOf(),
        0,
        1
    ),
    PURSE(
        listOf("Purse: §6$purse"),
        listOf("§6$purse Purse"),
        listOf(IslandType.THE_RIFT),
        1,
        2,
        purse
    ),
    MOTES(
        listOf("Motes: §d$motes"),
        listOf("§d$motes Motes"),
        listOf(IslandType.THE_RIFT),
        0,
        3,
        motes
    ),
    BANK(
        listOf("Bank: §6$bank"),
        listOf("§6$bank Bank"),
        listOf(IslandType.THE_RIFT),
        1,
        4,
        bank
    ),
    BITS(
        listOf("Bits: §b$bits"),
        listOf("§b$bits Bits"),
        listOf(IslandType.THE_RIFT),
        1,
        5,
        bits
    ),
    COPPER(
        listOf("Copper: §c$copper"),
        listOf("§c$copper Copper"),
        listOf(IslandType.GARDEN),
        0,
        6,
        copper
    ),
    GEMS(
        listOf("Gems: §a$gems"),
        listOf("§a$gems Gems"),
        listOf(IslandType.THE_RIFT),
        1,
        7,
        gems
    ),
    EMPTY_LINE(
        listOf("<empty>"),
        listOf(),
        listOf(),
        0,
        8
    ),
    LOCATION(
        listOf(location),
        listOf(),
        listOf(),
        0,
        9
    ),
    SKYBLOCK_TIME(
        listOf(SkyBlockTime.now().formatted(false)),
        listOf(),
        listOf(),
        0,
        10
    ),
    LOBBY_CODE(
        listOf("§8$lobbyCode"),
        listOf(),
        listOf(),
        0,
        11
    ),
    POWDER(
        listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder"),
        listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone"),
        listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES),
        0,
        12
    ),
    EMPTY_LINE2(
        listOf("<empty>"),
        listOf(),
        listOf(),
        0,
        13
    ),
    SLAYER(
        listOf("§7Slayer"),
        listOf(""),
        listOf(IslandType.HUB, IslandType.SPIDER_DEN, IslandType.THE_PARK, IslandType.THE_END, IslandType.CRIMSON_ISLE),
        0,
        14
    ),
    CURRENT_EVENT(
        listOf("§cCurrent Event"),
        listOf(""),
        listOf(),
        0,
        15
    ),
    MAYOR(
        listOf(
            MayorElection.currentCandidate?.name?.let { translateMayorNameToColor(it) } ?: "<hidden>"
        ) + (if (config.showMayorPerks) {
            MayorElection.currentCandidate?.perks?.map { " §7- §e${it.name}" } ?: emptyList()
        } else {
            emptyList()
        }),
        listOf(),
        listOf(IslandType.THE_RIFT),
        1,
        16
    ),
    EMPTY_LINE3(
        listOf("<empty>"),
        listOf(),
        listOf(),
        0,
        17
    ),
    HEAT(
        listOf(if(heat == "0") "Heat: §c♨ 0" else "Heat: $heat"),
        listOf(if(heat == "0") "§c♨ 0 Heat" else "$heat Heat"),
        listOf(IslandType.CRYSTAL_HOLLOWS),
        0,
        18,
        heat
    ),
    PARTY(
        listOf(
            "§9Party",
            *PartyAPI.partyMembers.takeWhile { partyCount < config.maxPartyList.get() }
                .map { " §7- §7$it" }
                .toTypedArray()
        ),
        listOf(),
        listOf(IslandType.CATACOMBS, IslandType.DUNGEON_HUB, IslandType.KUUDRA_ARENA, IslandType.CRIMSON_ISLE),
        0,
        19,
        partyCount.toString()
    ),
    MAXWELL(
        listOf("§7Maxwell Power"),
        listOf(),
        listOf(IslandType.THE_RIFT),
        1,
        20
    ),
    WEBSITE(
        listOf("§ewww.hypixel.net"),
        listOf(),
        listOf(),
        0,
        21
    );
}

class CustomScoreboard {
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.inSkyBlock) return
        config.position.renderStringsAndItems(display, posLabel = "Custom Scoreboard")
    }

    @SubscribeEvent
    fun onTick(event: TickEvent) {
        // Draws the custom scoreboard
        display = drawScoreboard()

        // Resets Party count
        partyCount = 0

        // Gets some values for the scoreboard
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
            if (element.data == "0" && config.hideEmptyLines){ // Hide empty lines
                lineMap[element.index] = listOf("<hidden>")
                continue
            }

            lineMap[element.index] = formatLine(element)
        }

        return formatDisplay(lineMap)
    }

    private fun formatLine(element: CustomScoreboardElements) : List<Any>{
        if (element.alternativeLine.isEmpty()) return element.displayLine
        return if (config.displayNumbersFirst) element.alternativeLine else element.displayLine
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
