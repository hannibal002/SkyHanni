package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SkyBlockUtils

enum class SafariBiome(val displayName: String, colorCode: String) {
    CAVERN("Cavern", "§6"),
    FOREST("Forest", "§2"),
    HAUNTED("Haunted", "§5"),
    ICY("Icy", "§9"),
    ;

    val formattedName = "$colorCode$displayName"

    val waypointName = "$formattedName Biome"

    val shards: List<SafariShard> get() = SafariShard.entries.filter { it.biome == this }

    companion object {
        fun currentArea(): SafariBiome? {
            return when (val area = SkyBlockUtils.graphArea) {
                "Cavern Biome" -> CAVERN
                "Icy Biome" -> ICY
                "Haunted Biome" -> HAUNTED
                "Forest Biome" -> FOREST
                AreaNode.NO_AREA, "", null -> null
                else -> {
                    ErrorManager.logErrorStateWithData(
                        "Unknown Safari biome detected.",
                        "Unknown Safari graph area.",
                        "area" to area,
                    )
                    null
                }
            }
        }
    }
}
