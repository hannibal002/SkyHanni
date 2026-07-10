package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.inventory.WardrobeUpdateEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import at.hannibal2.skyhanni.utils.compat.DyeCompat.Companion.isDye
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object WardrobeApi {

    val storage get() = ProfileStorageData.profileSpecific?.wardrobe

    private val patternGroup = RepoPattern.group("inventory.wardrobe")

    /**
     * REGEX-TEST: (1/3) Armor Sets
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory.name",
        "\\((?<currentPage>\\d+)/\\d+\\) Armor Sets"
    )

    /**
     * REGEX-TEST: (1/2) Equipment Sets
     */
    private val equipmentInventoryPattern by patternGroup.pattern(
        "inventory.name.equipment",
        "\\((?<currentPage>\\d+)/\\d+\\) Equipment Sets"
    )

    /**
     * REGEX-TEST: Slot 4: Equipped
     */
    private val equippedSlotPattern by patternGroup.pattern(
        "equippedslot.colorless",
        "Slot \\d+: Equipped",
    )

    const val FIRST_SLOT = 36
    private const val FIRST_HELMET_SLOT = 0
    private const val FIRST_CHESTPLATE_SLOT = 9
    private const val FIRST_LEGGINGS_SLOT = 18
    private const val FIRST_BOOTS_SLOT = 27
    const val MAX_SLOT_PER_PAGE = 9
    const val MAX_PAGES = 3

    var armorSlots = listOf<WardrobeSlot>()
        private set
    var equipmentSlots = listOf<WardrobeSlot>()
        private set
    val slots: List<WardrobeSlot>? get() = getSlots(inWardrobeType ?: return null)
    var inCustomWardrobe = false

    internal fun emptyItems(): List<SafeItemStack?> = listOf(null, null, null, null)

    var currentSlot: Int?
        get() = storage?.currentSlot
        set(value) {
            storage?.currentSlot = value
        }

    var currentEquipmentSlot: Int?
        get() = storage?.currentEquipmentSlot
        set(value) {
            storage?.currentEquipmentSlot = value
        }

    var currentPage: Int? = null
    private var inWardrobeType: WardrobeType? = null

    init {
        val aList = mutableListOf<WardrobeSlot>()
        val eList = mutableListOf<WardrobeSlot>()
        var aId = 0
        var eId = 0

        for (page in 1..MAX_PAGES) {
            for (slot in 0 until MAX_SLOT_PER_PAGE) {
                val inventorySlot = FIRST_SLOT + slot
                val item1Slot = FIRST_HELMET_SLOT + slot
                val item2Slot = FIRST_CHESTPLATE_SLOT + slot
                val item3Slot = FIRST_LEGGINGS_SLOT + slot
                val item4Slot = FIRST_BOOTS_SLOT + slot

                aList.add(WardrobeSlot(++aId, page, inventorySlot, item1Slot, item2Slot, item3Slot, item4Slot, WardrobeType.ARMOR))
                eList.add(WardrobeSlot(++eId, page, inventorySlot, item1Slot, item2Slot, item3Slot, item4Slot, WardrobeType.EQUIPMENT))
            }
        }
        armorSlots = aList
        equipmentSlots = eList
    }

    fun getSlots(type: WardrobeType) = when (type) {
        EQUIPMENT -> equipmentSlots
        ARMOR -> armorSlots
    }

    fun getCurrentWardrobeSlot(type: WardrobeType) = when (type) {
        EQUIPMENT -> currentEquipmentSlot
        ARMOR -> currentSlot
    }

    private fun getWardrobeItem(itemStack: SafeItemStack?) =
        if (itemStack == null || itemStack.isStainedGlassPane()) null else itemStack

    private fun getWardrobeSlotFromId(id: Int?, type: WardrobeType) = getSlots(type).find { it.id == id }

    fun inWardrobe() = InventoryUtils.inInventory() && inWardrobeType == WardrobeType.ARMOR

    fun inEquipmentWardrobe() = InventoryUtils.inInventory() && inWardrobeType == WardrobeType.EQUIPMENT

    fun createPriceLore(slot: WardrobeSlot, type: WardrobeType = slot.type) = buildList {
        if (slot.isEmpty()) return@buildList
        add("§aEstimated ${type.displayName} Value:")
        var totalPrice = 0.0
        for (stack in slot.items.filterNotNull().filter { it.getInternalNameOrNull() != null }) {
            EstimatedItemValueCalculator.getTotalPrice(stack)?.let { price ->
                add("  §7- ${stack.hoverName.formattedTextCompatLeadingWhiteLessResets()}: §6${price.shortFormat()}")
                totalPrice += price
            }
        }
        if (totalPrice != 0.0) add(" §aTotal Value: §6§l${totalPrice.shortFormat()} coins")
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inventoryPattern.matches(event.inventoryName).let {
            inWardrobeType = WardrobeType.ARMOR
            if (CustomWardrobe.config.enabled) inCustomWardrobe = it
        }
        equipmentInventoryPattern.matches(event.inventoryName).let {
            inWardrobeType = WardrobeType.EQUIPMENT
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        val wardrobeType = checkInventory(event.inventoryName) ?: return
        inWardrobeType = wardrobeType

        val itemsList = event.inventoryItems
        val activeSlots = getSlots(wardrobeType)

        val allGrayDye = activeSlots.all {
            itemsList[it.inventorySlot]?.isDye(DyeCompat.GRAY) == true || !it.isInCurrentPage()
        }

        if (allGrayDye) {
            val allSlotsEmpty = activeSlots.filter { it.isInCurrentPage() }.all { slot ->
                (slot.inventorySlots.all { getWardrobeItem(itemsList[it]) == null })
            }
            if (allSlotsEmpty) {
                for (slot in activeSlots.filter { it.isInCurrentPage() }) {
                    slot.getData()?.armor = emptyItems()
                }
            } else return
        }

        val foundCurrentSlot = processSlots(activeSlots, itemsList, wardrobeType)
        val currentSlotId = getCurrentWardrobeSlot(wardrobeType)

        if (!foundCurrentSlot && getWardrobeSlotFromId(currentSlotId, wardrobeType)?.page == currentPage) {
            when (wardrobeType) {
                EQUIPMENT -> currentEquipmentSlot = null
                ARMOR -> currentSlot = null
            }
            WardrobeUpdateEvent(wardrobeType, emptyItems()).post()
        }
    }

    private fun checkInventory(inventoryName: String): WardrobeType? {
        if (inventoryPattern.matchMatcher(inventoryName) {
                currentPage = group("currentPage").formatInt()
            } != null
        ) {
            return WardrobeType.ARMOR
        }

        if (equipmentInventoryPattern.matchMatcher(inventoryName) {
            currentPage = group("currentPage").formatInt()
        } != null) {
            return WardrobeType.EQUIPMENT
        }

        return null
    }

    private fun processSlots(activeSlots: List<WardrobeSlot>, itemsList: Map<Int, SafeItemStack>, type: WardrobeType): Boolean {
        var foundCurrentSlot = false

        for (slot in activeSlots.filter { it.isInCurrentPage() }) {
             val updatedItems = listOf(
                getWardrobeItem(itemsList[slot.item1Slot]),
                getWardrobeItem(itemsList[slot.item2Slot]),
                getWardrobeItem(itemsList[slot.item3Slot]),
                getWardrobeItem(itemsList[slot.item4Slot]),
            )
            slot.getData()?.armor = updatedItems
            if (equippedSlotPattern.matches(itemsList[slot.inventorySlot]?.cleanName())) {
                val wasNew = (getCurrentWardrobeSlot(type) != slot.id)
                when (type) {
                    EQUIPMENT -> currentEquipmentSlot = slot.id
                    ARMOR -> currentSlot = slot.id
                }
                foundCurrentSlot = true
                if (wasNew) {
                    WardrobeUpdateEvent(type, updatedItems).post()
                }
            }
            slot.locked = (itemsList[slot.inventorySlot]?.isDye(DyeCompat.RED) == true)
            if (slot.locked) activeSlots.forEach { if (it.id > slot.id) it.locked = true }
        }

        return foundCurrentSlot
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inWardrobeType ?: return

        DelayedRun.runDelayed(250.milliseconds) {
            val inventoryName = InventoryUtils.openInventoryName()

            val wardrobeType = checkInventory(inventoryName)

            if (wardrobeType == null) {
                currentPage = null
            }
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Wardrobe")
        event.addIrrelevant {
            if (armorSlots.isEmpty() && equipmentSlots.isEmpty()) {
                add("No slots")
                return@addIrrelevant
            }

            fun addDebugInfo(slots: List<WardrobeSlot>, type: WardrobeType) {
                add("--- ${type.displayName} Wardrobe ---")
                for (slot in slots) {
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
                        type.slotNames.forEachIndexed { id, armorName ->
                            slot.getData()?.armor?.get(id)?.hoverName?.formattedTextCompatLeadingWhiteLessResets()?.let { name ->
                                add("   $armorName: $name")
                            }
                        }
                    }
                }
            }

            addDebugInfo(armorSlots, WardrobeType.ARMOR)
            addDebugInfo(equipmentSlots, WardrobeType.EQUIPMENT)
        }
    }

    class WardrobeData(
        @Expose val id: Int,
        @Expose var armor: List<SafeItemStack?>, // 'armor' kept to avoid breaking JSON backwards compat
        @Expose var locked: Boolean,
        @Expose var favorite: Boolean,
    )
}
