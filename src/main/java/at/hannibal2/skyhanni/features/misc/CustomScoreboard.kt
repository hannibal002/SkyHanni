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
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class CustomScoreboard {
    private val config get() = SkyHanniMod.feature.misc.customScoreboard
    private var display = emptyList<List<Any>>()
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

    // Indexes for the scoreboard
    private var skyblockIndex = 0
    private var profileIndex = 1
    private var purseIndex = 2
    private var bankIndex = 3
    private var bitsIndex = 4
    private var copperIndex = 5
    private var gemsIndex = 6
    private var EMPTY_LINE = 7
    private var locationIndex = 8
    private var skyblockTimeIndex = 9
    private var lobbyCodeIndex = 10
    private var powderIndex = 11
    private var EMPTY_LINE2 = 12
    private var slayerIndex = 13
    private var currentEventIndex = 14
    private var mayorIndex = 15
    private var EMPTY_LINE3 = 16
    private var heatIndex = 17
    private var partyIndex = 18
    private var maxwellIndex = 19
    private var websiteIndex = 20


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

    private fun formatDisplay(lineMap: HashMap<Int, List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            lineMap[index]?.let {

                // Multiline support
                if (it[0] == "§9Party"
                    || it[0].toString().contains(MayorElection.currentCandidate?.name ?: "")
                    || it[0] == "§fPowder"
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

    private fun drawScoreboard() = buildList<List<Any>> {
        val lineMap = HashMap<Int, List<Any>>()
        lineMap[skyblockIndex] = Collections.singletonList("§6§lSKYBLOCK")
        lineMap[profileIndex] = Collections.singletonList("${getProfileTypeAsSymbol()}${HypixelData.profileName.firstLetterUppercase()}")
        lineMap[purseIndex] = Collections.singletonList("Purse: §6$purse")
        lineMap[bankIndex] = Collections.singletonList("Bank: §6$bank")
        lineMap[bitsIndex] = Collections.singletonList("Bits: §b$bits")
        lineMap[copperIndex] = Collections.singletonList("Copper: §c$copper")
        lineMap[gemsIndex] = Collections.singletonList("Gems: §a$gems")
        lineMap[EMPTY_LINE] = Collections.singletonList("<empty>")
        lineMap[locationIndex] = Collections.singletonList(location)
        lineMap[skyblockTimeIndex] = Collections.singletonList(SkyBlockTime.now().formatted(false))
        lineMap[lobbyCodeIndex] = Collections.singletonList("§8$lobbyCode")

        val powderList = mutableListOf<Any>()
        powderList.add("§fPowder")
        powderList.add(" §7- §fMithril: §2$mithrilPowder")
        powderList.add(" §7- §fGemstone: §d$gemstonePowder")
        lineMap[powderIndex] = powderList

        lineMap[EMPTY_LINE2] = Collections.singletonList("<empty>")

        val slayerList = mutableListOf<Any>()
        slayerList.add("§7Slayer") //todo: get slayer stuff
        lineMap[slayerIndex] = slayerList

        val eventList = mutableListOf<Any>()
        eventList.add("§cCurrent Event") //todo: get event stuff
        lineMap[currentEventIndex] = eventList

        val mayorList = mutableListOf<Any>()
        mayorList.add(MayorElection.currentCandidate?.name?.let { translateMayorNameToColor(it) } ?: "<hidden>")
        if (config.showMayorPerks) {
            for (perk in MayorElection.currentCandidate?.perks ?: emptyList()) {
                mayorList.add(" §7- §e${perk.name}")
            }
        }
        lineMap[mayorIndex] = mayorList

        lineMap[EMPTY_LINE3] = Collections.singletonList("<empty>")
        lineMap[heatIndex] = Collections.singletonList(if(heat == "0") "Heat: §c♨ 0" else "Heat: $heat")

        val partyList = mutableListOf<Any>()
        var partyCount = 0
        partyList.add("§9Party")
        for (member in PartyAPI.partyMembers){
            if (partyCount >= config.maxPartyList.get()) break
            partyList.add(" §7- §7$member")
            partyCount++
        }
        lineMap[partyIndex] = partyList

        lineMap[maxwellIndex] = Collections.singletonList("§7Maxwell Power")
        lineMap[websiteIndex] = Collections.singletonList("§ewww.hypixel.net")

        // Hide empty lines
        if (config.hideEmptyLines){
            lineMap[purseIndex] = Collections.singletonList(if(purse == "0") "<hidden>" else "Purse: §6$purse")
            lineMap[bankIndex] = Collections.singletonList(if(bank == "0") "<hidden>" else "Bank: §6$bank")
            lineMap[bitsIndex] = Collections.singletonList(if(bits == "0") "<hidden>" else "Bits: §b$bits")
            lineMap[copperIndex] = Collections.singletonList(if(copper == "0") "<hidden>" else "Copper: §c$copper")
            lineMap[gemsIndex] = Collections.singletonList(if(gems == "0") "<hidden>" else "Gems: §a$gems")
            lineMap[locationIndex] = Collections.singletonList(if(location == "None") "<hidden>" else location)
            lineMap[lobbyCodeIndex] = Collections.singletonList(if(lobbyCode == "None") "<hidden>" else "§8$lobbyCode")
            lineMap[heatIndex] = Collections.singletonList(if(heat == "0") "<hidden>" else "Heat: §c♨$heat")

            if (partyList.size == 1){
                lineMap[partyIndex] = Collections.singletonList("<hidden>")
            }
        }

        // Rift
        if(IslandType.THE_RIFT.isInIsland()){
            lineMap[purseIndex] = Collections.singletonList("Motes: §d$motes")
        }

        // Hide irrelevant lines
        if (config.hideIrrelevantLines){
            if (!IslandType.GARDEN.isInIsland()){
                lineMap[copperIndex] = Collections.singletonList("<hidden>")
            }
            if (IslandType.THE_RIFT.isInIsland()){
                lineMap[bankIndex] = Collections.singletonList("<hidden>")
                lineMap[bitsIndex] = Collections.singletonList("<hidden>")
                lineMap[gemsIndex] = Collections.singletonList("<hidden>")
                lineMap[mayorIndex] = Collections.singletonList("<hidden>")
            }
            if (!IslandType.DWARVEN_MINES.isInIsland()
                && !IslandType.CRYSTAL_HOLLOWS.isInIsland()
            ){
                lineMap[powderIndex] = Collections.singletonList("<hidden>")
            }
            if (!IslandType.CRYSTAL_HOLLOWS.isInIsland()){
                lineMap[heatIndex] = Collections.singletonList("<hidden>")
            }
            if (!IslandType.DUNGEON_HUB.isInIsland()
                && !IslandType.CATACOMBS.isInIsland()
                && !IslandType.KUUDRA_ARENA.isInIsland()
                && !IslandType.CRIMSON_ISLE.isInIsland()
            ){
                lineMap[partyIndex] = Collections.singletonList("<hidden>")
            }
            if (!IslandType.HUB.isInIsland()
                && !IslandType.SPIDER_DEN.isInIsland()
                && !IslandType.THE_PARK.isInIsland()
                && !IslandType.THE_END.isInIsland()
                && !IslandType.CRIMSON_ISLE.isInIsland()
            ){
                lineMap[slayerIndex] = Collections.singletonList("<hidden>")
            }
        }

        return formatDisplay(lineMap)
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
}
