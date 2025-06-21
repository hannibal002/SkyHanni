package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.seconds

enum class EquipmentSlot(val slot: Int, vararg val categories: ItemCategory) {
    NECKLACE(10, ItemCategory.NECKLACE),
    CLOAK(19, ItemCategory.CLOAK),
    BELT(28, ItemCategory.BELT),
    GLOVES(37, ItemCategory.GLOVES, ItemCategory.BRACELET),
}

@SkyHanniModule
object EquipmentApi {

    val inventory = InventoryDetector { it == "Your Equipment and Stats" }

    private val storage get() = ProfileStorageData.profileSpecific?.equipment

    private val equipment get() = if (RiftApi.inRift()) storage?.riftSlots else storage?.slots

    fun getEquipment(slot: EquipmentSlot): ItemStack? = equipment?.get(slot.ordinal)
    private fun setEquipment(slot: EquipmentSlot, itemStack: ItemStack?) = equipment?.set(slot.ordinal, itemStack)

    private val repoGroup = RepoPattern.group("data.equipment")

    /**
     * REGEX-TEST: §aYou equipped a §r§dSnowy Gillsplash Cloak§r§a!
     */
    private val chatEquipRegex by repoGroup.pattern(
        "chat.equip",
        "§aYou equipped a (?<item>.+)§r§a!",
    )

    private var lastClickedEquipment: Pair<ItemStack, EquipmentSlot>? = null
    private var lastClickedEquipmentTime = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdate(event: InventoryOpenEvent) {
        if (!inventory.isInside()) return
        EquipmentSlot.entries.forEach {
            handleInventoryItem(it, event.inventoryItems[it.slot])
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemClick(event: ItemClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val item = event.itemInHand ?: return
        val category = item.getItemCategoryOrNull() ?: return
        if (category !in ItemCategory.equipment) return
        val slot = EquipmentSlot.entries.find { category in it.categories } ?: return
        lastClickedEquipment = item to slot
        lastClickedEquipmentTime = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        chatEquipRegex.matchMatcher(event.message) {
            if (lastClickedEquipmentTime.passedSince() > 1.seconds) return@matchMatcher
            val chatItem = group("item").removeColor()
            val (item, slot) = lastClickedEquipment ?: return@matchMatcher
            if (item.cleanName() != chatItem) return@matchMatcher
            setEquipment(slot, item)
            lastClickedEquipment = null
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Equipment")
        event.addIrrelevant {
            val storage = storage ?: run {
                add("storage is null")
                return@addIrrelevant
            }
            add("Equipment:")
            storage.slots.forEach { item ->
                val name = item?.displayName
                add(" - $name")
            }
            add("Rift Equipment:")
            storage.riftSlots.forEach { item ->
                val name = item?.displayName
                add(" - $name")
            }
        }
    }

    private fun handleInventoryItem(slot: EquipmentSlot, itemStack: ItemStack?) {
        val item = if (itemStack.isNotEmpty() && !itemStack.isStainedGlassPane()) itemStack else null
        setEquipment(slot, item)
    }

    fun getEmptyEquipment(): MutableList<ItemStack?> = EquipmentSlot.entries.mapTo(mutableListOf()) { null }
}
