package at.hannibal2.skyhanni.utils

import net.minecraft.world.phys.Vec3

object VectorUtils {
    /**
     * Compares two vectors for equality, treating -0.0 and 0.0 as equal, unlike [Vec3.equals].
     */
    fun Vec3.isEqualTo(other: Vec3): Boolean = x == other.x && y == other.y && z == other.z
}
