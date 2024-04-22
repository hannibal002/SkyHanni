package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.Quad
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
        var armor: Quad<ItemStack?, ItemStack?, ItemStack?, ItemStack?>,

        @Expose
        var locked: Boolean,

        @Expose
        var favorite: Boolean,
    )

    private fun WardrobeSlot.getData() = storage?.wardrobeData?.getOrPut(id) {
        WardrobeData(
            id,
            Quad(null, null, null, null),
            true,
            false,
        )
    }

    var WardrobeSlot.helmet: ItemStack?
        get() = getData()?.armor?.first
        set(value) {
            val data = getData()?.armor ?: return
            getData()?.armor = Quad(value, data.second, data.third, data.fourth)
        }

    var WardrobeSlot.chestplate: ItemStack?
        get() = getData()?.armor?.second
        set(value) {
            val data = getData()?.armor ?: return
            getData()?.armor = Quad(data.first, value, data.third, data.fourth)
        }

    var WardrobeSlot.leggings: ItemStack?
        get() = getData()?.armor?.third
        set(value) {
            val data = getData()?.armor ?: return
            getData()?.armor = Quad(data.first, data.second, value, data.fourth)
        }

    var WardrobeSlot.boots: ItemStack?
        get() = getData()?.armor?.fourth
        set(value) {
            val data = getData()?.armor ?: return
            getData()?.armor = Quad(data.first, data.second, data.third, value)
        }

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

    private fun getWardrobeItem(itemStack: ItemStack?): ItemStack? {
        println(itemStack?.name)
        return if (itemStack == ItemStack(Blocks.stained_glass_pane)) null else itemStack
    }

    fun inWardrobe() = inventoryPattern.matches(InventoryUtils.openInventoryName())

    fun getWardrobePage(): Int? {
        inventoryPattern.matchMatcher(InventoryUtils.openInventoryName()) {
            return group("currentPage").formatInt()
        }
        return null
    }
}
