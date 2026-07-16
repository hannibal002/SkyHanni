package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import at.hannibal2.skyhanni.utils.compat.DyeCompat.Companion.isDye
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object LoadoutApi {

    val storage get() = ProfileStorageData.profileSpecific?.loadout
    val config get() = SkyHanniMod.feature.inventory.customLoadout

    private val patternGroup = RepoPattern.group("inventory.loadout")

    /**
     * REGEX-TEST: (1/3) Loadouts
     * REGEX-TEST: (2/3) Loadouts
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory.name",
        "\\((?<currentPage>\\d+)/\\d+\\) Loadouts",
    )

    /**
     * REGEX-TEST: Left-click to equip!
     */
    private val unequippedSlotPattern by patternGroup.pattern(
        "unequippedslot",
        "Left-click to equip!",
    )

    /**
     * REGEX-TEST: You must customize this loadout
     */
    private val unusedSlotPattern by patternGroup.pattern(
        "unusedslot",
        "You must customize this loadout",
    )

    /**
     * REGEX-TEST: Loadout 19 Locked
     */
    private val lockedSlotPattern by patternGroup.pattern(
        "lockedslot",
        "Loadout \\d+ Locked",
    )

    /**
     * REGEX-TEST: §7Current: §aHeart of the Forest 1
     * REGEX-TEST: §7Current: §aHeart of the Mountain 1
     * REGEX-TEST: §7Current: §aHurtful
     */
    private val currentSelectionPattern by patternGroup.pattern(
        "currentselection",
        "Current: (?:§.)*(?<selection>.+)",
    )

    /**
     * REGEX-TEST: Your tuning:
     */
    private val tuningHeaderPattern by patternGroup.pattern(
        "tuningheader",
        "Your tuning:",
    )

    private val ARMOR_SLOTS = listOf(11, 20, 29, 38)
    private val EQUIPMENT_SLOTS = listOf(10, 19, 28, 37)
    private const val PET_SLOT = 21
    private const val POWERSTONE_SLOT = 27
    private const val TUNINGS_SLOT = 36
    private const val HOTM_SLOT = 18
    private const val HOTF_SLOT = 9

    private const val FIRST_ICON_SLOT = 14
    private val ROWS_PER_PAGE = listOf(4, 4, 1)
    private const val SLOTS_PER_ROW = 3
    private const val MAX_PAGES = 3

    var slots = listOf<LoadoutSlot>()

    var currentPage: Int? = null
    private var inLoadouts = false

    internal fun emptyArmor(): List<SafeItemStack?> = listOf(null, null, null, null)
    internal fun emptyEquipment(): List<SafeItemStack?> = listOf(null, null, null, null)

    var currentSlot: Int?
        get() = storage?.currentSlot
        set(value) {
            storage?.currentSlot = value
        }

    init {
        val list = mutableListOf<LoadoutSlot>()
        var id = 0

        for (page in 1..MAX_PAGES) {
            for (row in 1..ROWS_PER_PAGE[page - 1]) {
                for (slot in 0 until SLOTS_PER_ROW) {
                    val index = FIRST_ICON_SLOT + ((row - 1) * 9) + slot
                    list.add(LoadoutSlot(id++, page, index))
                }
            }
        }
        slots = list
    }

    private fun getLoadoutItem(itemStack: SafeItemStack?) =
        if (itemStack == null || itemStack.isStainedGlassPane()) null else itemStack

    private fun getLoadoutSlotFromId(id: Int?) = slots.find { it.id == id }

    fun inLoadouts() = InventoryUtils.inInventory() && inLoadouts

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inventoryPattern.matches(event.inventoryName).let {
            inLoadouts = it
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        inventoryPattern.matchMatcher(event.inventoryName) {
            inLoadouts = true
            currentPage = group("currentPage").formatInt()
        } ?: return

        val itemsList = event.inventoryItems

        processIcons(itemsList)
        processSelectedLoadout(itemsList)
    }

    // Loadouts are supposed to change stuff on the Player.
    // If a loadout doesn't change anything, it doesn't have the "Left-click to equip!" so it's considered as equipped here
    private fun processIcons(itemsList: Map<Int, SafeItemStack>) {
        var anyLoadoutEquipped = false
        for (slot in slots.filter { it.isInCurrentPage() }) {
            val icon = itemsList[slot.inventorySlot]

            if (icon?.isDye(DyeCompat.GRAY) == true) {
                slot.getData()?.clear()
            }
            if (icon?.isDye(DyeCompat.RED) == true) {
                slot.getData()?.clear()
                slot.locked = true
            } else slot.locked = false
            if (icon.isCurrentSelectedLoadout()) {
                currentSlot = slot.id
                anyLoadoutEquipped = true
            }
            slot.getData()?.name = icon?.hoverName?.formattedTextCompatLeadingWhiteLessResets()
        }
        if (!anyLoadoutEquipped) currentSlot = null
    }

    private fun SafeItemStack?.isCurrentSelectedLoadout(): Boolean {
        if (this == null) return false
        if (lockedSlotPattern.matches(hoverName)) return false
        val lore = this.getLoreComponent()
        return lore.none { unequippedSlotPattern.find(it) } && lore.none { unusedSlotPattern.find(it) }
    }

    private fun processSelectedLoadout(itemsList: Map<Int, SafeItemStack>) {
        val data = getLoadoutSlotFromId(currentSlot)?.getData() ?: return

        data.armor = ARMOR_SLOTS.map { getLoadoutItem(itemsList[it]) }
        data.equipment = EQUIPMENT_SLOTS.map { getLoadoutItem(itemsList[it]) }
        data.pet = getLoadoutItem(itemsList[PET_SLOT])
        data.powerstone = itemsList[POWERSTONE_SLOT].parseCurrentSelection()
        data.tunings = itemsList[TUNINGS_SLOT].parseTunings()
        data.hotm = itemsList[HOTM_SLOT].parseCurrentSelection()
        data.hotf = itemsList[HOTF_SLOT].parseCurrentSelection()

        EquipmentSlot.entries.forEach {
            val itemStack = data.equipment[it.ordinal]
            if (itemStack != null && !itemStack.isStainedGlassPane()) {
                EquipmentApi.setEquipment(it, itemStack)
            } else EquipmentApi.setEquipment(it, null)
        }
    }

    // This is for Hotm, Hotf and Powerstone
    private fun SafeItemStack?.parseCurrentSelection(): String? {
        if (this == null) return null
        return this.getLoreComponent().firstNotNullOfOrNull {
            currentSelectionPattern.findMatcher(it.formattedTextCompatLessResets()) {
                group("selection")
            }
        }
    }

    private fun SafeItemStack?.parseTunings(): List<String>? {
        if (this == null) return null
        val lore = getLoreComponent()
        val headerIndex = lore.indexOfFirst { tuningHeaderPattern.find(it) }
        if (headerIndex == -1) return null
        val tunings = lore.drop(headerIndex + 1).takeWhile { it.string.isNotEmpty() }
        return if (tunings.isEmpty()) null else tunings.map { it.formattedTextCompatLessResets() }
    }

    fun clickSlot(slot: LoadoutSlot) {
        if (!slot.isInCurrentPage() || slot.locked) return
        currentSlot = slot.id
        InventoryUtils.clickSlot(slot.inventorySlot)
    }

    @HandleEvent
    fun onInventoryClose() {
        if (!inLoadouts) return
        DelayedRun.runDelayed(250.milliseconds) {
            if (!inventoryPattern.matches(InventoryUtils.openInventoryName())) {
                inLoadouts = false
                currentPage = null
            }
        }
    }

    class LoadoutData(
        @Expose val id: Int,
        @Expose var name: String?,
        @Expose var armor: List<SafeItemStack?>,
        @Expose var equipment: List<SafeItemStack?>,
        @Expose var pet: SafeItemStack?,
        @Expose var powerstone: String?,
        @Expose var tunings: List<String>?,
        @Expose var hotm: String?,
        @Expose var hotf: String?,
        @Expose var locked: Boolean,
        @Expose var favorite: Boolean,
    ) {
        fun clear() {
            name = null
            armor = emptyArmor()
            equipment = emptyEquipment()
            pet = null
            powerstone = null
            tunings = null
            hotm = null
            hotf = null
        }
    }
}
