package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.Companion.isBazaarItem
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CraftMaterialsFromBazaar {

    private val config get() = SkyHanniMod.feature.inventory.bazaar

    private val materialSlots = listOf(10, 11, 12, 19, 20, 21, 28, 29, 30)
    private val pattern by RepoPattern.pattern(
        "inventory.recipe.title",
        ".* Recipe"
    )

    private var inRecipeInventory = false
    private var purchasing = false
    private var display = listOf<Renderable>()
    private var neededMaterials = mapOf<NEUInternalName, Int>()
    private var multiplier = 1

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        val correctInventoryName = pattern.matches(event.inventoryName)
        val items = event.inventoryItems
        val correctItem = items[23]?.name == "§aCrafting Table"

        inRecipeInventory = correctInventoryName && correctItem && !purchasing
        if (!inRecipeInventory) return

        val recipeName = items[25]?.itemName ?: return
        showRecipe(calculateMaterialsNeeded(items), recipeName)
    }

    private fun showRecipe(
        recipeMaterials: MutableMap<NEUInternalName, Int>,
        recipeName: String,
    ) {
        val neededMaterials = mutableMapOf<NEUInternalName, Int>()
        display = buildList {
            val totalPrice = calculateTotalPrice(recipeMaterials, 1)
            add(Renderable.string("§7Craft $recipeName §7(§6${NumberUtil.format(totalPrice)}§7)"))
            for ((material, amount) in recipeMaterials) {
                var text = "§8${amount.addSeparators()}x " + material.itemName
                if (material.isBazaarItem()) {
                    neededMaterials[material] = amount
                    text += " §6${NumberUtil.format(material.getPrice() * amount)}"
                }
                add(Renderable.string(text))
            }
            if (neededMaterials.isNotEmpty()) {
                add(
                    Renderable.clickAndHover(
                        "§eGet from bazaar!",
                        listOf("§eClick here to buy the items from bazaar!"),
                        onClick = {
                            getFromBazaar(neededMaterials)
                        })
                )
            }
        }
    }

    private fun calculateMaterialsNeeded(items: Map<Int, ItemStack>): MutableMap<NEUInternalName, Int> {
        val recipeMaterials = mutableMapOf<NEUInternalName, Int>()
        for (slot in materialSlots) {
            val item = items[slot] ?: continue
            val internalName = item.getInternalNameOrNull() ?: continue
            val size = item.stackSize
            recipeMaterials.addOrPut(internalName, size)
        }
        return recipeMaterials
    }

    private fun getFromBazaar(neededMaterials: MutableMap<NEUInternalName, Int>) {
        this.neededMaterials = neededMaterials
        this.multiplier = 1
        purchasing = true
        update()
    }

    private fun update() {
        display = buildList {
            add(Renderable.string("§7Buy items from Bazaar:"))
            for ((material, amount) in neededMaterials) {
                val priceMultiplier = amount * multiplier
                var text = "§8${priceMultiplier.addSeparators()}x " + material.itemName
                if (material.isBazaarItem()) {
                    text += " §6${NumberUtil.format(material.getPrice() * priceMultiplier)}"
                }
                add(Renderable.optionalLink(text, onClick = {
                    BazaarApi.searchForBazaarItem(material.itemNameWithoutColor, priceMultiplier)
                }))
            }
            add(
                Renderable.clickAndHover(
                    "§eStop!",
                    listOf("§eClick here to stop this view!"),
                    onClick = {
                        purchasing = false
                        display = emptyList()
                    })
            )
            addMultipliers()
        }
    }

    private fun MutableList<Renderable>.addMultipliers() {
        for (m in listOf(1, 5, 16, 32, 64, 512)) {
            val nameColor = if (m == multiplier) "§amount" else "§e"
            val priceColor = if (m == multiplier) "§6" else "§7"
            val price = priceColor + NumberUtil.format(calculateTotalPrice(neededMaterials, m))
            add(
                Renderable.clickAndHover(
                    "${nameColor}Mulitply x$m $price",
                    listOf("§eClick here to multiply the items needed times $m!"),
                    onClick = {
                        multiplier = m
                        update()
                    })
            )
        }
    }

    private fun calculateTotalPrice(neededMaterials: Map<NEUInternalName, Int>, multiplier: Int): Double =
        neededMaterials
            .filter { it.key.isBazaarItem() }
            .map { it.key.getPrice() * it.value * multiplier }
            .sum()

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inRecipeInventory = false
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inRecipeInventory && !purchasing) return

        config.craftMaterialsFromBazaarPosition.renderRenderables(display, posLabel = "Craft Materials From Bazaar")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.craftMaterialsFromBazaar

}
