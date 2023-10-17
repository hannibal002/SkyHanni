// https://discord.com/channels/997079228510117908/1162844830360146080


package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

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
        update()
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            newList.add(map[index])
        }
        return newList
    }

    private fun update() {
        display = formatDisplay(drawScoreboard())
    }

    private fun drawScoreboard() = buildList<List<Any>> {
        addAsSingletonList("§6§lSKYBLOCK")
        addAsSingletonList("${getProfileTypeAsSymbol()} ${HypixelData.profileName.firstLetterUppercase()}")
        addAsSingletonList("§ePurse")
        addAsSingletonList("§eBank")
        addAsSingletonList("§bBits")
        addAsSingletonList("§cCopper")
        addAsSingletonList("§aGems")
        addAsSingletonList("§7Location")
        addAsSingletonList("§7Ingame Time")
        addAsSingletonList("§7Current Server")
        addAsSingletonList("§2Mithril §r/§2Gemstone §7Powder")
        addAsSingletonList("§cSlayer")
        addAsSingletonList("§7Next Event")
        addAsSingletonList("§7Current Event")
        addAsSingletonList("§2Soulflow")
        addAsSingletonList("§cHeat")

        addAsSingletonList("§9Party")
        for (member in PartyAPI.partyMembers){
            addAsSingletonList(" §7- §7$member")
        }

        addAsSingletonList("§7Pet")
        addAsSingletonList("§7Quiver (approximation)")
        addAsSingletonList("§7Maxwell Power")
    }

    private fun getProfileTypeAsSymbol() : String{
        if (HypixelData.ironman){
            return "§7♲"
        }
        if (HypixelData.stranded){
            return "§a☀"
        }
        if (HypixelData.bingo){
            return "§cⒷ" //TODO GET FUNNY BINGO SYMBOL, ALSO COLORS LOL
        }
        return "§e"
    }
}