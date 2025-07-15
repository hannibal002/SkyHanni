package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.subMapOfStringsStartingWith
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import java.util.NavigableMap
import java.util.TreeMap

@SkyHanniModule
object StorageApi {

    private val storage: NavigableMap<String, SkyHanniInventoryContainer>
        get() = ProfileStorageData.storageProfiles?.data ?: TreeMap()

    /**
     * REGEX-TEST: Ender Chest
     * REGEX-TEST: Ender Chest (1/9)
     */
    private val enderchestPattern by RepoPattern.pattern(
        "storage.enderchest",
        "Ender Chest(?: \\((?<page>\\d+)/\\d+\\))?",
    )

    /**
     * REGEX-TEST: Jumbo Backpack§r (Slot #2)
     */
    private val backpackPattern by RepoPattern.pattern(
        "storage.backpack",
        ".* Backpack§r \\(Slot #(?<page>\\d+)\\)",
    )

    /**
     * REGEX-TEST: Rift Storage (1/2)
     * REGEX-TEST: Rift Storage
     */
    private val riftStoragePattern by RepoPattern.pattern(
        "storage.rift",
        "Rift Storage(?: \\((?<page>\\d+)/\\d+\\))?",
    )

    val accessStorage: Map<String, SkyHanniInventoryContainer> get() = storage
    val enderchest: Map<String, SkyHanniInventoryContainer> get() = subMapOfStringsStartingWith("Ender Chest", storage)
    val backpack: Map<String, SkyHanniInventoryContainer> get() = subMapOfStringsStartingWith("Backpack", storage)
    val riftStorage: Map<String, SkyHanniInventoryContainer> get() = subMapOfStringsStartingWith("Rift Storage", storage)

    var currentStorage: SkyHanniInventoryContainer? = null
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        enderchestPattern.matchMatcher(event.inventoryName) {
            val page = groupOrNull("page")?.toInt() ?: 1
            handleRead("Ender Chest $page", event.inventoryItemsWithNull.values)
            return
        }
        backpackPattern.matchMatcher(event.inventoryName) {
            val page = groupOrNull("page")?.toInt() ?: 1
            handleRead("Backpack $page", event.inventoryItemsWithNull.values)
            return
        }
        riftStoragePattern.matchMatcher(event.inventoryName) {
            val page = groupOrNull("page")?.toInt() ?: 1
            handleRead("Rift Storage $page", event.inventoryItemsWithNull.values)
            return
        }
    }

    private var shouldReCheck = false
    private var shouldSave = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiContainerSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (currentStorage == null) return
        shouldReCheck = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!shouldReCheck) return
        currentStorage?.items = InventoryUtils.getItemsInOpenChestWithNull().map { it.stack }.drop(9)
        shouldReCheck = false
        shouldSave = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!shouldSave) return
        SkyHanniMod.configManager.saveConfig(ConfigFileType.STORAGE, "Updated Items")
        shouldSave = false
    }

    private fun handleRead(name: String, inventory: Collection<ItemStack?>) {
        val saneInventory = inventory.drop(9)
        val old = storage[name]
        val stored: SkyHanniInventoryContainer
        if (old == null) {
            stored = SkyHanniInventoryContainer(name, 9, saneInventory)
            storage[name] = stored
            return
        } else {
            stored = old
            old.items = saneInventory
        }
        currentStorage = stored
        shouldSave = true
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Storage Data")
        if (storage.isEmpty()) {
            event.addIrrelevant("Empty")
        } else {
            event.addIrrelevant(storage.values.sortedBy { it.internalName }.map { it.getDebug() + listOf("") }.flatten())
        }
    }
}
