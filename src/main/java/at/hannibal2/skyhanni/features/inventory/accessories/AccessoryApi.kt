package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuMiscJson
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnrichment
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object AccessoryApi {

    private val patternGroup = RepoPattern.group("data.accessory")
    private val storage get() = ProfileStorageData.profileSpecific

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
    private val isAbiCasePattern by patternGroup.pattern(
        "is.abicase",
        "ABICASE_.*",
    )

    /**
     * REGEX-TEST: §d§l§ka§r §d§lMYTHIC ACCESSORY §d§l§ka
     * REGEX-TEST: §6§l§ka§r §6§lLEGENDARY DUNGEON ACCESSORY §6§l§ka
     * REGEX-TEST: §6§l§ka§r §6§lLEGENDARY ACCESSORY §6§l§ka
     * REGEX-TEST: §6§lLEGENDARY ACCESSORY
     * REGEX-TEST: §5§l§ka§r §5§lEPIC ACCESSORY §5§l§ka
     * REGEX-TEST: §5§lEPIC ACCESSORY
     */
    private val isAccessoryLorePattern by patternGroup.pattern(
        "is.accessory.lore",
        "(?:§(?:k.|.))* ?(?:§.)*(?<rarity>.*?)(?=\\s(?:DUNGEON\\s)?)\\s(?:DUNGEON\\s)?(?:ACCESSORY|HATCESSORY)(?: §(?:k.|.)*)?",
    )

    /**
     * REGEX-TEST: Accessory Bag
     * REGEX-TEST: Accessory Bag (1/2)
     */
    private val accessoryBagNamePattern by RepoPattern.pattern(
        "bagname",
        "Accessory Bag(?: \\((?<page>\\d)\\/\\d\\))?",
    )
    // </editor-fold>

    // Acting as the vertex in the graph
    data class Accessory(
        var index: Int = -1,
        val internalName: NeuInternalName,
    ) {
        override fun toString(): String = internalName.asString()

        val successor: Accessory?
            get() = AccessoryApi.accessoryLineage.getRelatives(this, LineageType.SUCCESSOR, limit = 1).firstOrNull()
        val siblings: List<Accessory>
            get() = accessoryLineage.getRelatives(this, LineageType.SIBLING, Int.MAX_VALUE)
        val predecessor: Accessory?
            get() = accessoryLineage.getPredecessorOrNull(this)
    }

    // Edge in the graph
    data class LineageConnection(
        private val sourceIndex: Int,
        private val targetIndex: Int,
        val type: LineageType,
    ) {
        val source: Accessory? get() = accessoryLineage.getByIndexOrNull(sourceIndex)
        val target: Accessory? get() = accessoryLineage.getByIndexOrNull(targetIndex)
        override fun toString(): String = "$source -[$type]-> $target"
    }

    // Acting as a pseudo-weight for the graph
    enum class LineageType(private val displayName: String) {
        SUCCESSOR("Successor"), // a greater accessory
        SIBLING("Sibling"); // an accessory of the same tier

        override fun toString(): String = displayName
    }

    class AccessoryLineageTree {
        private val adjacencyMap = mutableMapOf<Accessory, ArrayList<LineageConnection>>()

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

        fun getPredecessorOrNull(accessory: Accessory): Accessory? {
            return adjacencyMap.entries.firstOrNull { (_, connections) ->
                connections.any { it.target == accessory && it.type == LineageType.SUCCESSOR }
            }?.key
        }

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
        accessoryLineageSoT.mapNotNull {
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

    private fun JsonObject.shouldProcessAccessory(): Boolean {
        val internalName = get("internalname")?.asString?.toInternalName() ?: return false
        if (internalName in ignoredAccessories) return false
        val lore = when {
            has("lore") -> get("lore").asJsonArray.map { it.asString }
            else -> return false
        }
        return isAccessory(internalName, lore)
    }

    private val pageCache: TimeLimitedCache<Int, Int> = TimeLimitedCache(10.minutes)
    private var lateRepoLoad = false
    private val ignoredAccessories: MutableList<NeuInternalName> = mutableListOf()
    private val accessoryLineageSoT: MutableMap<String, List<String>> = mutableMapOf()

    val accessoryLineage: AccessoryLineageTree by lazy {
        AccessoryLineageTree().apply {
            EnoughUpdatesManager.getItemInformation().entries
                .map { it.key.toInternalName() to it.value }
                .filter { it.second.shouldProcessAccessory() }
                .forEach { addAccessory(it.first) }
            if (accessoryLineageSoT.isEmpty()) lateRepoLoad = true
            else this.rebuildLineageLine()
        }
    }
    var inAccessoryBag = false
        private set

    private fun ItemStack.toStorageAccessory(): ProfileSpecificStorage.StorageAccessory? {
        val internalName = getInternalNameOrNull() ?: return null
        val rarity = getItemRarityOrNull() ?: return null
        val enrichment = SkyblockStat.getValueOrNull(getEnrichment().orEmpty())
        return ProfileSpecificStorage.StorageAccessory(internalName, rarity, enrichment)
    }

    fun ItemStack.isAccessory(): Boolean =
        getInternalNameOrNull()?.let { isAccessory(it, getLore()) } ?: false

    private fun isAccessory(internalName: NeuInternalName, lore: List<String>): Boolean =
        internalName !in ignoredAccessories && !isAccessoryLorePattern.anyMatches(lore)

    @HandleEvent
    fun onNeuRepoReloadEvent(event: NeuRepositoryReloadEvent) {
        val misc = event.getConstant<NeuMiscJson>("misc")
        val newIgnores = misc.ignoredTalismans.map { it.toInternalName() }
            .filter { it !in ignoredAccessories }
            .takeIfNotEmpty() ?: return
        ignoredAccessories.addAll(newIgnores)

        val newLineageLines = misc.talismanUpgrades.filter { it.key !in accessoryLineageSoT.keys }
        accessoryLineageSoT.putAll(newLineageLines)
        if (lateRepoLoad) {
            accessoryLineage.resetLineageConnections(null)
            accessoryLineage.rebuildLineageLine()
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

        pageCache[page] = event.inventoryItems.hashCode()
            .takeIf { it != pageCache[page] } ?: return

        val pageIndex = page - 1
        val lastRowFiltered = event.inventoryItems.filter { it.key !in 45..53 }
        val emptyFiltered = lastRowFiltered.values.filter { it.hasDisplayName() && it.getLore().isNotEmpty() }
        val accessoryItems = emptyFiltered.filter { it.isAccessory() }
        val mappedAccessoryItems = accessoryItems.mapNotNull { it.toStorageAccessory() }

        val target = when (IslandType.THE_RIFT.isInIsland()) {
            true -> storage.riftAccessoryStorage
            false -> storage.accessoryStorage
        }
        target.accessoryPages[pageIndex] = mappedAccessoryItems
    }
}
