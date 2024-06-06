package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
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
import java.util.regex.Matcher

@SkyHanniModule
object GeorgeDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.george
    private val useFandomWiki get() = SkyHanniMod.feature.misc.commands.betterWiki.useFandom

    private var isValidChest = false

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
        isValidChest = true
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
                calculateCheapestPricesWithPets(this, totalCost, this@buildList)
            }
        this.add(Renderable.string("§dTotal cost §7(§6Lowest BIN§7): §6${totalCost.addSeparators()} coins"))
        if (config.otherRarities) this.add(Renderable.string("§c§lDisclaimer:§r§c Total does not include costs to upgrade via Kat."))
    }

    private fun calculateCheapestPricesWithPets(
        matcher: Matcher,
        totalCost: Double,
        renderables: MutableList<Renderable>
    ): Boolean {
        var totalCost1 = totalCost
        val origTierString = matcher.group("tier")
        val origPetString = matcher.group("pet")
        val pet = origPetString.convertToInternalNameString().removePrefix("FROST_")
        val originalTier = LorenzRarity.getByName(origTierString.uppercase().replace(" ", "_"))?.id ?: 0
        val (cheapestTier, petPrice) = findCheapestTier(pet, originalTier)
        val displayPetString = "${LorenzRarity.getById(cheapestTier)?.formattedName} $origPetString"
        return if (petPrice != -1.0) {
            totalCost1 += petPrice
            renderables.add(
                Renderable.clickAndHover(
                    text = " §7- $displayPetString§7: §6${petPrice.addSeparators()} coins",
                    tips = listOf(
                        "§eClick to run §a/ahs ] $origPetString §eto find it on the AH.",
                        "§eNotes: Set the rarity filter yourself. §cBooster Cookie required!"
                    ),
                    onClick = { ChatUtils.sendCommandToServer("ahs ] $origPetString") }
                ))
        } else {
            val typeOfWiki = if (useFandomWiki) "Fandom" else "Official Hypixel"
            renderables.add(
                Renderable.clickAndHover(
                    text = " §7- $displayPetString§7: §eNot on AH; open it on $typeOfWiki.",
                    tips = listOf("§eView the $typeOfWiki Wiki article for $displayPetString §ehere."),
                    onClick = {
                        val urlCompliantPet = displayPetString.removeColor().replace(" ", "%20")
                        if (useFandomWiki) OSUtils.openBrowser("https://hypixel-skyblock.fandom.com/wiki/Special:Search?query=$urlCompliantPet&scope=internal")
                        else OSUtils.openBrowser("https://wiki.hypixel.net/index.php?search=$urlCompliantPet")
                    }
                ))
        }
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
        if (!isValidChest) return
        config.position.renderRenderables(display, posLabel = "George Display")
    }

    @SubscribeEvent
    fun onRenderOverlay(event: InventoryCloseEvent) {
        if (!isEnabled() && !event.reopenSameName) return
        display = emptyList()
    }

    private fun String.getPetPrice(otherRarity: Boolean = false): Double = this.asInternalName().getPriceOrNull() ?: if (otherRarity) Double.MAX_VALUE else -1.0
    private fun petInternalName(pet: String, originalTier: Int) = "$pet;$originalTier"
    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
