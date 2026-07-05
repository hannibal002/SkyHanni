package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.ItemBuyApi.buy
import at.hannibal2.skyhanni.api.ItemBuyApi.createBuyTip
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PrimitiveIngredient.Companion.toPrimitiveItemStacks
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SignUtils.isBazaarSign
import at.hannibal2.skyhanni.utils.SignUtils.isSupercraftAmountSetSign
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.InventoryGuiScaleCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import kotlin.time.Duration.Companion.minutes

/**
 * Renders the Visitor Shopping List HUD.
 * Shows what items are needed across all active visitors.
 */
@SkyHanniModule
object GardenVisitorShoppingList {

    private val config get() = VisitorApi.config.shoppingList
    private var display = emptyList<Renderable>()

    /**
     * Public method called by GardenVisitorStatus when inventory changes.
     * Triggers a full display rebuild.
     */
    fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        if (!config.enabled) return@buildList
        val (shoppingList, activeVisitors) = prepareDrawingData()
        val (newVisitors, knownVisitors) = activeVisitors.partition { visitor ->
            visitor.shoppingList.isEmpty()
        }

        drawShoppingList(shoppingList)
        drawVisitorSection(
            visitors = newVisitors,
            header = "New Visitor",
            renderer = {
                drawNewVisitor(it)
            },
        )
        drawVisitorSection(
            visitors = knownVisitors,
            header = "Visitor",
            renderer = {
                drawVisitor(it)
            },
        )
    }

    private fun MutableList<Renderable>.drawVisitorSection(
        visitors: List<VisitorApi.Visitor>,
        header: String,
        renderer: MutableList<Renderable>.(VisitorApi.Visitor) -> Unit,
    ) {
        if (visitors.isEmpty()) return

        if (isNotEmpty()) addString("")

        val amount = visitors.size
        val noun = if (amount == 1) header else "${header}s"

        addString("§e$amount §7$noun:")

        visitors.forEach { renderer(it) }
    }

    /**
     * Aggregates shopping lists from all active visitors.
     * Items from ignored visitors are excluded from the aggregated list.
     * @return Pair of (globalShoppingList, activeVisitors)
     */
    private fun prepareDrawingData(): Pair<MutableMap<NeuInternalName, Int>, MutableList<VisitorApi.Visitor>> {
        val globalShoppingList = mutableMapOf<NeuInternalName, Int>()
        val activeVisitors = mutableListOf<VisitorApi.Visitor>()

        for ((_, visitor) in VisitorApi.getVisitorsMap()) {
            if (visitor.status == VisitorApi.VisitorStatus.ACCEPTED ||
                visitor.status == VisitorApi.VisitorStatus.REFUSED
            ) continue

            activeVisitors.add(visitor)

            if (visitor.ignoreShoppingList) continue

            for ((internalName, amount) in visitor.shoppingList) {
                val old = globalShoppingList.getOrDefault(internalName, 0)
                globalShoppingList[internalName] = old + amount
            }
        }
        return globalShoppingList to activeVisitors
    }

    /**
     * Builds the shopping list section of the HUD.
     * Shows items needed with prices and sack counts.
     */
    private fun MutableList<Renderable>.drawShoppingList(
        shoppingList: MutableMap<NeuInternalName, Int>,
    ) {
        if (shoppingList.isEmpty()) return

        var totalPrice = 0.0
        addString("§7Visitor Shopping List:")

        for ((internalName, amount) in shoppingList) {
            val name = internalName.repoItemName
            val itemStack = internalName.getItemStack()

            val list = mutableListOf<Renderable>()
            list.addString(" §7- ")
            list.addItemStack(itemStack)

            list.add(
                Renderable.clickable(
                    "$name §ex${amount.addSeparators()}",
                    tips = internalName.createBuyTip(),
                    onLeftClick = {
                        if (!GardenApi.inGarden()) return@clickable
                        if (Minecraft.getInstance().screen is SignEditScreen) {
                            SignUtils.setTextIntoSign("$amount")
                        } else {
                            internalName.buy(amount)
                        }
                    },
                ),
            )

            if (config.showPrice) {
                val price = VisitorPriceCalculator.calculateItemPrice(internalName, amount)
                totalPrice += price
                val format = price.shortFormat()
                list.addString(" §7(§6$format§7)")
            }

            addSackData(internalName, amount, list)

            add(Renderable.horizontal(list))
        }

        if (totalPrice > 0) {
            val format = totalPrice.shortFormat()
            this[0] = Renderable.text("§7Visitor Shopping List: §7(§6$format§7)")
        }
    }

    /**
     * Adds sack count and craftability info to a shopping list item.
     */
    private fun addSackData(
        internalName: NeuInternalName,
        amount: Int,
        list: MutableList<Renderable>,
    ) {
        if (!config.showSackCount) return

        var amountInSacks = 0
        internalName.getAmountInSacksOrNull()?.let {
            amountInSacks = it
            val textColor = if (it >= amount) "a" else "e"
            list.addString(" §7(§${textColor}x${it.addSeparators()} §7in sacks)")
        }

        val ingredients = NeuItems.getRecipes(internalName)
            .firstOrNull { !it.ingredients.first().internalName.contains("PEST") }
            ?.ingredients.orEmpty()
        if (ingredients.isEmpty()) return

        val requiredIngredients = mutableMapOf<NeuInternalName, Int>()
        for ((key, count) in ingredients.toPrimitiveItemStacks()) {
            requiredIngredients.addOrPut(key, count)
        }

        var hasIngredients = true
        for ((key, value) in requiredIngredients) {
            val sackItem = key.getAmountInSacks()
            if (sackItem < value * (amount - amountInSacks)) {
                hasIngredients = false
                break
            }
        }

        if (hasIngredients && (amount - amountInSacks) > 0) {
            val leftToCraft = amount - amountInSacks
            list.addString(" §7(")
            val renderable = Renderable.clickable(
                "§aCraftable!",
                {
                    if (Minecraft.getInstance().screen is SignEditScreen) {
                        SignUtils.setTextIntoSign("$leftToCraft")
                    } else {
                        HypixelCommands.viewRecipe(internalName)
                    }
                },
                tips = listOf("Click to view recipe or paste craft amount into sign!"),
            ) { GardenApi.inGarden() }
            list.add(renderable)
            list.addString("§7)")
        }
    }

    /**
     * Draws a single new-visitor entry with item preview.
     * Click toggles [VisitorApi.Visitor.ignoreShoppingList].
     */
    private fun MutableList<Renderable>.drawNewVisitor(visitor: VisitorApi.Visitor) {
        val visitorName = visitor.visitorName

        val list = mutableListOf<Renderable>()
        list.addString(" §7- $visitorName")

        if (config.itemPreview) {
            val repoVisitor = GardenVisitorColorNames.visitorMap[visitorName.removeColor()]
            val items = repoVisitor?.needItems
            if (items == null) {
                ErrorManager.logErrorStateWithData(
                    "Visitor has no items in repository",
                    "needItems is empty",
                    "visitorName" to visitorName,
                    "cleanName" to visitorName.removeColor(),
                )
                logMissingRepoItems(visitorName)
                list.addString(" §7(§c?§7)")
                add(Renderable.horizontal(list))
                return
            }
            if (items.isEmpty()) {
                if (repoVisitor.unknownRewards) {
                    list.addString(" §7(§fUnknown§7)")
                } else {
                    list.addString(" §7(§fAny§7)")
                }
            } else {
                for (item in items) {
                    list.addItemStack(NeuInternalName.fromItemName(item).getItemStack())
                }
            }
        }

        add(Renderable.horizontal(list))
    }

    private fun MutableList<Renderable>.drawVisitor(visitor: VisitorApi.Visitor) {
        val list = mutableListOf<Renderable>()
        list.addString(" §7- ")
        list.add(visitor.toClickableName())
        add(Renderable.horizontal(list))
    }

    private fun VisitorApi.Visitor.toClickableName(): Renderable {
        val displayName = if (ignoreShoppingList) {
            "§7§m${visitorName.removeColor()}"
        } else {
            GardenVisitorColorNames.getColoredName(visitorName)
        }
        return Renderable.clickable(
            displayName,
            onLeftClick = {
                ignoreShoppingList = !ignoreShoppingList
                updateDisplay()
            },
            tips = listOf(
                if (ignoreShoppingList) "§eClick to include this visitor in the shopping list."
                else "§eClick to ignore this visitor in the shopping list.",
            ),
        )
    }

    private val visitorMissingItemsWarnTime: MutableMap<String, SimpleTimeMark> = mutableMapOf()

    private fun logMissingRepoItems(name: String) {
        if ((visitorMissingItemsWarnTime[name] ?: SimpleTimeMark.farPast()).passedSince() < 10.minutes) return
        visitorMissingItemsWarnTime[name] = SimpleTimeMark.now()
        val text = "Visitor '$name§7' has no items in repo!"
        ChatUtils.debug(text)
        visitorMissingItemsWarnTime.removeIf { it.value.passedSince() > 10.minutes }
    }

    @HandleEvent
    fun onGuiRenderTop() {
        if (!config.enabled) return

        if (InventoryUtils.inAnyInventory()) {
            InventoryGuiScaleCompat.withOriginalHudScale {
                renderDisplay()
            }
        } else {
            renderDisplay()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onScreenDrawn(event: ScreenDrawnEvent) {
        if (!config.enabled) return
        val gui = event.gui
        if (gui !is SignEditScreen) return

        renderDisplay()
    }

    private fun renderDisplay() {
        if (showGui() && shouldShowShoppingList()) {
            config.position.renderRenderables(display, posLabel = "Visitor Shopping List")
        }
    }

    private fun shouldShowShoppingList(): Boolean {
        if (VisitorApi.inInventory) return true
        if (BazaarApi.inBazaarInventory) return true

        val currentScreen = Minecraft.getInstance().screen ?: return true
        val isInOwnInventory = currentScreen is InventoryScreen
        if (isInOwnInventory) return true
        if (currentScreen is SignEditScreen &&
            (currentScreen.isBazaarSign() || currentScreen.isSupercraftAmountSetSign())
        ) return true

        return false
    }

    private fun hideExtraGuis() = GardenApi.hideExtraGuis() && !VisitorApi.inInventory

    private fun showGui(): Boolean {
        if (IslandType.HUB.isInIsland()) {
            if (config.inBazaarAlley && SkyBlockUtils.graphArea == "Bazaar Alley") {
                return true
            }
            if (config.inFarmingAreas && SkyBlockUtils.graphArea == "Farm") {
                return true
            }
        }
        if (config.inFarmingAreas && IslandType.THE_FARMING_ISLANDS.isInIsland()) return true
        if (hideExtraGuis()) return false
        return GardenApi.inGarden() && (GardenApi.onBarnPlot || !config.onlyWhenClose)
    }

    @HandleEvent
    fun onProfileJoin() {
        updateDisplay()
    }
}
