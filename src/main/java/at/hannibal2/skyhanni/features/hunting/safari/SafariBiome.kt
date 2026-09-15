package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.features.hunting.safari.checklist.SafariShard
import at.hannibal2.skyhanni.utils.LorenzVec

enum class SafariBiome(val displayName: String, private val colorCode: String) {
    CAVERN("Cavern", "§6"),
    FOREST("Forest", "§2"),
    HAUNTED("Haunted", "§5"),
    ICY("Icy", "§9"),
    ;

    val formattedName get() = "$colorCode$displayName"

    val shards: List<SafariShard> get() = SafariShard.entries.filter { it.biome == this }


    companion object {
        fun currentArea(playerLocation: LorenzVec): SafariBiome? {
            val centerX = -49.5
            val centerZ = 0.5
            return when {
                playerLocation.x < centerX && playerLocation.z < centerZ -> ICY
                playerLocation.x < centerX && playerLocation.z >= centerZ -> CAVERN
                playerLocation.x >= centerX && playerLocation.z >= centerZ -> FOREST
                playerLocation.x >= centerX && playerLocation.z < centerZ -> HAUNTED
                else -> throw IllegalStateException("Player is not in any known safari biome.")
            }
        }
    }

}
