package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.subMapOfStringsStartingWith
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.block.BlockChest
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
    private val mutableIslandChest: MutableMap<String, SkyHanniInventoryContainer>
        get() = subMapOfStringsStartingWith(
            "Private Island Chest",
            storage,
        )
    val islandChest: Map<String, SkyHanniInventoryContainer> get() = mutableIslandChest

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
        if (!IslandType.PRIVATE_ISLAND.isCurrent() || !isPrivateIslandStorageEnabled()) return
        if (InventoryUtils.isInNormalChest(event.inventoryName)) {
            handlePrivateIslandRead(event.inventoryItemsWithNull.values)
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

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onMinutePassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(60) || !isPrivateIslandStorageEnabled()) return
        mutableIslandChest.removeIf { (_, chest) ->
            if (chest.primaryCords == null) {
                ErrorManager.logErrorStateWithData(
                    "Something went wrong during Private Island cleanup",
                    "Tried to remove a container that isn't a chest",
                    "Chest" to chest,
                    "Storage" to accessStorage,
                )
                return@removeIf false
            }
            when {
                chest.primaryCords.distanceSqToPlayer() > 30 * 30 -> false
                chest.primaryCords.getBlockAt() !is BlockChest -> true
                chest.secondaryCords == null -> getNeighbourBlocks(chest.primaryCords).any { it.second is BlockChest }
                else -> chest.secondaryCords.getBlockAt() !is BlockChest
            }.also {
                if (it) ChatUtils.debug("Removed Private Island Chest at: ${chest.primaryCords}")
            }
        }
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

    private fun handlePrivateIslandRead(inventory: Collection<ItemStack?>) {
        val primary = lastChestClicked ?: run {
            ErrorManager.logErrorStateWithData("Failed to save chest", "Failed to save chest on Private Island", "inventory" to inventory)
            return
        }
        val secondary = doubleChestCord
        val name = "Private Island Chest $primary"
        val saneInventory = inventory.toList()
        val old = storage[name]
        val stored: SkyHanniInventoryContainer
        if (old == null) {
            stored = SkyHanniInventoryContainer(name, 9, saneInventory, "Private Island Chest", primary, secondary)
            storage[name] = stored
            return
        } else {
            stored = old
            old.items = saneInventory
        }
        currentStorage = stored
        shouldSave = true
    }

    private var lastChestClicked: LorenzVec? = null
    private var doubleChestCord: LorenzVec? = null

    private fun getNeighbourBlocks(position: LorenzVec) =
        listOf(position.add(x = 1), position.add(x = -1), position.add(z = 1), position.add(z = -1)).map {
            it to it.getBlockAt()
        }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (!isPrivateIslandStorageEnabled()) return
        val chest = event.getBlockState.block as? BlockChest ?: return
        // Double Chest Check
        val otherChest = getNeighbourBlocks(event.position).firstOrNull { it.second == chest }?.first
        if (otherChest == null) {
            lastChestClicked = event.position
            doubleChestCord = null
        } else if (otherChest.lengthSquared() > event.position.lengthSquared()) {
            lastChestClicked = event.position
            doubleChestCord = otherChest
        } else {
            lastChestClicked = otherChest
            doubleChestCord = event.position
        }
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

    private fun isPrivateIslandStorageEnabled() = SkyHanniMod.feature.inventory.savePrivateIslandChests
}
