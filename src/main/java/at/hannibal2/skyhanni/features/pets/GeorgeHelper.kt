package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object GeorgeHelper {

    private val config get() = SkyHanniMod.feature.misc.pets.tamingSixty
    private val useFandomWiki get() = SkyHanniMod.feature.misc.commands.betterWiki.useFandom
    private const val SPAWN_EGG_SLOT = 41

    private val patternGroup = RepoPattern.group("george.taming-sixty")

    /**
     * REGEX-TEST:   §dMythic Enderman
     * REGEX-TEST:   §6Legendary Black Cat
     * REGEX-TEST:   §5Epic Rift Ferret
     * REGEX-TEST:   §5Epic Jellyfish
     * REGEX-TEST:   §9Rare Frost Wisp
     */
    private val neededPetPattern by patternGroup.pattern(
        "needed-pet.loreline",
        "(?i) *(?<fullThing>(?<tierColorCodes>§.)*(?<tier>(?:un)?common|rare|epic|legendary|mythic) (?<pet>[\\S ]+))",
    )

    init {
        InventoryDetector(
            openInventory = { DelayedRun.runNextTick { checkInventoryItems() } },
        ) { name ->
            name == "Offer Pets"
        }
    }

    private var display = emptyList<Renderable>()

    private fun checkInventoryItems() {
        val items = InventoryUtils.getItemsAtSlots(SPAWN_EGG_SLOT)

        constructDisplay(items[0].getLore())
    }

    private fun constructDisplay(lore: List<String>) {
        var totalCost = 0.0
        display = buildList {
            addString("§dTaming 60 Helper")
            lore.forEach { line ->
                neededPetPattern.matchMatcher(line) {
                    val petInfo = findCheapestPet(group("tier"), group("pet"), group("tierColorCodes"))
                    if (petInfo.petPrice > 0) totalCost += petInfo.petPrice
                    add(petInfo.renderableInfo)
                }
            }
            addString("§7Total Cost: §6${totalCost.addSeparators()} coins")
        }
    }

    private fun findCheapestPet(tierName: String, petName: String, tierColorCodes: String): PetInfo {
        val colorlessPetName = petName.removeColor().uppercase().replace(" ", "_")
        val tierNumber = LorenzRarity.getByName(tierName.uppercase().replace(" ", "_"))?.id ?: 0

        val (cheapestTier, cheapestPrice) = findCheapestTier(colorlessPetName, tierNumber)

        val stackRenderableInternalName = petInternalName(colorlessPetName, tierNumber).toInternalName()
        val stack = Renderable.item(stackRenderableInternalName)

        val rarityColorCode = LorenzRarity.getById(cheapestTier)?.chatColorCode ?: tierColorCodes
        val formattedPet = "${rarityColorCode}${LorenzRarity.getById(cheapestTier)?.formattedName} $petName"

        val clickableRenderable = if (cheapestPrice > 0) {
            val tips = mutableListOf(
                "§eClick to find a $formattedPet §eon the AH!",
                "§7(Make sure to adjust the rarity filter.)",
            )
            if (cheapestTier < tierNumber) tips.add(
                "§7(Does not include costs to upgrade via Kat.)",
            )
            Renderable.clickable(
                text = " §7- $formattedPet: §6${cheapestPrice.addSeparators()} coins",
                tips = tips,
                onLeftClick = { HypixelCommands.auctionSearch("] $petName") },
            )
        } else {
            val selectedWiki = if (useFandomWiki) "Fandom" else "Hypixel"
            Renderable.clickable(
                text = " §7- $formattedPet: §cNo price found. §eSee the $selectedWiki Wiki.",
                tips = listOf("§eView the $selectedWiki Wiki article for $formattedPet§e."),
                onLeftClick = {
                    val urlCompliantPet = formattedPet.removeColor().replace(" ", "%20")
                    val petURL = if (useFandomWiki) {
                        "https://hypixel-skyblock.fandom.com/wiki/Special:Search?query=$urlCompliantPet&scope=internal"
                    } else {
                        "https://wiki.hypixel.net/index.php?search=$urlCompliantPet"
                    }
                    OSUtils.openBrowser(petURL)
                },
            )
        }

        return PetInfo(cheapestPrice, Renderable.horizontal(stack, clickableRenderable))
    }

    private fun findCheapestTier(pet: String, originalTier: Int) = buildList {
        this.add(originalTier to petInternalName(pet, originalTier).getPetPrice())
        if (config.otherTiers) {
            this.add(originalTier - 1 to petInternalName(pet, originalTier - 1).getPetPrice(otherRarity = true))
            if (originalTier != 5) this.add(
                originalTier + 1 to petInternalName(pet, originalTier + 1).getPetPrice(
                    otherRarity = true,
                ),
            )
        }
    }.minBy { it.second }

    private data class PetInfo(
        var petPrice: Double,
        var renderableInfo: HorizontalContainerRenderable,
    )

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnIsland = IslandType.HUB)
    fun onRenderOverlay() {
        if (!config.enabled) return
        if (display.isEmpty()) return
        config.position.renderRenderables(display, posLabel = "Taming 60 Helper")
    }

    @HandleEvent
    fun onInventoryClose() {
        display = emptyList()
    }

    private fun petInternalName(pet: String, tier: Int) = "$pet;$tier"
    private fun String.getPetPrice(otherRarity: Boolean = false): Double =
        this.toInternalName().getPriceOrNull() ?: if (otherRarity) Double.MAX_VALUE else -1.0
}
