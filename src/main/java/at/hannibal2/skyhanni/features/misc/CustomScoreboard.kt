//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni, and snippets from item tracker features <3
//


package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class CustomScoreboard {
    private val config get() = SkyHanniMod.feature.misc.customScoreboard
    private var display = emptyList<List<Any>>()
    private var purse = "0"
    private var bank = "0"
    private var bits = "0"
    private var copper = "0"
    private var gems = "0"

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
        }
        bits = getBits()
        purse = LorenzUtils.formatInteger(PurseAPI.currentPurse.toInt())
        copper = getCopper()
    }

    private fun formatDisplay(lineMap: HashMap<Int, List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            lineMap[index]?.let {

                // Multiline for Party Members
                if (it[0] == "§9Party"){
                    newList.add(listOf(it[0]))
                    for (item in it){
                        if (item != it[0]){
                            newList.add(listOf(item))
                        }
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
        lineMap[0] = Collections.singletonList("§6§lSKYBLOCK")
        lineMap[1] = Collections.singletonList("${getProfileTypeAsSymbol()}${HypixelData.profileName.firstLetterUppercase()}")
        lineMap[2] = Collections.singletonList("Purse: §6$purse")
        lineMap[3] = Collections.singletonList("Bank: §6$bank")
        lineMap[4] = Collections.singletonList("Bits: §b$bits")
        lineMap[5] = Collections.singletonList("Copper: §c$copper")
        lineMap[6] = Collections.singletonList("Gems: §a$gems")
        lineMap[7] = Collections.singletonList("<empty>")
        lineMap[8] = Collections.singletonList("§7Location")
        lineMap[9] = Collections.singletonList("§7Ingame Time")
        lineMap[10] = Collections.singletonList("§7IRL Time")
        lineMap[11] = Collections.singletonList("§7Current Server")
        lineMap[12] = Collections.singletonList("§2Mithril §r/§2Gemstone §7Powder") //could be multiline, need to decide
        lineMap[13] = Collections.singletonList("<empty>")

        val slayerList = mutableListOf<Any>()
        slayerList.add("§7Slayer") //get slayer stuff
        lineMap[14] = slayerList

        lineMap[15] = Collections.singletonList("§7Next Event")

        val eventList = mutableListOf<Any>()
        eventList.add("§cCurrent Event") //get event stuff
        lineMap[16] = eventList
        lineMap[17] = Collections.singletonList("<empty>")

        lineMap[18] = Collections.singletonList("§cHeat")

        val partyList = mutableListOf<Any>()
        var partyCount = 0
        partyList.add("§9Party")
        for (member in PartyAPI.partyMembers){
            if (partyCount >= config.maxPartyList.get()) break
            partyList.add(" §7- §7$member")
            partyCount++
        }
        lineMap[19] = partyList

        lineMap[20] = Collections.singletonList("§7Pet")
        lineMap[21] = Collections.singletonList("§7Quiver")
        lineMap[22] = Collections.singletonList("§7Maxwell Power")
        lineMap[23] = Collections.singletonList("§ewww.hypixel.net")

        // Hide empty lines
        if (config.hideEmptyLines){
            lineMap[2] = Collections.singletonList(if(purse == "0") "<hidden>" else "Purse: §6$purse")
            lineMap[3] = Collections.singletonList(if(bank == "0") "<hidden>" else "Bank: §6$bank")
            lineMap[4] = Collections.singletonList(if(bits == "0") "<hidden>" else "Bits: §b$bits")
            lineMap[5] = Collections.singletonList(if(copper == "0") "<hidden>" else "Copper: §c$copper")
            lineMap[6] = Collections.singletonList(if(gems == "0") "<hidden>" else "Gems: §a$gems")

            if (partyList.size == 1){
                lineMap[19] = Collections.singletonList("<hidden>")
            }
        }

        return formatDisplay(lineMap)
    }

    private fun getBits() : String {
        val bitsRegex = Regex("""Bits: ([\d|,]+)[\d|.]*""")
        val scoreboard = ScoreboardData.sidebarLinesFormatted
        val bits = scoreboard.firstOrNull { bitsRegex.matches(it.removeColor()) }?.let {
            bitsRegex.find(it.removeColor())?.groupValues?.get(1)
        }
        return bits ?: "0"
    }

    private fun getCopper() : String {
        val copperRegex = Regex("""Copper: ([\d|,]+)[\d|.]*""")
        val scoreboard = ScoreboardData.sidebarLinesFormatted
        val copper = scoreboard.firstOrNull { copperRegex.matches(it.removeColor()) }?.let {
            copperRegex.find(it.removeColor())?.groupValues?.get(1)
        }
        return copper ?: "0"
    }

    private fun getProfileTypeAsSymbol() : String{
        if (HypixelData.ironman){
            return "§7♲ "
        }
        if (HypixelData.stranded){
            return "§a☀ "
        }
        if (HypixelData.bingo){
            return "§cⒷ " //TODO COLORS LOL, maybe bingoAPI? idk
        }
        return "§e"
    }
}