package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
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
import at.hannibal2.skyhanni.utils.CollectionUtils.enumMapOf
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
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnrichment
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object AccessoryApi {

    private val patternGroup = RepoPattern.group("data.accessory")
    private val storage get() = ProfileStorageData.profileSpecific?.stats?.accessoryStorage

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: PARTY_HAT_CRAB_BLACK_ANIMATED
     */
    private val isHatPattern by patternGroup.pattern(
        "is.hat",
        "(?:PARTY|BALLOON)_HAT_.*",
    )

    /**
     * REGEX-TEST: ABICASE_BLUE_BLUE
     */
    val isAbiCasePattern by patternGroup.pattern(
        "is.abicase",
        "ABICASE_.*",
    )

    /**
     * REGEX-TEST: Accessory Bag
     * REGEX-TEST: Accessory Bag (1/2)
     */
    private val accessoryBagNamePattern by RepoPattern.pattern(
        "bagname",
        "Accessory Bag(?: \\((?<page>\\d)\\/\\d\\))?",
    )

    /**
     * REGEX-TEST: §7Bonus Pest Chance: §a+60%
     * REGEX-TEST: §7Combat Wisdom: §a+5
     * REGEX-TEST: §7Crit Damage: §c+6% §d(+6%)
     * REGEX-TEST: §7Defense: §a+15 §d(+15)
     * REGEX-TEST: §7Farming Fortune: §a+5 §d(+5)
     * REGEX-TEST: §7Fear: §a+6
     * REGEX-TEST: §7Fishing Speed: §a+1
     * REGEX-TEST: §7Fishing Wisdom: §a+1
     * REGEX-TEST: §7Foraging Fortune: §a+5 §d(+5)
     * REGEX-TEST: §7Foraging Fortune: §a+25
     * REGEX-TEST: §7Foraging Wisdom: §a+1
     * REGEX-TEST: §7Gemstone Fortune: §a+10
     * REGEX-TEST: §7Health: §a+5
     * REGEX-TEST: §7Health: §a+15 §d(+15)
     * REGEX-TEST: §7Health Regen: §a+3
     * REGEX-TEST: §7Intelligence: §a+3
     * REGEX-TEST: §7Intelligence: §a+15 §d(+15)
     * REGEX-TEST: §7Magic Find: §a+0.5
     * REGEX-TEST: §7Magic Find: §a+0.5 §8(+0.5)
     * REGEX-TEST: §7Mana Regen: §a+10%
     * REGEX-TEST: §7Mining Fortune: §a+25 §d(+25)
     * REGEX-TEST: §7Mining Speed: §a+50 §d(+50)
     * REGEX-TEST: §7Pet Luck: §a+1
     * REGEX-TEST: §7Pristine: §a+1 §d(+1)
     * REGEX-TEST: §7Runecrafting Wisdom: §a+2
     * REGEX-TEST: §7Sea Creature Chance: §c+1.5%
     * REGEX-TEST: §7Sea Creature Chance: §c+1% §d(+1%)
     * REGEX-TEST: §7Speed: §a+3
     * REGEX-TEST: §7Strength: §c+8 §d(+8)
     * REGEX-TEST: §7Trophy Fish Chance: §a+3%
     * REGEX-TEST: §7True Defense: §a+3
     * REGEX-TEST: §7True Defense: §a+6.5 §d(+6.5)
     * REGEX-TEST: §7Vitality: §a+10
     */
    private val accessoryStatsLorePattern by RepoPattern.pattern(
        "lore.stats",
        "§7(?<stat>[\\w ]+): (?:§.)+\\+(?<value>[\\d.]+)%?(?: (?:§.)+\\(.*\\))?"
    )
    // </editor-fold>

    // Acting as the vertex in the graph
    data class Accessory(
        var index: Int = -1,
        @Expose var internalName: NeuInternalName,
        @Expose var rarity: LorenzRarity? = null,
        @Expose var enrichment: SkyblockStat? = null,
        @Expose var totalStats: Map<SkyblockStat, Double> = enumMapOf(),
    ) {
        override fun toString(): String = internalName.asString()

        val magicPower: Int get() = if (rarity == null) 0 else getMagicalPower()
        val successor: Accessory?
            get() = repoAccessoryLineage.getRelatives(this, LineageType.SUCCESSOR, limit = 1).firstOrNull()
        val siblings: List<Accessory>
            get() = repoAccessoryLineage.getRelatives(this, LineageType.SIBLING, Int.MAX_VALUE)
        val predecessor: Accessory?
            get() = repoAccessoryLineage.getPredecessorOrNull(this)
    }

    // Edge in the graph
    data class LineageConnection(
        private val sourceIndex: Int,
        private val targetIndex: Int,
        val type: LineageType,
    ) {
        val source: Accessory? get() = repoAccessoryLineage.getByIndexOrNull(sourceIndex)
        val target: Accessory? get() = repoAccessoryLineage.getByIndexOrNull(targetIndex)
        override fun toString(): String = "$source -[$type]-> $target"
    }

    // Acting as a pseudo-weight for the graph
    enum class LineageType(private val displayName: String) {
        SUCCESSOR("Successor"), // a greater accessory
        SIBLING("Sibling"), // an accessory of the same tier (abicases, hats)
        ;

        override fun toString(): String = displayName
    }

    // Todo
    private fun Accessory.getMagicalPower(): Int = when (rarity) {
        LorenzRarity.LEGENDARY -> 6
        else -> 0
    }

    class AccessoryLineageTree {
        private val adjacencyMap = mutableMapOf<Accessory, ArrayList<LineageConnection>>()
        val accessorySet get() = adjacencyMap.keys

        fun getByIndexOrNull(index: Int) = adjacencyMap.keys.find { it.index == index }

        fun getAccessoryOrNull(neuInternalName: NeuInternalName) =
            adjacencyMap.keys.find { it.internalName == neuInternalName }

        fun getRelatives(
            accessory: Accessory,
            relationshipType: LineageType,
            limit: Int = 1,
        ): List<Accessory> = adjacencyMap[accessory]
            ?.filter { it.type == relationshipType }
            ?.mapNotNull { it.target }
            ?.take(limit).orEmpty()

        fun getPredecessorOrNull(accessory: Accessory): Accessory? = adjacencyMap.entries.firstOrNull { (_, connections) ->
            connections.any { it.target == accessory && it.type == LineageType.SUCCESSOR }
        }?.key

        fun addAccessory(internalName: NeuInternalName) = addAccessory(Accessory(internalName = internalName))
        private fun addAccessory(accessory: Accessory): Accessory {
            accessory.index = adjacencyMap.size
            adjacencyMap[accessory] = arrayListOf()
            return accessory
        }

        fun addLineageConnection(source: Accessory, target: Accessory, type: LineageType) {
            val connection = LineageConnection(source.index, target.index, type)
            adjacencyMap[source]?.add(connection)
        }

        fun resetLineageConnections(accessory: Accessory?) = when (accessory) {
            null -> adjacencyMap.forEach { (_, connections) -> connections.clear() }
            else -> adjacencyMap[accessory]?.clear()
        }

        override fun toString(): String = buildString {
            adjacencyMap.forEach { (accessory, edges) ->
                if (edges.isEmpty()) append("$accessory -/->\n")
                else edges.forEach { append("$it\n") }
                appendLine()
            }
        }
    }

    private fun AccessoryLineageTree.rebuildLineageLine() {
        repoAccessoryLineageSoT.mapNotNull {
            val accessoryInternalName = it.key.toInternalName()
            val accessory = this.getAccessoryOrNull(accessoryInternalName) ?: return@mapNotNull null
            accessory to it.value
        }.forEach { (accessory, family) ->
            val internalNameStr = accessory.internalName.asString()
            val isAbiCase = isAbiCasePattern.matches(internalNameStr)
            val isHat = isHatPattern.matches(internalNameStr)

            val lineageType: LineageType = when {
                isAbiCase || isHat -> LineageType.SIBLING
                else -> LineageType.SUCCESSOR
            }

            val directFamily = when (lineageType) {
                LineageType.SIBLING -> family
                else -> family.take(1)
            }

            directFamily.mapNotNull { this.getAccessoryOrNull(it.toInternalName()) }
                .forEach { targetAccessory -> // Climb down the lineage tree (or across)
                    this.addLineageConnection(targetAccessory, accessory, lineageType)
                }
        }
    }

    private var lateRepoLoad = false
    private var inventoryHashCache: Int = 0
    private val pageHashCache: TimeLimitedCache<Int, Int> = TimeLimitedCache(10.minutes)
    private val ignoredAccessories: MutableList<NeuInternalName> = mutableListOf()
    private val repoAccessoryLineageSoT: MutableMap<String, List<String>> = mutableMapOf()

    fun getMissingAccessories(storage: ProfileSpecificStorage.StatsStorage.AccessoryStorage) =
        repoAccessoryLineageSoT

    val repoAccessoryLineage: AccessoryLineageTree by lazy {
        AccessoryLineageTree().apply {
            EnoughUpdatesManager.getItemInformation().entries
                .map { it.key.toInternalName() to it.value }
                .filter { it.first.isAccessory() }
                .forEach { addAccessory(it.first) }
            if (repoAccessoryLineageSoT.isEmpty()) lateRepoLoad = true
            else this.rebuildLineageLine()
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

    @HandleEvent
    fun onNeuRepoReloadEvent(event: NeuRepositoryReloadEvent) {
        val misc = event.getConstant<NeuMiscJson>("misc")
        val newIgnores = misc.ignoredTalismans.map { it.toInternalName() }
            .filter { it !in ignoredAccessories }
            .takeIfNotEmpty() ?: return
        ignoredAccessories.addAll(newIgnores)

        val newLineageLines = misc.talismanUpgrades.filter { it.key !in repoAccessoryLineageSoT.keys }
        repoAccessoryLineageSoT.putAll(newLineageLines)
        if (lateRepoLoad) {
            repoAccessoryLineage.resetLineageConnections(null)
            repoAccessoryLineage.rebuildLineageLine()
        }
        lateRepoLoad = true // Always re-trigger building the lineage line after initial load
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
