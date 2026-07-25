package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.OwnInventoryArmorUpdateEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.OwnInventoryMenuUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.getItemOnCursor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object OwnInventoryData {

    private var itemAmounts = mapOf<NeuInternalName, Int>()
    private var dirty = false

    /**
     * REGEX-TEST: §aMoved §r§e10 Wheat§r§a from your Sacks to your inventory.
     */
    private val sackToInventoryChatPattern by RepoPattern.pattern(
        "data.owninventory.chat.movedsacktoinventory",
        "§aMoved §r§e\\d* (?<name>.*)§r§a from your Sacks to your inventory.",
    )

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true, onlyOnSkyblock = true)
    fun onItemPickupReceivePacket(event: PacketReceivedEvent) {
        val packet = event.packet
        when (packet) {
            is ClientboundTakeItemEntityPacket -> {
                dirty = true
            }
            is ClientboundContainerSetSlotPacket -> {
                dirty = true

                if (packet.containerId != 0) return

                val slot = packet.slot
                val item = packet.item

                DelayedRun.runNextTick {
                    val internalName = item.getInternalName()
                    when (slot) {
                        in 0..4 -> {} // crafting output+grid
                        in 5..8 -> { // armor
                            ChatUtils.debug("OwnInventoryArmorUpdateEvent: $slot - $item - $internalName")
                            OwnInventoryArmorUpdateEvent(item, slot).post()
                        }
                        in 9..43 -> { // normal items
                            ChatUtils.debug("OwnInventoryItemUpdateEvent: $slot - $item - $internalName")
                            OwnInventoryItemUpdateEvent(item, slot).post()
                        }
                        44 -> { // skyblock menu
                            ChatUtils.debug("OwnInventoryMenuUpdateEvent: $slot - $item - $internalName")
                            OwnInventoryMenuUpdateEvent(item).post()
                        }
                        45 -> {} // offhand
                    }
                }

            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onClickEntity(event: PacketSentEvent) {
        val packet = event.packet

        if (packet is ServerboundContainerClickPacket) {
            dirty = true
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (itemAmounts.isEmpty()) {
            itemAmounts = getCurrentItems()
        }

        if (!dirty) return
        dirty = false

        val map = getCurrentItems()
        for ((internalName, amount) in map) {
            calculateDifference(internalName, amount)
        }
        itemAmounts = map
    }

    private fun getCurrentItems(): MutableMap<NeuInternalName, Int> {
        val map = mutableMapOf<NeuInternalName, Int>()
        for (itemStack in InventoryUtils.getItemsInOwnInventory()) {
            val internalName = itemStack.getInternalNameOrNull() ?: continue
            map.addOrPut(internalName, itemStack.count)
        }
        for (name in InventoryUtils.getArmorInternalNames()) {
            map.addOrPut(name, 1)
        }
        return map
    }

    @HandleEvent
    fun onWorldChange() {
        itemAmounts = emptyMap()
    }

    private fun calculateDifference(internalName: NeuInternalName, newAmount: Int) {
        val oldAmount = itemAmounts[internalName] ?: 0

        val diff = newAmount - oldAmount
        if (diff > 0) {
            addItem(internalName, diff)
        }
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        val item = MinecraftCompat.localPlayerOrNull?.getItemOnCursor() ?: return
        val internalNameOrNull = item.getInternalNameOrNull() ?: return
        ignoreItem(500.milliseconds, internalNameOrNull)
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        ignoreItem(500.milliseconds) { true }

        val itemName = event.item?.hoverName.formattedTextCompatLeadingWhiteLessResets()
        checkAHMovements(itemName)
    }

    private fun checkAHMovements(itemName: String) {
        val inventoryName = InventoryUtils.openInventoryName()

        // cancel own auction
        if (inventoryName.let { it == "BIN Auction View" || it == "Auction View" }) {
            if (itemName == "§cCancel Auction") {
                val item = InventoryUtils.getItemAtSlotIndex(13)
                val internalName = item?.getInternalNameOrNull() ?: return
                ignoreItem(5.seconds, internalName)
            }
        }

        // bought item from bin ah
        if (inventoryName == "Confirm Purchase" && itemName == "§aConfirm") {
            val item = InventoryUtils.getItemAtSlotIndex(13)
            val internalName = item?.getInternalNameOrNull() ?: return
            ignoreItem(5.seconds, internalName)
        }

        // bought item from normal ah
        if (inventoryName == "Auction View" && itemName == "§6Collect Auction") {
            val item = InventoryUtils.getItemAtSlotIndex(13)
            val internalName = item?.getInternalNameOrNull() ?: return
            ignoreItem(5.seconds, internalName)
        }

        // collected all items in "own bins"
        if (inventoryName == "Your Bids" && itemName == "§aClaim All") {
            for (stack in InventoryUtils.getItemsInOpenChest().map { it.item }) {
                if (stack.getLore().any { it == "§7Status: §aSold!" || it == "7Status: §aEnded!" }) {
                    val internalName = stack.getInternalNameOrNull() ?: return
                    ignoreItem(5.seconds, internalName)
                }
            }
        }

        // items in anvil
        if (inventoryName == "Anvil") {
            for (stack in InventoryUtils.getItemsAtSlots(13, 29, 33)) {
                val internalName = stack.getInternalNameOrNull() ?: continue
                ignoreItem(5.seconds, internalName)
            }
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        sackToInventoryChatPattern.matchMatcher(event.message) {
            val name = group("name")
            ignoreItem(500.milliseconds) { it.repoItemName.contains(name) }
        }
    }

    fun ignoreItem(duration: Duration, internalName: NeuInternalName) {
        ignoreItem(duration) { it == internalName }
    }

    fun ignoreItem(duration: Duration, condition: (NeuInternalName) -> Boolean) {
        ignoredItemsUntil.add(IgnoredItem(condition, SimpleTimeMark.now() + duration))
    }

    private val ignoredItemsUntil = mutableListOf<IgnoredItem>()

    class IgnoredItem(val condition: (NeuInternalName) -> Boolean, val blockedUntil: SimpleTimeMark)

    private fun addItem(internalName: NeuInternalName, add: Int) {
        if (SkyBlockUtils.lastWorldSwitch.passedSince() < 3.seconds) return

        ignoredItemsUntil.removeIf { it.blockedUntil.isInPast() }
        if (ignoredItemsUntil.any { it.condition(internalName) }) {
            return
        }

        if (internalName.startsWith("MAP-")) return

        ItemAddInInventoryEvent(internalName, add).post()
    }
}
