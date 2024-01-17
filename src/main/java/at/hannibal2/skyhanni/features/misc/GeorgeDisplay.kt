package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GeorgeDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.george

    private val ROW_OFFSET: Int = 9 * 4
    private val INDEX_OFFSET: Int = 6 - 1

    private val patternGroup = RepoPattern.group("george.tamingcap")
    private val neededPetPattern by patternGroup.pattern(
        "needed.pet.loreline",
        " +(?<fullThing>(?<tierColorCodes>§.)*(?<tier>(?:[uU][nN])?[cC][oO][mM][mM][oO][nN]|[rR][aA][rR][eE]|[eE][pP][iI][cC]|[lL][eE][gG][eE][nN][dD][aA][rR][yY]|[mM][yY][tT][hH][iI][cC]|[dD][iI][vV][iI][nN][eE]|(?:[vV][eE][rR][yY] )?[sS][pP][eE][cC][iI][aA][lL]|[uU][lL][tT][iI][mM][aA][tT][eE]|[sS][uU][pP][rR][eE][mM][eE]|[aA][dD][mM][iI][nN]) (?<pet>[\\S ]+))"
    )

    private val display = mutableListOf<Renderable>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (event.inventoryName != "Offer Pets") return
        val stack = event.inventoryItems[ROW_OFFSET + INDEX_OFFSET] ?: return
        if (stack.cleanName() != "+1 Taming Level Cap") return
        val updateList: MutableList<Renderable> = mutableListOf<Renderable>(Renderable.string("§d§lTaming 60 Cost:"))
        var totalCost: Double = 0.0
        loop@ for (line in stack.getLore()) {
            neededPetPattern.matchMatcher(line) {
                val origTierString = group("tier")
                val tier = when (origTierString.uppercase()) {
                    "COMMON" -> "0"
                    "UNCOMMON" -> "1"
                    "RARE" -> "2"
                    "EPIC" -> "3"
                    "LEGENDARY" -> "4"
                    "MYTHIC" -> "5"
                    else -> ""
                }
                val origPetString = group("pet") ?: ""
                val pet = origPetString.uppercase().replace(" ", "_").removePrefix("FROST_")
                val petPrice = "$pet;$tier".asInternalName().getPriceOrNull() ?: continue@loop
                totalCost += petPrice
                updateList.add(Renderable.clickAndHover(
                    text = " §7- ${group("fullThing")}§7: §6${petPrice.addSeparators()} coins",
                    tips = listOf("§aClick to run §e/ahsearch [Lvl 1] $origPetString §ato find this pet on the Auction House.", "§aNotes: §eMake sure to set the rarity filter to ${group("tierColorCodes")}$origTierString §eon your own! §cBooster Cookie required!"),
                    onClick = {
                        LorenzUtils.sendCommandToServer("ahsearch [Lvl 1] $origPetString")
                    }
                ))
            }
        }
        updateList.addAll(listOf<Renderable>(Renderable.string(" §7- §dTotal cost §7(§6Lowest BIN§7): §6${totalCost.addSeparators()} coins"), Renderable.string("§c§lDisclaimer:§r§c Some pets are not available on the Auction House.")))
        updateList.update()
    }

    private fun List<Renderable>.update() {
        display.clear()
        display.addAll(this)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Offer Pets") return
        config.pos.renderRenderables(display, posLabel = "Taming 60 Progress")
    }

    private fun isEnabled() = config.enabled
}
