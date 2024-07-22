package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemNameResolver
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Objects
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ItemPickupLog {

    enum class DisplayLayout(private val display: String) {
        CHANGE_AMOUNT("§a+256"),
        ICON("§e✎"),
        ITEM_NAME("§d[Cute] Skirtwearer's Cake Soul"),
        ;

        override fun toString() = display
    }

    private val config get() = SkyHanniMod.feature.inventory.itemPickupLogConfig
    private val coinIcon = "COIN_TALISMAN".asInternalName()

    private var itemList = mutableMapOf<Int, Pair<ItemStack, Int>>()
    private var itemsAddedToInventory = mutableMapOf<Int, PickupEntry>()
    private var itemsRemovedFromInventory = mutableMapOf<Int, PickupEntry>()
    private var display: Renderable? = null

    private val patternGroup = RepoPattern.group("itempickuplog")
    private val shopPattern by patternGroup.pattern(
        "shoppattern",
        "^(?<itemName>.+?)(?: x\\d+)?\$",
    )

    private val bannedItemsPattern by patternGroup.list(
        "banneditems",
        "SKYBLOCK_MENU",
        "CANCEL_PARKOUR_ITEM",
        "CANCEL_RACE_ITEM",
    )
    private val bannedItemsConverted = bannedItemsPattern.map { it.toString().asInternalName() }


    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        display?.let { config.pos.renderRenderable(it, posLabel = "Item Pickup Log Display") }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!isEnabled()) return
        itemList.clear()
        itemsAddedToInventory.clear()
        itemsRemovedFromInventory.clear()
    }

    @SubscribeEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled() || !config.sack) return

        event.sackChanges.forEach {
            val itemStack = (it.internalName.getItemStack())
            val item = PickupEntry(itemStack.dynamicName(), it.delta.absoluteValue.toLong(), it.internalName)

            updateItem(itemStack.hash(), item, itemStack, it.delta < 0)
        }
    }

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled() || !config.coins || !worldChangeCooldown()) return

        updateItem(0, PickupEntry("§6Coins", event.coins.absoluteValue.toLong(), coinIcon), coinIcon.getItemStack(), event.coins < 0)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        val oldItemList = mutableMapOf<Int, Pair<ItemStack, Int>>()


        oldItemList.putAll(itemList)

        itemList.clear()

        val inventoryItems = InventoryUtils.getItemsInOwnInventory().toMutableList()
        val cursorItem = Minecraft.getMinecraft().thePlayer.inventory?.itemStack

        if (cursorItem != null) {
            val hash = cursorItem.hash()
            //this prevents items inside hypixel guis counting when picked up
            if (oldItemList.contains(hash)) {
                inventoryItems.add(cursorItem)
            }
        }

        for (itemStack in inventoryItems) {
            val hash = itemStack.hash()
            val old = itemList[hash]
            if (old != null) {
                itemList[hash] = old.copy(second = old.second + itemStack.stackSize)
            } else {
                itemList[hash] = itemStack to itemStack.stackSize
            }
        }

        if (!worldChangeCooldown()) return

        for ((key, value) in oldItemList) {
            val stack = value.first
            val oldAmount = value.second
            if (!itemList.containsKey(key)) {
                val item = PickupEntry(stack.dynamicName(), oldAmount.toLong(), stack.getInternalNameOrNull())
                updateItem(key, item, stack, true)
            } else if (oldAmount > itemList[key]!!.second) {
                val amount = (oldAmount - itemList[key]?.second!!)
                val item = PickupEntry(stack.dynamicName(), amount.toLong(), stack.getInternalNameOrNull())
                updateItem(key, item, stack, true)
            }
        }

        for ((key, value) in itemList) {
            val stack = value.first
            val oldAmount = value.second
            if (!oldItemList.containsKey(key)) {
                val item = PickupEntry(stack.dynamicName(), oldAmount.toLong(), stack.getInternalNameOrNull())
                updateItem(key, item, stack, false)
            } else if (oldAmount > oldItemList[key]!!.second) {
                val amount = (oldAmount - oldItemList[key]?.second!!)
                val item = PickupEntry(stack.dynamicName(), amount.toLong(), stack.getInternalNameOrNull())
                updateItem(key, item, stack, false)
            }
        }
        updateDisplay()
    }

    private fun ItemStack.dynamicName(): String {
        val compact = when (getItemCategoryOrNull()) {
            ItemCategory.ENCHANTED_BOOK -> true
            ItemCategory.PET -> true
            else -> false
        }
        return if (compact) getInternalName().itemName else displayName
    }


    private fun ItemStack.hash(): Int {
        var displayName = this.displayName.removeColor()
        val matcher = shopPattern.matcher(displayName)
        if (matcher.matches()) {
            displayName = matcher.group("itemName")
        }
        return Objects.hash(
            this.getInternalNameOrNull(),
            displayName.removeColor(),
            this.getItemRarityOrNull(),
        )
    }

    private fun isBannedItem(item: ItemStack): Boolean {
        if (item.getInternalNameOrNull()?.startsWith("MAP") == true) {
            return true
        }

        if (bannedItemsConverted.contains(item.getInternalNameOrNull())) {
            return true
        }

        if (item.getExtraAttributes()?.hasKey("quiver_arrow") == true) {
            return true
        }
        return false
    }

    private fun updateItem(hash: Int, itemInfo: PickupEntry, item: ItemStack, removed: Boolean) {
        if (isBannedItem(item)) return

        val targetInventory = if (removed) itemsRemovedFromInventory else itemsAddedToInventory
        val oppositeInventory = if (removed) itemsAddedToInventory else itemsRemovedFromInventory

        oppositeInventory[hash]?.let { existingItem ->
            existingItem.timeUntilExpiry = SimpleTimeMark.now()
        }

        targetInventory[hash]?.let { existingItem ->
            existingItem.updateAmount(itemInfo.amount)
            return
        }

        targetInventory[hash] = itemInfo
    }

    private data class PickupEntry(val name: String, var amount: Long, val neuInternalName: NEUInternalName?) {
        var timeUntilExpiry = SimpleTimeMark.now()

        fun updateAmount(change: Long) {
            amount += change
            timeUntilExpiry = SimpleTimeMark.now()
        }

        fun isExpired() = timeUntilExpiry.passedSince() > config.expireAfter.seconds
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    private fun renderList(prefix: String, entry: PickupEntry) = Renderable.horizontalContainer(
        buildList {
            val displayLayout: List<DisplayLayout> = config.displayLayout
            for (item in displayLayout) {
                when (item) {
                    DisplayLayout.ICON -> {
                        val itemIcon = entry.neuInternalName?.getItemStack()
                        if (itemIcon != null) {
                            addItemStack(itemIcon)
                        } else {
                            ItemNameResolver.getInternalNameOrNull(entry.name)?.let { addItemStack(it) }
                        }
                    }

                    DisplayLayout.CHANGE_AMOUNT -> {
                        val formattedAmount = if (config.shorten) entry.amount.shortFormat() else entry.amount.addSeparators()
                        add(Renderable.string("${prefix}${formattedAmount}"))
                    }

                    DisplayLayout.ITEM_NAME -> {
                        add(Renderable.string(entry.name))
                    }
                }
            }
        },
    )


    private fun worldChangeCooldown(): Boolean = LorenzUtils.lastWorldSwitch.passedSince() > 2.seconds

    private fun updateDisplay() {
        if (!isEnabled()) return

        val display = mutableListOf<Renderable>()

        itemsAddedToInventory.values.removeIf { it.isExpired() }
        itemsRemovedFromInventory.values.removeIf { it.isExpired() }

        val removedItemsToNoLongerShow = itemsRemovedFromInventory.toMutableMap()
        val addedItemsToNoLongerShow = itemsAddedToInventory.toMutableMap()

        if (config.compactLines) {
            val iterator = addedItemsToNoLongerShow.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()

                if (removedItemsToNoLongerShow.containsKey(item.key)) {
                    val it = removedItemsToNoLongerShow[item.key]
                    if (it != null) {
                        val currentTotalValue = item.value.amount - it.amount
                        val entry = PickupEntry(item.value.name, currentTotalValue, item.value.neuInternalName)

                        if (currentTotalValue > 0) {
                            display.add(renderList("§a+", entry))
                        } else if (currentTotalValue < 0) {
                            display.add(renderList("§c", entry))
                        } else {
                            itemsAddedToInventory.remove(item.key)
                            itemsRemovedFromInventory.remove(item.key)
                        }
                        removedItemsToNoLongerShow.remove(item.key)
                        iterator.remove()
                    }
                } else {
                    display.add(renderList("§a+", item.value))
                }
            }
        } else {
            for (item in addedItemsToNoLongerShow) {

                display.add(renderList("§a+", item.value))
                removedItemsToNoLongerShow[item.key]?.let {
                    display.add(renderList("§c-", it))
                    removedItemsToNoLongerShow.remove(item.key)
                }
            }
        }
        for (item in removedItemsToNoLongerShow) {
            display.add(renderList("§c-", item.value))
        }
        if (display.isEmpty()) {
            this.display = null
            return
        }
        val renderable = Renderable.verticalContainer(display, verticalAlign = config.alignment)

        this.display = Renderable.fixedSizeColumn(renderable, 30)
    }
}
