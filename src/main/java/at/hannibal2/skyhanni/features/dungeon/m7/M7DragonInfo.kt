package at.hannibal2.skyhanni.features.dungeon.m7

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.util.AxisAlignedBB

enum class M7DragonInfo(
    val color: String,
    val isEasy: Boolean,
    val priority: IntArray,
    val dragonLocation: M7DragonLocation,
    val colorCode: Char,
    var status: M7SpawnedStatus = M7SpawnedStatus.UNDEFEATED
) {
    POWER(
        "Red",
        false,
        intArrayOf(1, 3),
        M7DragonLocation.POWER,
        LorenzColor.RED.chatColorCode
    ),
    FLAME(
        "Orange",
        true,
        intArrayOf(2, 1),
        M7DragonLocation.FLAME,
        LorenzColor.GOLD.chatColorCode
    ),
    APEX(
        "Green",
        true,
        intArrayOf(5, 2),
        M7DragonLocation.APEX,
        LorenzColor.GREEN.chatColorCode
    ),
    ICE(
        "Blue",
        false,
        intArrayOf(3, 4),
        M7DragonLocation.ICE,
        LorenzColor.AQUA.chatColorCode
    ),
    SOUL(
        "Purple",
        true,
        intArrayOf(4, 5),
        M7DragonLocation.SOUL,
        LorenzColor.LIGHT_PURPLE.chatColorCode
    );

    companion object {
        fun clearSpawned() {
            entries.forEach {
                it.status = M7SpawnedStatus.UNDEFEATED
                it.status.id = -1
            }
        }
    }
}

enum class M7SpawnedStatus(var id: Int = -1) {
    UNDEFEATED,
    SPAWNING,
    ALIVE,
    DEFEATED;
}

enum class M7DragonLocation(
    val particleBox: AxisAlignedBB,
    val deathBox: AxisAlignedBB,
    val spawnLocation: LorenzVec
) {
    POWER(
        AxisAlignedBB(24.0, 15.0, 30.0, 56.0, 22.0, 62.0),
        AxisAlignedBB(14.0, 13.0, 46.0, 39.0, 28.0, 71.0),
        LorenzVec(27.0, 14.0, 59.0),
    ),
    FLAME(
        AxisAlignedBB(82.0, 15.0, 88.0, 53.0, 22.0, 59.0),
        AxisAlignedBB(70.0, 8.0, 47.0, 102.0, 28.0, 77.0),
        LorenzVec(83.0, 15.0, 56.0),
    ),
    APEX(
        AxisAlignedBB(24.0, 15.0, 30.0, 91.0, 22.0, 97.0),
        AxisAlignedBB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0),
        LorenzVec(26.0, 15.0, 92.0)
    ),
    ICE(
        AxisAlignedBB(82.0, 15.0, 88.0, 91.0, 22.0, 97.0),
        AxisAlignedBB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5),
        LorenzVec(83.0, 14.0, 94.0)
    ),
    SOUL(
        AxisAlignedBB(53.0, 15.0, 59.0, 122.0, 22.0, 128.0),
        AxisAlignedBB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5),
        LorenzVec(56.0, 14.0, 125.0)
    );
}
