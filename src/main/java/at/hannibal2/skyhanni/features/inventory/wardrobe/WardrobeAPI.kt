package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object WardrobeAPI {

    val storage get() = ProfileStorageData.profileSpecific?.wardrobe

    private val repoGroup = RepoPattern.group("inventory.wardrobe")
    private val inventoryPattern by repoGroup.pattern(
        "inventory.name",
        "Wardrobe \\((?<currentPage>\\d+)/\\d+\\)"
    )

    /**
     * REGEX-TEST: §7Slot 4: §aEquipped
     */
    private val equippedSlotPattern by repoGroup.pattern(
        "equippedslot",
        "§7Slot \\d+: §aEquipped"
    )

    private const val FIRST_SLOT = 36
    private const val FIRST_HELMET_SLOT = 0
    private const val FIRST_CHESTPLATE_SLOT = 9
    private const val FIRST_LEGGINGS_SLOT = 18
    private const val FIRST_BOOTS_SLOT = 27
    const val MAX_SLOT_PER_PAGE = 9
    const val MAX_PAGES = 2

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
    ) {
        private fun getData() = storage?.wardrobeData?.getOrPut(id) {
            WardrobeData(
                id,
                (1..4).associateWith { null }.toMutableMap(),
                locked = true,
                favorite = false,
            )
        }

        var helmet: ItemStack?
            get() = getData()?.armor?.get(1)
            set(value) {
                getData()?.armor?.set(1, value)
            }

        var chestplate: ItemStack?
            get() = getData()?.armor?.get(2)
            set(value) {
                getData()?.armor?.set(2, value)
            }

        var leggings: ItemStack?
            get() = getData()?.armor?.get(3)
            set(value) {
                getData()?.armor?.set(3, value)
            }

        var boots: ItemStack?
            get() = getData()?.armor?.get(4)
            set(value) {
                getData()?.armor?.set(4, value)
            }

        var locked: Boolean
            get() = getData()?.locked ?: true
            set(value) {
                getData()?.locked = value
            }

        var favorite: Boolean
            get() = getData()?.favorite ?: false
            set(value) {
                getData()?.favorite = value
            }

        val armor get() = (1..4).associateWith { getData()?.armor?.get(it) }.toSortedMap().values.toList()

        fun isEmpty(): Boolean = armor.all { it == null }

        fun isCurrentSlot() = getData()?.id == currentWardrobeSlot

        fun isInCurrentPage() = (currentPage == null && page == 1) || (page == currentPage)
    }


    var currentWardrobeSlot: Int?
        get() = storage?.currentWardrobeSlot
        set(value) {
            storage?.currentWardrobeSlot = value
        }

    var currentPage: Int? = null

    init {
        val list = mutableListOf<WardrobeSlot>()
        var id = 0

        for (page in 1..MAX_PAGES) {
            for (slot in 0 until MAX_SLOT_PER_PAGE) {
                val inventorySlot = FIRST_SLOT + slot
                val helmetSlot = FIRST_HELMET_SLOT + slot
                val chestplateSlot = FIRST_CHESTPLATE_SLOT + slot
                val leggingsSlot = FIRST_LEGGINGS_SLOT + slot
                val bootsSlot = FIRST_BOOTS_SLOT + slot
                list.add(WardrobeSlot(++id, page, inventorySlot, helmetSlot, chestplateSlot, leggingsSlot, bootsSlot))
            }
        }
        wardrobeSlots = list
    }

    private fun getWardrobeItem(itemStack: ItemStack?) =
        if (itemStack?.item == ItemStack(Blocks.stained_glass_pane).item || itemStack == null) null else itemStack

    private fun getWardrobeSlotFromId(id: Int?) = wardrobeSlots.find { it.id == id }

    fun inWardrobe() = inventoryPattern.matches(InventoryUtils.openInventoryName())

    fun createWardrobePriceLore(slot: WardrobeSlot) = buildList {
        if (slot.isEmpty()) return@buildList
        add("§aEstimated Armor Value:")
        var totalPrice = 0.0
        slot.armor.forEach {
            if (it != null) {
                val price = EstimatedItemValueCalculator.getTotalPrice(it)
                add("  §7- ${it.name}: §6${NumberUtil.format(price)}")
                totalPrice += price
            }
        }
        if (totalPrice != 0.0) add(" §aTotal Value: §6§l${NumberUtil.format(totalPrice)} coins")
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return

        inventoryPattern.matchMatcher(event.inventoryName) {
            currentPage = group("currentPage").formatInt()
        } ?: return
        if (currentPage == null) return

        val itemsList = event.inventoryItems

        val allGrayDye = wardrobeSlots.all {
            itemsList[it.inventorySlot]?.itemDamage == EnumDyeColor.GRAY.dyeDamage || !it.isInCurrentPage()
        }

        if (allGrayDye) {
            val allSlotsEmpty = wardrobeSlots.all {
                (getWardrobeItem(itemsList[it.helmetSlot]) == null && getWardrobeItem(itemsList[it.chestplateSlot]) == null &&
                    getWardrobeItem(itemsList[it.leggingsSlot]) == null && getWardrobeItem(itemsList[it.bootsSlot]) == null) ||
                    !it.isInCurrentPage()
            }
            if (allSlotsEmpty) {
                wardrobeSlots.filter { it.isInCurrentPage() }.forEach {
                    it.helmet = null
                    it.chestplate = null
                    it.leggings = null
                    it.boots = null
                }
            } else return
        }

        var foundCurrentSlot = false

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
        if (!foundCurrentSlot && getWardrobeSlotFromId(currentWardrobeSlot)?.page == currentPage) {
            currentWardrobeSlot = null
        }
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
            wardrobeSlots.forEach { slot ->
                val slotInfo = buildString {
                    append("Slot ${slot.id}")
                    if (slot.favorite) append(" - Favorite: true")
                }
                if (slot.locked) {
                    add("$slotInfo is locked")
                } else if (slot.isEmpty()) {
                    add("$slotInfo is empty")
                } else {
                    add(slotInfo)
                    slot.helmet?.let { add("   Helmet: ${it.name}") }
                    slot.chestplate?.let { add("   Chestplate: ${it.name}") }
                    slot.leggings?.let { add("   Leggings: ${it.name}") }
                    slot.boots?.let { add("   Boots: ${it.name}") }
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
