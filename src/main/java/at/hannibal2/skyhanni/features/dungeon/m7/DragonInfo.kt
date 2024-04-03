package at.hannibal2.skyhanni.features.dungeon.m7

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.util.AxisAlignedBB

enum class DragonInfo(
    val color: String,
    var status: SpawnedStatus,
    val isEasy: Boolean,
    val priority: IntArray,
    val particleBox: AxisAlignedBB,
    val deathBox: AxisAlignedBB,
    val spawnLocation: LorenzVec,
    var id: Int,
    val colorCode: Char
) {
    POWER("Red", SpawnedStatus.UNDEFEATED, false, intArrayOf(1, 3), AxisAlignedBB(24.0, 15.0, 30.0, 56.0, 22.0, 62.0), AxisAlignedBB(14.5, 13.0, 45.5, 39.5, 28.0, 70.5), LorenzVec(27.0, 14.0, 59.0), -1, LorenzColor.RED.chatColorCode),
    FLAME("Orange", SpawnedStatus.UNDEFEATED, true, intArrayOf(2, 1), AxisAlignedBB(82.0, 15.0, 88.0, 56.0, 22.0, 62.0), AxisAlignedBB(70.0, 8.0, 47.0, 102.0, 28.0, 77.0), LorenzVec(85.0, 14.0, 56.0), -1, LorenzColor.GOLD.chatColorCode),
    APEX("Green", SpawnedStatus.UNDEFEATED, true, intArrayOf(5, 2), AxisAlignedBB(24.0, 15.0, 30.0, 91.0, 22.0, 97.0), AxisAlignedBB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0), LorenzVec(27.0, 14.0, 94.0), -1, LorenzColor.GREEN.chatColorCode),
    ICE("Blue", SpawnedStatus.UNDEFEATED, false, intArrayOf(3, 4), AxisAlignedBB(82.0, 15.0, 88.0, 91.0, 22.0, 97.0), AxisAlignedBB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5), LorenzVec(84.0, 14.0, 94.0), -1, LorenzColor.AQUA.chatColorCode),
    SOUL("Purple", SpawnedStatus.UNDEFEATED, true, intArrayOf(4, 5), AxisAlignedBB(53.0, 15.0, 59.0, 122.0, 22.0, 128.0), AxisAlignedBB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5), LorenzVec(56.0, 14.0, 125.0), -1, LorenzColor.LIGHT_PURPLE.chatColorCode),
    NONE("None", SpawnedStatus.UNDEFEATED, false, intArrayOf(0, 0), AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), LorenzVec(0.0, 0.0, 0.0), -1, LorenzColor.CHROMA.chatColorCode);

    companion object {
        fun clearSpawned() {
            entries.forEach { it.status = SpawnedStatus.UNDEFEATED }
        }
    }
}

enum class SpawnedStatus {
    UNDEFEATED,
    SPAWNING,
    ALIVE,
    DEFEATED;
}
