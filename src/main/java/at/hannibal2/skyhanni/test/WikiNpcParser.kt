package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzVec

/**
 * This parses the npc locations of the hypixel wiki.
 *
 * 1. open https://hypixelskyblock.minecraft.wiki/w/NPC/List
 * 2. select all
 * 3. copy
 * 4. run the string through [parse]
 */
object WikiNpcParser {

    private val sectionToIsland = mapOf(
        "Hub Island" to IslandType.HUB,
        "The Barn" to IslandType.THE_FARMING_ISLANDS,
        "Mushroom Desert" to IslandType.THE_FARMING_ISLANDS,
        "Gold Mine" to IslandType.GOLD_MINES,
        "Deep Caverns" to IslandType.DEEP_CAVERNS,
        "Dwarven Mines" to IslandType.DWARVEN_MINES,
        "Crystal Hollows" to IslandType.CRYSTAL_HOLLOWS,
        "Spider's Den" to IslandType.SPIDER_DEN,
        "Spider\u2019s Den" to IslandType.SPIDER_DEN,
        "Crimson Isle" to IslandType.CRIMSON_ISLE,
        "Mushroom Desert" to IslandType.THE_FARMING_ISLANDS,
        "The Park" to IslandType.THE_PARK,
        "The End" to IslandType.THE_END,
        "Dungeon Hub" to IslandType.DUNGEON_HUB,
        "The Catacombs" to IslandType.CATACOMBS,
        "Winter Island" to IslandType.WINTER,
        "Rift Dimension" to IslandType.THE_RIFT,
    )

    fun parse(text: String): MutableMap<IslandType, MutableMap<String, LorenzVec>> {
        val result = mutableMapOf<IslandType, MutableMap<String, LorenzVec>>()
        var currentIsland: IslandType? = null

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed == "Removed NPCs") break

            sectionToIsland[trimmed]?.let {
                currentIsland = it
                continue
            }

            if (trimmed.contains("⏣ Dwarven Mines") || trimmed.contains("\u23E3 Dwarven Mines")) {
                currentIsland = IslandType.DWARVEN_MINES
                continue
            }

            val island = currentIsland ?: continue

            if (!line.startsWith("\t")) continue

            val fields = line.split("\t").map { it.trim() }
            if (fields.size < 5) continue

            val name = fields.getOrNull(1)?.takeIf { it.isNotBlank() } ?: continue
            if (name.toDoubleOrNull() != null) continue
            if (name == "Name") continue

            val coords = findCoordinateTriple(fields) ?: continue

            result.getOrPut(island) { mutableMapOf() }[name] = coords
        }

        println("parsed ${result.values.sumOf { it.size }} npcs from ${result.size} islands")
        return result
    }

    private fun findCoordinateTriple(fields: List<String>): LorenzVec? {
        for (i in 2..fields.size - 3) {
            val x = parseCoordinate(fields[i]) ?: continue
            val y = parseCoordinate(fields[i + 1]) ?: continue
            val z = parseCoordinate(fields[i + 2]) ?: continue
            return LorenzVec(x, y, z)
        }
        return null
    }

    private fun parseCoordinate(field: String): Double? {
        val first = field.lines().first().trim()
        if (first.isBlank() || first.equals("Various", true) || first.equals("Varies", true)) return null
        return first.toDoubleOrNull()
    }
}
