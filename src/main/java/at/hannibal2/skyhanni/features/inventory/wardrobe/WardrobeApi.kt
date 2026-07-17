package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
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
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.milliseconds

abstract class WardrobeApi {

    companion object {
        @JvmStatic
        protected val patternGroup = RepoPattern.group("inventory.wardrobe")

        /**
         * REGEX-TEST: Slot 4: Equipped
         */
        val equippedSlotPattern by patternGroup.pattern(
            "equippedslot.colorless",
            "Slot \\d+: Equipped",
        )

        const val FIRST_SLOT = 36
        private const val FIRST_ITEM_SLOT = 0
        private const val SECOND_ITEM_SLOT = 9
        private const val THIRD_ITEM_SLOT = 18
        private const val FORTH_ITEM_SLOT = 27
        const val MAX_SLOT_PER_PAGE = 9
        const val MAX_PAGES = 3

        internal fun emptyItems(): List<SafeItemStack?> = listOf(null, null, null, null)
    }

    protected abstract val inventoryPattern: Pattern
    protected abstract val valueName: String
    protected abstract val debugTitle: String

    abstract val storage: ProfileSpecificStorage.WardrobeStorage?

    var slots: List<WardrobeSlot> = emptyList()
        private set

    private var inThisWardrobe = false

    internal var currentPage: Int? = null

    var currentSlot: Int?
        get() = storage?.currentSlot
        set(value) {
            storage?.currentSlot = value
        }

    init {
        val list = mutableListOf<WardrobeSlot>()
        var id = 0

        for (page in 1..MAX_PAGES) {
            for (slot in 0 until MAX_SLOT_PER_PAGE) {
                val inventorySlot = FIRST_SLOT + slot
                val item1Slot = FIRST_ITEM_SLOT + slot
                val item2Slot = SECOND_ITEM_SLOT + slot
                val item3Slot = THIRD_ITEM_SLOT + slot
                val item4Slot = FORTH_ITEM_SLOT + slot
                list.add(WardrobeSlot(this, ++id, page, inventorySlot, item1Slot, item2Slot, item3Slot, item4Slot))
            }
        }
        slots = list
    }

    private fun getWardrobeItem(itemStack: SafeItemStack?) =
        if (itemStack == null || itemStack.isStainedGlassPane()) null else itemStack

    private fun getWardrobeSlotFromId(id: Int?) = slots.find { it.id == id }

    fun inWardrobe() = InventoryUtils.inInventory() && inThisWardrobe

    fun createPriceLore(slot: WardrobeSlot) = buildList {
        if (slot.isEmpty()) return@buildList
        add("§aEstimated $valueName Value:")
        var totalPrice = 0.0
        for (stack in slot.armor.filterNotNull().filter { it.getInternalNameOrNull() != null }) {
            EstimatedItemValueCalculator.getTotalPrice(stack)?.let { price ->
                add("  §7- ${stack.hoverName.formattedTextCompatLeadingWhiteLessResets()}: §6${price.shortFormat()}")
                totalPrice += price
            }
        }
        if (totalPrice != 0.0) add(" §aTotal Value: §6§l${totalPrice.shortFormat()} coins")
    }

    protected fun handleInventoryOpen(inventoryName: String): Boolean {
        val matched = inventoryPattern.matches(inventoryName)
        inThisWardrobe = matched
        return matched
    }

    protected fun handleInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!checkInventory(event.inventoryName)) return

        val itemsList = event.inventoryItems

        val allGrayDye = slots.all {
            itemsList[it.inventorySlot]?.isDye(DyeCompat.GRAY) == true || !it.isInCurrentPage()
        }

        if (allGrayDye) {
            val allSlotsEmpty = slots.filter { it.isInCurrentPage() }.all { slot ->
                (slot.inventorySlots.all { getWardrobeItem(itemsList[it]) == null })
            }
            if (allSlotsEmpty) {
                for (slot in slots.filter { it.isInCurrentPage() }) {
                    slot.getData()?.armor = emptyItems()
                }
            } else return
        }

        val foundCurrentSlot = processSlots(slots, itemsList)
        if (!foundCurrentSlot && getWardrobeSlotFromId(currentSlot)?.page == currentPage) {
            currentSlot = null
        }
    }

    private fun checkInventory(inventoryName: String): Boolean =
        inventoryPattern.matchMatcher(inventoryName) {
            inThisWardrobe = true
            currentPage = group("currentPage").formatInt()
        } != null

    private fun processSlots(slots: List<WardrobeSlot>, itemsList: Map<Int, SafeItemStack>): Boolean {
        var foundCurrentSlot = false

        for (slot in slots.filter { it.isInCurrentPage() }) {
            slot.getData()?.armor = listOf(
                getWardrobeItem(itemsList[slot.item1Slot]),
                getWardrobeItem(itemsList[slot.item2Slot]),
                getWardrobeItem(itemsList[slot.item3Slot]),
                getWardrobeItem(itemsList[slot.item4Slot]),
            )
            if (equippedSlotPattern.matches(itemsList[slot.inventorySlot]?.cleanName)) {
                currentSlot = slot.id
                foundCurrentSlot = true
            }
            slot.locked = (itemsList[slot.inventorySlot]?.isDye(DyeCompat.RED) == true)
            if (slot.locked) slots.forEach { if (it.id > slot.id) it.locked = true }
        }

        return foundCurrentSlot
    }

    protected fun handleInventoryClose() {
        if (!inThisWardrobe) return

        DelayedRun.runDelayed(250.milliseconds) {
            val inventoryName = InventoryUtils.openInventoryName()
            inThisWardrobe = inventoryPattern.matches(inventoryName)
            if (!inThisWardrobe) currentPage = null
        }
    }

    protected fun handleDebugDataCollect(event: DebugDataCollectEvent) {
        event.title(debugTitle)
        event.addIrrelevant {
            if (slots.isEmpty()) {
                add("No slots")
                return@addIrrelevant
            }

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
                    setOf("Item1", "Item2", "Item3", "Item4").forEachIndexed { id, armorName ->
                        slot.getData()?.armor?.get(id)?.hoverName?.formattedTextCompatLeadingWhiteLessResets()?.let { name ->
                            add("   $armorName: $name")
                        }
                    }
                }
            }
        }
    }

    class WardrobeData(
        @Expose val id: Int,
        @Expose var armor: List<SafeItemStack?>,
        @Expose var locked: Boolean,
        @Expose var favorite: Boolean,
    )
}
