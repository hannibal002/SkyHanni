package at.hannibal2.skyhanni.features.event.yearofthewitch

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchableScrollable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@SkyHanniModule
object StewHelper {

    /**
     * REGEX-TEST: Warmed Flakes x64
     */
    private val stewItemNamePattern by RepoPattern.pattern(
        "event.year-of-the-witch.stew-item",
        "(?<item>.*) x(?<amount>\\d+)",
    )

    private val inventoryDetector = InventoryDetector(
        checkInventoryName = { it == "Witches Stew" },
        // TODO use InventoryUpdatedEvent, either in this file, or in InventoryDetector
        onOpenInventory = { DelayedRun.runNextTick { checkSlots() } },
    )

    private var display = emptyList<Renderable>()
    private val textInput = SearchTextInput()
    private val scrollValue = ScrollValue()
    private val config get() = SkyHanniMod.feature.event.yearOfTheWitch

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onBackgroundDrawnEvent(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.stewHighlighter) return
        if (!inventoryDetector.isInside()) return
        for (slot in event.container.slots) {
            val stack = slot.item
            if (stack.item != Items.PLAYER_HEAD) continue
            val status = getStewStatus(stack) ?: continue
            status.color?.let { slot.highlight(it) }
        }
    }

    private fun checkSlots() {
        if (!config.stewHelper) return
        val items = InventoryUtils.getItemsInOpenChest().map { it.item }
        val requiredItems = mutableMapOf<NeuInternalName, Int>()
        for (stack in items) {
            if (stack.item != Items.PLAYER_HEAD) continue
            if (getStewStatus(stack) == StewStatus.HAS_EATEN) continue
            val ingredientLine = stack.getLoreComponent().map { it.string }.nextAfter("Requires:") ?: continue
            stewItemNamePattern.matchMatcher(ingredientLine) {
                val id = NeuInternalName.fromItemNameOrNull(group("item")) ?: return@matchMatcher
                requiredItems[id] = group("amount").toInt()
            }
        }
        constructDisplay(requiredItems)
    }

    private fun constructDisplay(items: Map<NeuInternalName, Int>) {
        val displayLines = items.map { createRenderableLine(it.key, it.value) }

        display = buildList {
            addString("§dStew Helper")
            if (displayLines.isEmpty()) {
                addString("§a§lYou have eaten all the stews!")
            } else {
                add(displayLines.buildSearchableScrollable(height = 225, textInput, velocity = 2.0, scrollValue = scrollValue))
            }
        }
    }

    private fun createRenderableLine(internalName: NeuInternalName, amountNeeded: Int): Searchable {
        val itemStack = internalName.getItemStack()
        val stackRenderable = Renderable.item(itemStack)
        val itemName = internalName.repoItemName
        val tooltip = buildList {
            add(itemStack.hoverName)
            addAll(itemStack.getLoreComponent())
        }
        val amountInSacks = internalName.getAmountInSacks()

        val color = if (amountInSacks >= amountNeeded) "§a" else "§e"

        val nameRenderable = Renderable.hoverTips(
            content = Renderable.text(" §7- §a${itemName.removeColor()} $color$amountInSacks/$amountNeeded"),
            tips = tooltip,
        )

        val container = Renderable.horizontal(stackRenderable, nameRenderable).toSearchable(itemName.removeColor())
        return container
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnIsland = IslandType.HUB)
    fun onRenderOverlay() {
        if (!config.stewHelper) return
        if (display.isEmpty()) return
        config.stewHelperPosition.renderRenderables(display, posLabel = "Witch Stew Helper")
    }

    @HandleEvent
    fun onInventoryClose() {
        display = emptyList()
    }

    // TODO repo patterns for the two lastLine
    private fun getStewStatus(stack: ItemStack): StewStatus? {
        val lastLine = stack.getLoreComponent().lastOrNull()?.string ?: return null
        if (lastLine == "You've already eaten this stew!") return StewStatus.HAS_EATEN
        if (lastLine == "Click to give ingredients!") return StewStatus.HAS_ENOUGH
        return StewStatus.MISSING_INGREDIENTS
    }

    private enum class StewStatus(val color: LorenzColor?) {
        MISSING_INGREDIENTS(null),
        HAS_ENOUGH(LorenzColor.YELLOW),
        HAS_EATEN(LorenzColor.GREEN)
    }
}
