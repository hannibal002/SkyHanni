package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@SkyHanniModule
object StorageApi {

    private val storage: MutableMap<String, SkyHanniInventoryContainer>
        get() = ProfileStorageData.profileSpecific?.storage ?: mutableMapOf()

    /**
     * REGEX-TEST: Ender Chest (1/9)
     */
    private val enderchestPattern by RepoPattern.pattern("storage.enderchest", "Ender Chest \\((?<page>\\d)/\\d\\)")

    /**
     * REGEX-TEST: Jumbo Backpack§r (Slot #2)
     */
    private val backpackPattern by RepoPattern.pattern("storage.backpack", ".* Backpack§r \\(Slot #(?<page>\\d+)\\)")

    val accessStorage: Map<String, SkyHanniInventoryContainer> get() = storage

    var currentStorage : SkyHanniInventoryContainer? = null
    private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        enderchestPattern.matchMatcher(event.inventoryName) {
            val page = group("page").toInt()
            handleRead("Ender Chest $page",event.inventoryItemsWithNull.values)
            return
        }
        backpackPattern.matchMatcher(event.inventoryName) {
            val page = group("page").toInt()
            handleRead("Backpack $page",event.inventoryItemsWithNull.values)
            return
        }
    }

    private var shouldReCheck = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiContainerSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if(currentStorage == null) return
        shouldReCheck = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if(!shouldReCheck) return
        currentStorage?.items = InventoryUtils.getItemsInOpenChestWithNull().map { it.stack }.drop(9)
        shouldReCheck = false
    }

    private fun handleRead(name: String, inventory: Collection<ItemStack?>) {
        val saneInventory = inventory.drop(9)
        val old = storage[name]
        if (old == null) {
            val s = SkyHanniInventoryContainer(name, 9, saneInventory)
            storage[name] = s
            currentStorage = s
            return
        }
        old.items = saneInventory
        currentStorage = old
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Storage Data")
        if(storage.isEmpty()){
            event.addIrrelevant("Empty")
        }else {
            event.addIrrelevant(storage.values.sortedBy { it.internalName }.map { it.getDebug() + listOf("") }.flatten())
        }
    }
}
