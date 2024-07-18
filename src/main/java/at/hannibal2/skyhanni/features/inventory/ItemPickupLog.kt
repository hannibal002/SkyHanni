package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemNameResolver
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
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
    private val config get() = SkyHanniMod.feature.inventory.itemPickupLogConfig

    private var itemList = mutableMapOf<Int, Pair<ItemStack, Int>>()
    private var itemsAddedToInventory = mutableMapOf<Int, UpdatedItem>()
    private var itemsRemovedFromInventory = mutableMapOf<Int, UpdatedItem>()

    private val patternGroup = RepoPattern.group("itempickuplog")
    private val shopPattern by patternGroup.pattern(
        "shoppattern",
        "^(?<itemName>.+?)(?: x\\d+)?\$",
    )

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val newDisplay = mutableListOf<Renderable>()

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
                            newDisplay.add(renderList("§a+", currentTotalValue, item.value.name))
                        } else if (currentTotalValue < 0) {
                            newDisplay.add(renderList("§c", currentTotalValue, item.value.name))
                        } else {
                            itemsAddedToInventory.remove(item.key)
                            itemsRemovedFromInventory.remove(item.key)
                        }
                        removedItemsToNoLongerShow.remove(item.key)
                        iterator.remove()
                    }
                } else {
                    newDisplay.add(renderList("§a+", item.value.amount, item.value.name))
                }
            }
        } else {
            for (item in addedItemsToNoLongerShow) {

                newDisplay.add(renderList("§a+", item.value.amount, item.value.name))
                removedItemsToNoLongerShow[item.key]?.let {
                    newDisplay.add(renderList("§c-", it.amount, it.name))
                    removedItemsToNoLongerShow.remove(item.key)
                }
            }
        }
        for (item in removedItemsToNoLongerShow) {
            newDisplay.add(renderList("§c-", item.value.amount, item.value.name))
        }
        config.pos.renderRenderables(newDisplay, posLabel = "Item Pickup Log Display")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        itemList.clear()
        itemsAddedToInventory.clear()
        itemsRemovedFromInventory.clear()
    }

    @SubscribeEvent
    fun onStackChange(event: SackChangeEvent) {
        if (config.sack) {
            event.sackChanges.forEach {
                val itemStack = (it.internalName.getItemStack())
                val item = UpdatedItem(itemStack.displayName, it.delta.absoluteValue)

                updateItem(itemStack.hash(), item, itemStack, it.delta < 0)
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val oldItemList = mutableMapOf<Int, Pair<ItemStack, Int>>()

        itemList.forEach {
            oldItemList[it.key] = it.value
        }

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

        if (LorenzUtils.lastWorldSwitch.passedSince() < 2.seconds) return

        oldItemList.forEach {
            if (!itemList.containsKey(it.key)) {
                val item = UpdatedItem(it.value.first.displayName, it.value.second)
                updateItem(it.key, item, it.value.first, true)
            } else if (it.value.second > itemList[it.key]!!.second) {
                val amount = (it.value.second - itemList[it.key]?.second!!)
                val item = UpdatedItem(it.value.first.displayName, amount)
                updateItem(it.key, item, it.value.first, true)
            }
        }

        itemList.forEach {
            if (!oldItemList.containsKey(it.key)) {
                val item = UpdatedItem(it.value.first.displayName, it.value.second)
                updateItem(it.key, item, it.value.first, false)
            } else if (it.value.second > oldItemList[it.key]!!.second) {
                val amount = (it.value.second - oldItemList[it.key]?.second!!)
                val item = UpdatedItem(it.value.first.displayName, amount)
                updateItem(it.key, item, it.value.first, false)
            }
        }
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

    //TODO convert to repo patterns once that pr is merged
    private val bannedItems = setOf(
        "SKYBLOCK_MENU".asInternalName(),
        "CANCEL_PARKOUR_ITEM".asInternalName(),
        "CANCEL_RACE_ITEM".asInternalName(),
    )

    private fun isBannedItem(item: ItemStack): Boolean {
        if (item.getInternalNameOrNull()?.startsWith("MAP") == true) {
            return true
        }
        if (bannedItems.contains(item.getInternalNameOrNull())) {
            return true
        }
        if (item.getExtraAttributes()?.hasKey("quiver_arrow") == true) {
            return true
        }
        return false
    }

    private fun updateItem(hash: Int, itemInfo: UpdatedItem, item: ItemStack, removed: Boolean) {
        if (removed) {
            itemsAddedToInventory[hash]?.let { added ->
                added.time = SimpleTimeMark.now()
            }
            itemsRemovedFromInventory[hash]?.let {
                it.updateAmount(itemInfo.amount)
                return
            }
            if (isBannedItem(item)) return
            itemsRemovedFromInventory[hash] = itemInfo
        } else {
            itemsRemovedFromInventory[hash]?.let { added ->
                added.time = SimpleTimeMark.now()
            }
            itemsAddedToInventory[hash]?.let {
                it.updateAmount(itemInfo.amount)
                return
            }
            if (isBannedItem(item)) return
            itemsAddedToInventory[hash] = itemInfo
        }
    }

    private data class UpdatedItem(val name: String, var amount: Int) {
        var time = SimpleTimeMark.now()

        fun updateAmount(change: Int) {
            amount += change
            time = SimpleTimeMark.now()
        }

        fun isExpired() = time.passedSince() > config.expireAfter.seconds
    }

    private fun renderList(prefix: String, amount: Int, name: String): Renderable {
        return if (config.showItemIcon) {
            Renderable.horizontalContainer(
                buildList {
                    add(Renderable.string("${prefix}${amount.addSeparators()}"))
                    ItemNameResolver.getInternalNameOrNull(name)?.let {
                        addItemStack(
                            it,
                        )
                    }
                    add(Renderable.string(name))
                },
            )
        } else {
            Renderable.string("$prefix $amount $name")
        }
    }
}
