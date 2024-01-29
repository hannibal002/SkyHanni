package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
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
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.convertToInternalNameString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GeorgeDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.george
    private val useFandomWiki get() = SkyHanniMod.feature.commands.fandomWiki.enabled

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
        this.add(
            Renderable.string(
                "§d§lTaming 60 Cost: §r§d(${
                    if (config.otherRarities) "cheapest" else "exact"
                } rarity)"
            )
        )
        var totalCost = 0.0
        for (line in lore)
            neededPetPattern.matchMatcher(line) {
                val origTierString = group("tier")
                val origPetString = group("pet")
                val pet = origPetString.convertToInternalNameString().removePrefix("FROST_")
                val originalTier = LorenzRarity.getByName(origTierString.uppercase().replace(" ", "_"))?.id ?: 0
                val (cheapestTier, petPrice) = findCheapestTier(pet, originalTier)
                val displayPetString = "${LorenzRarity.getById(cheapestTier)?.formattedName} $origPetString"
                if (petPrice != -1.0) {
                    totalCost += petPrice
                    this@buildList.add(
                        Renderable.clickAndHover(
                            text = " §7- $displayPetString§7: §6${petPrice.addSeparators()} coins",
                            tips = listOf(
                                "§aClick to run §e/ahs ] $origPetString §ato find it on the Auction House.",
                                "§aNotes: §eSet the rarity filter yourself. §cBooster Cookie required!"
                            ),
                            onClick = { LorenzUtils.sendCommandToServer("ahs ] $origPetString") }
                        ))
                } else {
                    val typeOfWiki = if (useFandomWiki) "Fandom" else "Official Hypixel"
                    this@buildList.add(
                        Renderable.clickAndHover(
                            text = " §7- $displayPetString§7: §eNot on AH; view its $typeOfWiki article here.",
                            tips = listOf("§eClick to open the $typeOfWiki Wiki article for the $displayPetString §eto view how to obtain it."),
                            onClick = {
                                val urlCompliantPet = displayPetString.removeColor().replace(" ", "%20")
                                if (useFandomWiki) OSUtils.openBrowser("https://hypixel-skyblock.fandom.com/wiki/Special:Search?query=$urlCompliantPet&scope=internal")
                                else OSUtils.openBrowser("https://wiki.hypixel.net/index.php?search=$urlCompliantPet")
                            }
                        ))
                }
            }
        this.add(Renderable.string("§dTotal cost §7(§6Lowest BIN§7): §6${totalCost.addSeparators()} coins"))
        if (config.otherRarities) this.add(Renderable.string("§c§lDisclaimer:§r§c Total does not include costs to upgrade via Kat."))
    }

    private fun findCheapestTier(pet: String, originalTier: Int) = buildList<Pair<Int, Double>> {
        this.add(originalTier to petInternalName(pet, originalTier).getPetPrice())
        if (config.otherRarities) {
            this.add(originalTier - 1 to petInternalName(pet, originalTier - 1).getPetPrice(otherRarity = true))
            if (originalTier != 5) this.add(
                originalTier + 1 to petInternalName(pet, originalTier + 1).getPetPrice(
                    otherRarity = true
                )
            )
        }
    }.minBy { it.second }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!offerPetsChestPattern.matches(InventoryUtils.openInventoryName())) return
        config.position.renderRenderables(display, posLabel = "George Display")
    }

    @SubscribeEvent
    fun onRenderOverlay(event: InventoryCloseEvent) {
        if (!isEnabled() && !event.reopenSameName) return
        display = emptyList()
    }

    private fun String.getPetPrice(otherRarity: Boolean = false): Double = this.asInternalName().getPriceOrNull() ?: if (otherRarity) Double.MAX_VALUE else -1.0
    private fun petInternalName(pet: String, originalTier: Int) = "$pet$SEPARATOR$originalTier"
    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
