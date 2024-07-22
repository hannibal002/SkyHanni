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
import at.hannibal2.skyhanni.utils.ItemNameResolver
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
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
        ICON("§e✎"),
        ITEM_NAME("§d[Cute] Skirtwearer's Cake Soul"),
        CHANGE_AMOUNT("§a+256"),
        ;

        override fun toString() = display
    }

    private val config get() = SkyHanniMod.feature.inventory.itemPickupLogConfig
    private val coinIcon = "COIN_TALISMAN".asInternalName()

    private var itemList = mutableMapOf<Int, Pair<ItemStack, Int>>()
    private var itemsAddedToInventory = mutableMapOf<Int, PickupEntry>()
    private var itemsRemovedFromInventory = mutableMapOf<Int, PickupEntry>()
    private var display: Renderable = Renderable.string("")

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
        config.pos.renderRenderable(display, posLabel = "Item Pickup Log Display")
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
            val item = PickupEntry(itemStack.displayName, it.delta.absoluteValue.toLong(), it.internalName)

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

        oldItemList.forEach {
            if (!itemList.containsKey(it.key)) {
                val item = PickupEntry(it.value.first.displayName, it.value.second.toLong(), it.value.first.getInternalNameOrNull())
                updateItem(it.key, item, it.value.first, true)
            } else if (it.value.second > itemList[it.key]!!.second) {
                val amount = (it.value.second - itemList[it.key]?.second!!)
                val item = PickupEntry(it.value.first.displayName, amount.toLong(), it.value.first.getInternalNameOrNull())
                updateItem(it.key, item, it.value.first, true)
            }
        }

        itemList.forEach {
            if (!oldItemList.containsKey(it.key)) {
                val item = PickupEntry(it.value.first.displayName, it.value.second.toLong(), it.value.first.getInternalNameOrNull())
                updateItem(it.key, item, it.value.first, false)
            } else if (it.value.second > oldItemList[it.key]!!.second) {
                val amount = (it.value.second - oldItemList[it.key]?.second!!)
                val item = PickupEntry(it.value.first.displayName, amount.toLong(), it.value.first.getInternalNameOrNull())
                updateItem(it.key, item, it.value.first, false)
            }
        }
        updateDisplay()
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
        if (removed) {
            itemsAddedToInventory[hash]?.let { added ->
                added.timeUntilExpiry = SimpleTimeMark.now()
            }
            itemsRemovedFromInventory[hash]?.let {
                it.updateAmount(itemInfo.amount)
                return
            }
            itemsRemovedFromInventory[hash] = itemInfo
        } else {
            itemsRemovedFromInventory[hash]?.let { added ->
                added.timeUntilExpiry = SimpleTimeMark.now()
            }
            itemsAddedToInventory[hash]?.let {
                it.updateAmount(itemInfo.amount)
                return
            }
            itemsAddedToInventory[hash] = itemInfo
        }
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

    private fun renderList(prefix: String, amount: Long, name: String, itemIcon: NEUInternalName?) = Renderable.horizontalContainer(
        buildList {
            for (item in config.displayLayout) {
                when (item) {
                    DisplayLayout.ICON -> {
                        if (itemIcon != null) {
                            addItemStack(itemIcon)
                        } else {
                            ItemNameResolver.getInternalNameOrNull(name)?.let { addItemStack(it) }
                        }
                    }

                    DisplayLayout.CHANGE_AMOUNT -> {
                        val formattedAmount = if (config.shorten) amount.shortFormat() else amount.addSeparators()
                        add(Renderable.string("${prefix}${formattedAmount}"))
                    }

                    DisplayLayout.ITEM_NAME -> {
                        add(Renderable.string(name))
                    }

                    null -> {}
                }
            }
        },
    )


    private fun worldChangeCooldown(): Boolean {
        return LorenzUtils.lastWorldSwitch.passedSince() > 2.seconds
    }

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

                        if (currentTotalValue > 0) {
                            display.add(renderList("§a+", currentTotalValue, item.value.name, item.value.neuInternalName))
                        } else if (currentTotalValue < 0) {
                            display.add(renderList("§c", currentTotalValue, item.value.name, item.value.neuInternalName))
                        } else {
                            itemsAddedToInventory.remove(item.key)
                            itemsRemovedFromInventory.remove(item.key)
                        }
                        removedItemsToNoLongerShow.remove(item.key)
                        iterator.remove()
                    }
                } else {
                    display.add(renderList("§a+", item.value.amount, item.value.name, item.value.neuInternalName))
                }
            }
        } else {
            for (item in addedItemsToNoLongerShow) {

                display.add(renderList("§a+", item.value.amount, item.value.name, item.value.neuInternalName))
                removedItemsToNoLongerShow[item.key]?.let {
                    display.add(renderList("§c-", it.amount, it.name, it.neuInternalName))
                    removedItemsToNoLongerShow.remove(item.key)
                }
            }
        }
        for (item in removedItemsToNoLongerShow) {
            display.add(renderList("§c-", item.value.amount, item.value.name, item.value.neuInternalName))
        }
        val renderable = Renderable.verticalContainer(display, verticalAlign = config.alignment)

        this.display = Renderable.fixedSizeBox(renderable, 30, 75)
    }
}
