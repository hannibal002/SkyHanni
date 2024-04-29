package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

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
    var inCustomWardrobe = false

    class WardrobeSlot(
        val id: Int,
        val page: Int,
        val inventorySlot: Int,
        val helmetSlot: Int,
        val chestplateSlot: Int,
        val leggingsSlot: Int,
        val bootsSlot: Int,
    )

    private fun WardrobeSlot.getData() = storage?.wardrobeData?.getOrPut(id) {
        WardrobeData(
            id,
            (1..4).associateWith { null }.toMutableMap(),
            locked = false,
            favorite = false,
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

    fun WardrobeSlot.getArmor() = (1..4).associateWith { getData()?.armor?.get(it) }.toSortedMap().values.toList()

    fun WardrobeSlot.isEmpty(): Boolean = getArmor().all { it == null }

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

    fun WardrobeSlot.isCurrentSlot() = getData()?.id == currentWardrobeSlot

    fun WardrobeSlot.isInCurrentPage() = (currentPage == null && page == 1) || (page == currentPage)

    var currentWardrobeSlot: Int?
        get() = storage?.currentWardrobeSlot
        set(value) {
            storage?.currentWardrobeSlot = value
        }

    var currentPage: Int? = null

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

    private fun getWardrobeItem(itemStack: ItemStack?) =
        if (itemStack?.item == ItemStack(Blocks.stained_glass_pane).item) null else itemStack

    private fun getWardrobeSlotFromId(id: Int?) = wardrobeSlots.find { it.id == currentWardrobeSlot }

    fun inWardrobe() = inventoryPattern.matches(InventoryUtils.openInventoryName())

    fun createWardrobePriceLore(slot: WardrobeSlot) = buildList {
        if (slot.isEmpty()) return@buildList
        add("§aEstimated Armor Value:")
        val armor = slot.getArmor()
        var totalPrice = 0.0
        armor.forEach {
            if (it != null) {
                val price = EstimatedItemValueCalculator.calculate(it).first
                add("  §7- ${it.name}: §6${NumberUtil.format(price)}")
                totalPrice += price
            }
        }
        if (totalPrice != 0.0) add(" §aTotal Value: §6§l${NumberUtil.format(totalPrice)} coins")
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return

        var inWardrobe = false
        inventoryPattern.matchMatcher(event.inventoryName) {
            currentPage = group("currentPage").formatInt()
            inWardrobe = true
        }
        if (!inWardrobe) return
        if (currentPage == null) return
        var foundCurrentSlot = false

        val itemsList = event.inventoryItems
        for (slot in wardrobeSlots.filter { it.isInCurrentPage() }) {
            slot.helmet = getWardrobeItem(itemsList[slot.helmetSlot])
            slot.chestplate = getWardrobeItem(itemsList[slot.chestplateSlot])
            slot.leggings = getWardrobeItem(itemsList[slot.leggingsSlot])
            slot.boots = getWardrobeItem(itemsList[slot.bootsSlot])
            if (equippedSlotPattern.matches(itemsList[slot.inventorySlot]?.name)) {
                currentWardrobeSlot = slot.id
                foundCurrentSlot = true
            }
            slot.locked = (itemsList[slot.inventorySlot] == ItemStack(Items.dye, EnumDyeColor.RED.dyeDamage))
            if (slot.locked) wardrobeSlots.forEach { if (it.id > slot.id) it.locked = true }
        }
        if (!foundCurrentSlot && getWardrobeSlotFromId(currentWardrobeSlot)?.page == currentPage)
            currentWardrobeSlot = null
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            if (!inWardrobe()) currentPage = null
        }
    }

    @SubscribeEvent
    fun onDebugCollect(event: DebugDataCollectEvent) {
        event.title("Wardrobe")
        event.addIrrelevant {
            add("Current wardrobe slot: $currentWardrobeSlot")
            wardrobeSlots.forEach { slot ->
                if (slot.locked) {
                    add("Slot ${slot.id} is locked")
                } else {
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

    class WardrobeData(
        @Expose val id: Int,
        @Expose var armor: MutableMap<Int, ItemStack?>,
        @Expose var locked: Boolean,
        @Expose var favorite: Boolean,
    )
}
