package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.features.hunting.safari.checklist.SafariShard
import at.hannibal2.skyhanni.utils.SkyBlockUtils

enum class SafariBiome(val displayName: String, private val colorCode: String) {
    CAVERN("Cavern", "§6"),
    FOREST("Forest", "§2"),
    HAUNTED("Haunted", "§5"),
    ICY("Icy", "§9"),
    ;

    val formattedName get() = "$colorCode$displayName"

    val shards: List<SafariShard> get() = SafariShard.entries.filter { it.biome == this }


    companion object {
        fun currentArea(): SafariBiome? {
            return when (SkyBlockUtils.graphArea) {
                "Cavern Biome" -> CAVERN
                "Icy Biome" -> ICY
                "Haunted Biome" -> HAUNTED
                "Forest Biome" -> FOREST
                else -> null
            }
        }
    }
}
