//
// Requested by alpaka8123 (https://discord.com/channels/997079228510117908/1162844830360146080)
// Done by J10a1n15, with lots of help from hanni, and snippets from item tracker features <3
//


package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import kotlin.collections.HashMap

class CustomScoreboard {
    private val config get() = SkyHanniMod.feature.misc.customScoreboard
    private var display = emptyList<List<Any>>()
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.inSkyBlock) return
        config.position.renderStringsAndItems(display, posLabel = "Custom Scoreboard")
    }

    @SubscribeEvent
    fun onTick(event: TickEvent) {
        display = drawScoreboard()
    }

    private fun formatDisplay(lineMap: HashMap<Int, List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            lineMap[index]?.let {

                //Multiline for Party Members (breaks nothing)
                if (it[0] == "§9Party"){
                    newList.add(listOf(it[0]))
                    for (item in it){
                        if (item != it[0]){
                            newList.add(listOf(item))
                        }
                    }
                    continue
                }

                if(it[0] == "<empty>"){
                    newList.add(listOf(""))
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
        lineMap[2] = Collections.singletonList("§ePurse")
        lineMap[3] = Collections.singletonList("§eBank")
        lineMap[4] = Collections.singletonList("§bBits")
        lineMap[5] = Collections.singletonList("§cCopper")
        lineMap[6] = Collections.singletonList("§aGems")
        lineMap[7] = Collections.singletonList("§7Location")
        lineMap[8] = Collections.singletonList("§7Ingame Time")
        lineMap[9] = Collections.singletonList("§7IRL Time")
        lineMap[10] = Collections.singletonList("§7Current Server")
        lineMap[11] = Collections.singletonList("§2Mithril §r/§2Gemstone §7Powder") //could be multiline, need to decide

        val slayerList = mutableListOf<Any>()
        slayerList.add("§7Slayer") //get slayer stuff
        lineMap[12] = slayerList

        lineMap[13] = Collections.singletonList("§7Next Event")

        val eventList = mutableListOf<Any>()
        eventList.add("§cCurrent Event") //get event stuff
        lineMap[14] = eventList

        lineMap[15] = Collections.singletonList("§cHeat")

        val partyList = mutableListOf<Any>()
        var partyCount = 0
        partyList.add("§9Party")
        for (member in PartyAPI.partyMembers){
            if (partyCount == config.maxPartyList.get()) break
            partyList.add(" §7- §7$member")
            partyCount++
        }
        lineMap[16] = partyList

        lineMap[17] = Collections.singletonList("§7Pet")
        lineMap[18] = Collections.singletonList("§7Quiver")
        lineMap[19] = Collections.singletonList("§7Maxwell Power")
        lineMap[20] = Collections.singletonList("§ewww.hypixel.net")
        lineMap[21] = Collections.singletonList("<empty>")
        lineMap[22] = Collections.singletonList("<empty>")
        lineMap[23] = Collections.singletonList("<empty>")
        lineMap[24] = Collections.singletonList("<empty>")

        return formatDisplay(lineMap)
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