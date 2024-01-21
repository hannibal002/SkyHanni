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
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GeorgeDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.george

    private val SEPARATOR = ";"

    private val patternGroup = RepoPattern.group("george.tamingcap")
    private val neededPetPattern by patternGroup.pattern(
        "needed.pet.loreline",
        "(?i) +(?<fullThing>(?<tierColorCodes>§.)*(?<tier>${enumJoinToPattern<LorenzRarity>{it.rawName.lowercase()}}) (?<pet>[\\S ]+))"
    )
    private val offerPetsChestPattern by patternGroup.pattern(
        "offerpets.chestname",
        "Offer Pets"
    )
    private val increaseCapItemPattern by patternGroup.pattern(
        "increasecap.itemname",
        "\\+1 Taming Level Cap"
    )

    private var display = listOf<Renderable>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (!offerPetsChestPattern.matches(event.inventoryName)) return
        val stack = event.inventoryItems[41] ?: return //the slot #41 = spawn egg with the tooltip
        if (!increaseCapItemPattern.matches(stack.cleanName())) return
        display = drawDisplay(stack.getLore())
    }

    private fun drawDisplay(lore: List<String>) = buildList<Renderable> {
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
                val originalTier = LorenzRarity.getByName(origTierString.uppercase().replace(" ", "_") )?.id ?: 0
                val (cheapestTier, petPrice) = findCheapestTier(pet, originalTier)
                val displayPetString =
                    if (cheapestTier == originalTier) group("fullThing")
                    else "${LorenzRarity.getById(cheapestTier )?.formattedName} $origPetString"
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
        return this
    }

    private fun findCheapestTier(pet: String, originalTier: Int): IndexedValue<Double> {
        val petPriceOne = petInternalName(pet, originalTier).getPetPrice()
        val petPrices= mutableListOf(petPriceOne)
        if (config.otherRarities || petPriceOne == -1.0) {
            petPrices.add(petInternalName(pet, originalTier - 1).getPetPrice(otherRarity = true))
            if (originalTier != 5) petPrices.add(petInternalName(pet, originalTier + 1).getPetPrice(otherRarity = true))
        }
        return petPrices.withIndex().minBy { it.value }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Offer Pets") return
        config.position.renderRenderables(display, posLabel = "George Display")
    }

    private fun String.getPetPrice(otherRarity: Boolean = false): Double = this.asInternalName().getPriceOrNull() ?: if (otherRarity) Double.MAX_VALUE else -1.0
    private fun petInternalName(pet: String, originalTier: Int) = "$pet$SEPARATOR$originalTier"
    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
