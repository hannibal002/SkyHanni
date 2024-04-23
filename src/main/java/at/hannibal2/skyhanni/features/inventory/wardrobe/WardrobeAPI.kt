package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object WardrobeAPI {

    val storage get() = ProfileStorageData.profileSpecific?.wardrobe


    private val group = RepoPattern.group("inventory.wardrobe")
    private val inventoryPattern by group.pattern(
        "inventory.name",
        "Wardrobe \\((?<currentPage>\\d+)/\\d+\\)"
    )

    /**
     * REGEX-TEST: §7Slot 4: §aEquipped
     */
    private val equippedSlotPattern by group.pattern(
        "equippedslot",
        "§7Slot \\d+: §aEquipped"
    )

    var wardrobeSlots = listOf<WardrobeSlot>()

    class WardrobeSlot(
        val id: Int,
        val page: Int,
        val inventorySlot: Int,
        val helmetSlot: Int,
        val chestplateSlot: Int,
        val leggingsSlot: Int,
        val bootsSlot: Int,
    )

    class WardrobeData(
        @Expose
        val id: Int,

        @Expose
        var armor: MutableMap<Int, ItemStack?>,

        @Expose
        var locked: Boolean,

        @Expose
        var favorite: Boolean,
    )

    private fun WardrobeSlot.getData() = storage?.wardrobeData?.getOrPut(id) {
        WardrobeData(
            id,
            mutableMapOf(1 to null, 2 to null, 3 to null, 4 to null),
            false,
            false,
        )
    }

    var WardrobeSlot.helmet: ItemStack?
        get() = getData()?.armor?.get(1)
        set(value) {
            getData()?.armor?.set(1, value)
        }

    var WardrobeSlot.chestplate: ItemStack?
        get() = getData()?.armor?.get(2)
        set(value) {
            getData()?.armor?.set(2, value)
        }

    var WardrobeSlot.leggings: ItemStack?
        get() = getData()?.armor?.get(3)
        set(value) {
            getData()?.armor?.set(3, value)
        }

    var WardrobeSlot.boots: ItemStack?
        get() = getData()?.armor?.get(4)
        set(value) {
            getData()?.armor?.set(4, value)
        }

    val WardrobeSlot.armor: List<ItemStack?>
        get() = listOf(boots, leggings, chestplate, helmet)

    var WardrobeSlot.locked: Boolean
        get() = getData()?.locked ?: true
        set(value) {
            getData()?.locked = value
        }

    var WardrobeSlot.favorite: Boolean
        get() = getData()?.favorite ?: false
        set(value) {
            getData()?.favorite = value
        }

    val WardrobeSlot.isCurrentSlot: Boolean
        get() = getData()?.id == currentWardrobeSlot

    val WardrobeSlot.isInCurrentPage: Boolean
        get() = page == getWardrobePage()

    var currentWardrobeSlot: Int?
        get() = storage?.currentWardrobeSlot
        set(value) {
            storage?.currentWardrobeSlot = value
        }

    init {
        val list = mutableListOf<WardrobeSlot>()
        val slotsPerPage = 9
        val maxPages = 2

        val firstSlot = 36
        val firstHelmetSlot = 0
        val firstChestplateSlot = 9
        val firstLeggingsSlot = 18
        val firstBootsSlot = 27

        var id = 0

        for (page in 1..maxPages) {
            for (slot in 0 until slotsPerPage) {
                val inventorySlot = firstSlot + slot
                val helmetSlot = firstHelmetSlot + slot
                val chestplateSlot = firstChestplateSlot + slot
                val leggingsSlot = firstLeggingsSlot + slot
                val bootsSlot = firstBootsSlot + slot
                list.add(WardrobeSlot(id, page, inventorySlot, helmetSlot, chestplateSlot, leggingsSlot, bootsSlot))
                id++
            }
        }
        wardrobeSlots = list
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inWardrobe()) return
        val page = getWardrobePage() ?: return

        for (slot in wardrobeSlots.filter { it.page == page }) {
            slot.helmet = getWardrobeItem(event.inventoryItems[slot.helmetSlot])
            slot.chestplate = getWardrobeItem(event.inventoryItems[slot.chestplateSlot])
            slot.leggings = getWardrobeItem(event.inventoryItems[slot.leggingsSlot])
            slot.boots = getWardrobeItem(event.inventoryItems[slot.bootsSlot])
            if (equippedSlotPattern.matches(event.inventoryItems[slot.inventorySlot]?.name)) {
                currentWardrobeSlot = slot.id
            }
        }
    }

    private fun getWardrobeItem(itemStack: ItemStack?) =
        if (itemStack?.item == ItemStack(Blocks.stained_glass_pane).item) null else itemStack

    fun inWardrobe() = inventoryPattern.matches(InventoryUtils.openInventoryName())

    fun getWardrobePage(): Int? {
        inventoryPattern.matchMatcher(InventoryUtils.openInventoryName()) {
            return group("currentPage").formatInt()
        }
        return null
    }

    @SubscribeEvent
    fun onDebugCollect(event: DebugDataCollectEvent) {
        event.title("Wardrobe")
        event.addIrrelevant {
            add("Current wardrobe slot: $currentWardrobeSlot")
            wardrobeSlots.forEach { slot ->
                add(
                    "Slot ${slot.id} - " +
                        "Helmet: ${slot.helmet?.name} - " +
                        "Chestplate: ${slot.chestplate?.name} - " +
                        "Leggings: ${slot.leggings?.name} - " +
                        "Boots: ${slot.boots?.name}"
                )
            }
        }
    }
}
