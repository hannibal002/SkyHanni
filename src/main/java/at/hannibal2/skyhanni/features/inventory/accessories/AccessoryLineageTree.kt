package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.isAccessory
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.neuCraftTextSlayerCraftReqPattern
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.repoAccessoryLineage
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.utils.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import com.google.gson.JsonObject

class LineageConnection(
    private val sourceIndex: Int,
    private val targetIndex: Int,
    val type: LineageType,
) {
    val source: Accessory? get() = repoAccessoryLineage.getByIndexOrNull(sourceIndex)
    val target: Accessory? get() = repoAccessoryLineage.getByIndexOrNull(targetIndex)
    override fun toString(): String = "$source -[$type]-> $target"
}

enum class LineageType(private val displayName: String) {
    SUCCESSOR("Successor"),
    SIBLING("Sibling"),
    ;

    override fun toString(): String = displayName
}

class AccessoryLineageTree {
    private val adjacencyMap = mutableMapOf<Accessory, ArrayList<LineageConnection>>()
    fun getAdjacencyMap() = adjacencyMap.toMap()

    fun getByIndexOrNull(index: Int) = adjacencyMap.keys.find { it.index == index }
    private fun NeuInternalName.getAccessoryOrNull() = adjacencyMap.keys.find { it.internalName == this }

    fun getRelatives(
        accessory: Accessory,
        relationshipType: LineageType,
        limit: Int = 1,
    ): List<Accessory> = adjacencyMap[accessory]
        ?.filter { it.type == relationshipType }
        ?.mapNotNull { it.target }
        ?.filter { it.internalName != accessory.internalName }
        ?.take(limit).orEmpty()

    fun getRelatives(
        accessoryName: NeuInternalName,
        relationshipType: LineageType,
    ) = accessoryName.getAccessoryOrNull()?.let {
        val limit = when (relationshipType) {
            LineageType.SUCCESSOR -> 1
            LineageType.SIBLING -> 512
        }
        getRelatives(it, relationshipType, limit)
    }.orEmpty()

    private fun addLineageConnection(source: Accessory, target: Accessory, type: LineageType) {
        val connection = LineageConnection(source.index, target.index, type)
        adjacencyMap[source]?.add(connection)
    }

    private fun addAccessory(accessory: Accessory): Accessory {
        accessory.index = adjacencyMap.size
        adjacencyMap[accessory] = arrayListOf()
        return accessory
    }

    fun tryAddAccessory(repoData: MutableMap.MutableEntry<String, JsonObject>) {
        val internalName = repoData.key.toInternalName().takeIf { it.isAccessory() } ?: return
        val accessory = addAccessory(Accessory(internalName = internalName))
        val craftText = repoData.value.get("craftText")?.asString ?: return
        neuCraftTextSlayerCraftReqPattern.matchMatcher(craftText) {
            val slayerType = when (group("slayer")) {
                "Wolf" -> SlayerType.SVEN
                "Vampire" -> SlayerType.VAMPIRE
                "Blaze" -> SlayerType.INFERNO
                "Enderman" -> SlayerType.VOID
                "Spider" -> SlayerType.TARANTULA
                "Zombie" -> SlayerType.REVENANT
                else -> null
            } ?: return
            val level = group("level").formatIntOrNull() ?: return
            accessory.craftSlayerRequirement = slayerType to level
        }
    }

    fun rebuildLineageLine(sourceMap: Map<String, List<String>>) = sourceMap.mapNotNull {
        val accessory = it.key.toInternalName().getAccessoryOrNull() ?: return@mapNotNull null
        accessory to it.value.map { relative ->
            relative.toInternalName()
        }
    }.forEach { (accessory, relativeInternalNames) ->
        val lineageType: LineageType = if (accessory.isAbiCase || accessory.isHat)
            LineageType.SIBLING
        else
            LineageType.SUCCESSOR

        val directFamily = relativeInternalNames.mapNotNull { it.getAccessoryOrNull() }
        val applicableFamily = when (lineageType) {
            LineageType.SUCCESSOR -> directFamily.filter { it != accessory }.takeIfNotEmpty()?.take(1)
            LineageType.SIBLING -> directFamily
        } ?: return@forEach

        applicableFamily.forEach { targetAccessory ->
            this.addLineageConnection(accessory, targetAccessory, lineageType)
        }
    }

    override fun toString(): String = buildString {
        adjacencyMap.forEach { (accessory, edges) ->
            if (edges.isEmpty()) append("$accessory -/->\n")
            else edges.forEach { append("$it\n") }
            appendLine()
        }
    }
}
