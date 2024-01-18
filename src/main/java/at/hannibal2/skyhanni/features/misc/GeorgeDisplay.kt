package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
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

    private val SEPARATOR: String = ";"

    private val patternGroup = RepoPattern.group("george.tamingcap")
    private val neededPetPattern by patternGroup.pattern(
        "needed.pet.loreline",
        "(?i) +(?<fullThing>(?<tierColorCodes>§.)*(?<tier>(?:un)?common|rare|epic|legendary|mythic|divine|(?:very )?special|ultimate|supreme|admin) (?<pet>[\\S ]+))"
    )

    private val display = mutableListOf<Renderable>()

    private enum class RarityToTier(
        val rarityInternal: String,
        val tier: Int,
        val rarityDisplay: String,
    ) {
        COMMON("COMMON", 0, "§fCommon"),
        UNCOMMON("UNCOMMON", 1, "§aUncommon"),
        RARE("RARE", 2, "§9Rare"),
        EPIC("EPIC", 3, "§5Epic"),
        LEGENDARY("LEGENDARY", 4, "§6Legendary"),
        MYTHIC("MYTHIC", 5, "§dMythic"),
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Offer Pets") return
        val stack = event.inventoryItems[ROW_OFFSET + INDEX_OFFSET] ?: return
        if (stack.cleanName() != "+1 Taming Level Cap") return
        display.clear()
        display.addAll(listBuilding(stack.getLore()))
    }

    private fun listBuilding(lore: List<String>): MutableList<Renderable> {
        val updateList: MutableList<Renderable> = mutableListOf<Renderable>(Renderable.string("§d§lTaming 60 Cost: §r§d(${if (config.otherRarities) "cheapest" else "exact"} rarity)"))
        var totalCost: Double = 0.0
        for (line in lore) {
            neededPetPattern.matchMatcher(line) {
                val origTierString = group("tier") ?: ""
                val tier = RarityToTier.entries.find { it.rarityInternal == origTierString.uppercase() }?.tier ?: -1
                val origPetString = group("pet") ?: ""
                val pet = origPetString.uppercase().replace(" ", "_").removePrefix("FROST_")
                val petPrices: MutableList<Double> = mutableListOf<Double>()
                val petPriceOne = "$pet;$tier".asInternalName().getPriceOrNull() ?: -1.0
                petPrices.add(petPriceOne)
                if (config.otherRarities || petPriceOne == -1.0) {
                    val lowerTier = "$pet$SEPARATOR${tier - 1}".asInternalName().getPriceOrNull() ?: Double.MAX_VALUE
                    petPrices.add(lowerTier)
                    if (tier != 5) {
                        val higherTier = "$pet$SEPARATOR${tier + 1}".asInternalName().getPriceOrNull() ?: Double.MAX_VALUE
                        petPrices.add(higherTier)
                    }
                }
                val petPrice = petPrices.min()
                val tierUsed = when (petPrices.indexOf(petPrice)) {
                    1 -> tier - 1
                    2 -> tier + 1
                    else -> tier
                }
                val displayPetString = if (tierUsed == tier) group("fullThing") else {
                    "${RarityToTier.entries.find { it.tier == tierUsed }?.rarityDisplay} $origPetString"
                }
                if (petPrice != -1.0) {
                    totalCost += petPrice
                    updateList.add(Renderable.clickAndHover(
                        text = " §7- $displayPetString§7: §6${petPrice.addSeparators()} coins",
                        tips = listOf(
                            "§aClick to run §e/ahs ] $origPetString §ato find it on the Auction House.",
                            "§aNotes: §eSet the rarity filter yourself. §cBooster Cookie required!"
                        ),
                        onClick = { LorenzUtils.sendCommandToServer("ahs ] $origPetString") }
                    ))
                } else {
                    updateList.add(Renderable.clickAndHover(
                        text = " §7- $displayPetString§7: §eNot on AH; view its Wiki article here.",
                        tips = listOf("§4Click to run §e/wiki $pet §4to view how to obtain it."),
                        onClick = { LorenzUtils.sendCommandToServer("wiki $pet") }
                    ))
                }
            }
        }
        updateList.add(Renderable.string("§dTotal cost §7(§6Lowest BIN§7): §6${totalCost.addSeparators()} coins"))
        if (config.otherRarities) updateList.add(Renderable.string("§c§lDisclaimer:§r§c Total does not include costs to upgrade via Kat."))
        return updateList
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Offer Pets") return
        config.position.renderRenderables(display, posLabel = "George Display")
    }

    private fun isEnabled() = config.enabled && LorenzUtils.inSkyBlock
}
