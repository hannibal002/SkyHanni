package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object FishingBaitDisplay {

    private val config get() = SkyHanniMod.feature.fishing.fishingBaitDisplay

    private const val BAIT_SLOT = 44
    private const val BAIT_HOTBAR_INDEX = 8
    private val baitCategories = setOf(ItemCategory.BAIT, ItemCategory.FISHING_BAIT)
    private val baitRemainingPattern by RepoPattern.pattern(
        "fishing.baitdisplay.remaining",
        "Bait Remaining: (?<amount>[\\d,]+)",
    )

    private var display = emptyList<Renderable>()
    private var bait: BaitDisplayEntry? = null

    private data class BaitDisplayEntry(
        val itemStack: ItemStack?,
        val displayName: String,
        val amount: Int?,
    )

    private val noBaitEntry = BaitDisplayEntry(
        itemStack = null,
        displayName = "§cNo Bait",
        amount = null,
    )

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        updateBaitFromInventory()
    }

    @HandleEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (event.slot != BAIT_SLOT) return
        bait = event.itemStack.getBaitDisplayEntry()
        updateDisplay()
    }

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!FishingApi.holdingRod) return
        if (bait == null) updateBaitFromInventory()
        if (display.isEmpty()) updateDisplay()
        if (display.isEmpty()) return

        config.position.renderRenderable(
            Renderable.horizontal(
                display,
                spacing = 1,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ),
            posLabel = "Fishing Bait Display",
        )
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.showIcon) {
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun updateBaitFromInventory() {
        bait = InventoryUtils.getItemsInOwnInventoryWithNull()
            ?.getOrNull(BAIT_HOTBAR_INDEX)
            ?.getBaitDisplayEntry()
            ?: noBaitEntry
        updateDisplay()
    }

    private fun drawDisplay() = buildList {
        val bait = bait ?: return@buildList

        if (config.showIcon.get()) {
            bait.itemStack?.let {
                addItemStack(it, scale = 1.0)
            }
        }
        bait.amount?.let {
            addString("§b${it.addSeparators()}x")
        }
        val namePrefix = if (isEmpty()) "" else " "
        addString("$namePrefix${bait.displayName}")
    }

    private fun ItemStack.getBaitDisplayEntry(): BaitDisplayEntry {
        val displayName = hoverName.formattedTextCompatLeadingWhiteLessResets()
        val cleanDisplayName = displayName.removeColor()
        val category = getItemCategoryOrNull()
        val amount = getBaitRemainingAmount()
        val isBait = category != null && category in baitCategories ||
            UtilsPatterns.baitPattern.matches(cleanDisplayName) ||
            amount != null
        val isNoBait = cleanDisplayName == "No Bait"
        if (!isBait && !isNoBait) return noBaitEntry

        val icon = copy().also { it.count = 1 }

        return BaitDisplayEntry(
            itemStack = icon,
            displayName = if (isNoBait) "§cNo Bait" else displayName,
            amount = amount,
        )
    }

    private fun ItemStack.getBaitRemainingAmount(): Int? {
        for (line in getLoreComponent()) {
            baitRemainingPattern.matchMatcher(line.string.removeColor()) {
                return group("amount").formatInt()
            }
        }
        return null
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
