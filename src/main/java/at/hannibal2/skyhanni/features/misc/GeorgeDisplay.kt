package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.enumJoinToPattern
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

    private val SEPARATOR = ";"

    private val neededPetPattern by RepoPattern.pattern(
        "george.tamingcap.needed.pet.loreline",
        "(?i) +(?<fullThing>(?<tierColorCodes>§.)*(?<tier>${enumJoinToPattern<LorenzRarity>{it.rawName.lowercase()}}) (?<pet>[\\S ]+))"
    )

    private var display = listOf<Renderable>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Offer Pets") return
        val stack = event.inventoryItems[41] ?: return
        if (stack.cleanName() != "+1 Taming Level Cap") return
        display = listBuilding(stack.getLore())
    }

    private fun listBuilding(lore: List<String>): MutableList<Renderable> {
        val updateList: MutableList<Renderable> = mutableListOf(
            Renderable.string("§d§lTaming 60 Cost: §r§d(${
                if (config.otherRarities) "cheapest" else "exact"
            } rarity)")
        )
        var totalCost = 0.0
        for (line in lore) {
            neededPetPattern.matchMatcher(line) {
                val origTierString = group("tier")
                val origPetString = group("pet")
                val pet = origPetString.uppercase().replace(" ", "_").removePrefix("FROST_")
                val originalTier = LorenzRarity.entries.find { it.name == origTierString.uppercase() }!!.id
                val petPriceOne = "$pet$SEPARATOR$originalTier".getPetPrice()
                val petPrices: MutableList<Double> = mutableListOf(petPriceOne)
                if (config.otherRarities || petPriceOne == -1.0) {
                    petPrices.add("$pet$SEPARATOR${originalTier - 1}".getPetPrice(otherRarity = true))
                    if (originalTier != 5) petPrices.add("$pet$SEPARATOR${originalTier + 1}".getPetPrice(otherRarity = true))
                }
                val petPrice = petPrices.min()
                val cheapestTier = petPrices.cheapestTierIndex(petPrice, originalTier)
                val displayPetString =
                    if (cheapestTier == originalTier) group("fullThing")
                    else "${LorenzRarity.entries.find { it.id == cheapestTier }!!.formattedName} $origPetString"
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

    private fun MutableList<Double>.cheapestTierIndex(petPrice: Double, originalTier: Int) =
        when (this.indexOf(petPrice)) {
            1 -> originalTier - 1
            2 -> originalTier + 1
            else -> originalTier
        }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Offer Pets") return
        config.position.renderRenderables(display, posLabel = "George Display")
    }

    private fun String.getPetPrice(otherRarity: Boolean = false): Double = this.asInternalName().getPriceOrNull() ?: if (otherRarity) Double.MAX_VALUE else -1.0

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
