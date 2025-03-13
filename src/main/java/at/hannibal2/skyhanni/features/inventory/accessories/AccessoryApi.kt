package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuMiscJson
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.inventory.AccessoriesUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnrichment
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object AccessoryApi {

    private val patternGroup = RepoPattern.group("data.accessory")
    private val storage get() = ProfileStorageData.profileSpecific?.stats?.accessoryStorage

    // <editor-fold desc="Patterns">
    val isAbiCasePattern by patternGroup.pattern(
        "is.abicase",
        "ABICASE_.*",
    )

    val isHatPattern by patternGroup.pattern(
        "is.hat",
        "(?:PARTY|BALLOON)_HAT_.*",
    )

    private val accessoryBagNamePattern by RepoPattern.pattern(
        "bagname",
        "Accessory Bag(?: \\((?<page>\\d)\\/\\d\\))?",
    )

    private val accessoryStatsLorePattern by RepoPattern.pattern(
        "lore.stats",
        "§7(?<stat>[\\w ]+): (?:§.)+\\+(?<value>[\\d.]+)%?(?: (?:§.)+\\(.*\\))?",
    )
    // </editor-fold>

    val HEGEMONY_ARTIFACT = "HEGEMONY_ARTIFACT".toInternalName()
    val RIFT_PRISM = "RIFT_PRISM".toInternalName()

    private var lateRepoLoad = false
    private var inventoryHashCache: Int = 0
    private val pageHashCache: TimeLimitedCache<Int, Int> = TimeLimitedCache(10.minutes)
    private val ignoredAccessories: MutableList<NeuInternalName> = mutableListOf()
    private val repoAccessoryLineageSoT: MutableMap<String, List<String>> = mutableMapOf()

    fun getMissing(storage: AccStorage): List<Accessory> = repoAccessoryLineage.getAdjacencyMap().keys.filter { acc ->
        if (acc.internalName in ignoredAccessories) return@filter false
        if (storage.hasAccessory(acc.internalName)) return@filter false
        if (storage.isFulfilled(acc.internalName)) return@filter false

        true
    }

    val repoAccessoryLineage: AccessoryLineageTree by lazy {
        AccessoryLineageTree().apply {
            EnoughUpdatesManager.getItemInformation().entries
                .map { it.key.toInternalName() to it.value }
                .filter { it.first.isAccessory() }
                .forEach { addAccessory(it.first) }
            if (repoAccessoryLineageSoT.isEmpty()) lateRepoLoad = true
            else this.rebuildLineageLine(repoAccessoryLineageSoT)
        }
    }
    var inAccessoryBag = false
        private set

    private fun ItemStack.toStorageAccessory(): Accessory? {
        val internalName = getInternalNameOrNull() ?: return null
        val rarity = getItemRarityOrNull() ?: return null
        val enrichment = SkyblockStat.getValueOrNull(getEnrichment().orEmpty())
        return Accessory(
            internalName = internalName,
            rarity = rarity,
            enrichment = enrichment,
            totalStats = getAccessoryStatsOrEmpty(),
        )
    }

    private fun ItemStack.getAccessoryStatsOrEmpty(): Map<SkyblockStat, Double> =
        if (!this.isAccessory() || this.getLore().isEmpty()) emptyMap()
        else this.getLore().mapNotNull { line ->
            accessoryStatsLorePattern.matchMatcher(line) {
                val fixedStat = group("stat").replace(" ", "_").uppercase()
                val stat = SkyblockStat.getValueOrNull(fixedStat) ?: return@matchMatcher null
                val value = groupOrNull("value")?.formatDoubleOrNull() ?: return@matchMatcher null
                stat to value
            }
        }.toMap()

    fun NeuInternalName.isAccessory() = this.getItemStackOrNull()?.isAccessory() ?: false
    fun ItemStack.isAccessory(): Boolean = getItemCategoryOrNull() in ItemCategory.accessories
    fun ItemStack.getAccessoryRarityOrNull(): LorenzRarity? = when {
        isAccessory() -> getItemRarityOrNull()
        else -> null
    }

    fun LorenzRarity.getBaseMagicalPower(): Int? = when (this) {
        LorenzRarity.COMMON, LorenzRarity.SPECIAL -> 3
        LorenzRarity.UNCOMMON, LorenzRarity.VERY_SPECIAL -> 5
        LorenzRarity.RARE -> 8
        LorenzRarity.EPIC -> 12
        LorenzRarity.LEGENDARY -> 16
        LorenzRarity.MYTHIC -> 22
        else -> null
    }

    @HandleEvent
    fun onNeuRepoReloadEvent(event: NeuRepositoryReloadEvent) {
        val misc = event.getConstant<NeuMiscJson>("misc")
        val newIgnores = misc.ignoredTalismans.map { it.toInternalName() }
            .filter { it !in ignoredAccessories }
            .takeIfNotEmpty() ?: return
        ignoredAccessories.addAll(newIgnores)

        val newLineageLines = misc.talismanUpgrades.filter { it.key !in repoAccessoryLineageSoT.keys }
        repoAccessoryLineageSoT.putAll(newLineageLines)
        if (lateRepoLoad) repoAccessoryLineage.rebuildLineageLine(repoAccessoryLineageSoT)
        lateRepoLoad = true // Always re-trigger lineage building after initial load
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inAccessoryBag = false
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        val storage = storage ?: return
        var page = -1
        accessoryBagNamePattern.matchMatcher(event.inventoryName) {
            page = when (groupOrNull("page")) {
                null -> 1
                else -> groupOrNull("page")?.formatIntOrNull() ?: 1
            }
            inAccessoryBag = true
        }
        if (!inAccessoryBag || page == -1 || event.inventoryItems.isEmpty()) return

        pageHashCache[page] = event.inventoryItems.hashCode()
            .takeIf { it != pageHashCache[page] } ?: return

        val pageIndex = page - 1
        val lastRowFiltered = event.inventoryItems.filter { it.key !in 45..53 }
        val emptyFiltered = lastRowFiltered.values.filter { it.hasDisplayName() && it.getLore().isNotEmpty() }
        val accessoryItems = emptyFiltered.filter { it.isAccessory() }
        val mappedAccessoryItems = accessoryItems.mapNotNull { it.toStorageAccessory() }

        val target = when (IslandType.THE_RIFT.isInIsland()) {
            true -> storage.riftAccessories
            false -> storage.mainAccessories
        }
        target.accessoryPages[pageIndex] = mappedAccessoryItems
        AccessoriesUpdatedEvent(storage).post()
    }

    @HandleEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        val storage = storage ?: return

        val ownInventoryItems = InventoryUtils.getItemsInOwnInventory().takeIf {
            it.hashCode() != inventoryHashCache
        } ?: return
        inventoryHashCache = ownInventoryItems.hashCode()

        val accessoriesInOwnInventory = ownInventoryItems
            .filter { it.hasDisplayName() && it.getLore().isNotEmpty() }
            .filter { it.isAccessory() }
            .mapNotNull { it.toStorageAccessory() }
            .toMutableList()

        val target = when (IslandType.THE_RIFT.isInIsland()) {
            true -> storage.riftAccessories
            false -> storage.mainAccessories
        }
        target.looseAccessories = accessoriesInOwnInventory
        AccessoriesUpdatedEvent(storage).post()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(75, "inventory.magicalPower", "inventory.stats.magicalPower")
        event.move(75, "inventory.statsTuning", "inventory.stats.statsTuning")
        event.move(75, "#profile.stats", "#profile.stats.currentStats")
        event.move(75, "#profile.maxwell", "#profile.stats.maxwell")
    }
}
