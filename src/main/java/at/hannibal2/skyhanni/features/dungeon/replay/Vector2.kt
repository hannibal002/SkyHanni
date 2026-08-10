package at.hannibal2.skyhanni.features.dungeon.replay

import net.minecraft.world.phys.Vec2

data class Vector2(
    val x: Float = 0f,
    val y: Float = 0f
) {
    companion object {
        fun Vec2.toVector2(): Vector2 = Vector2(this.x, this.y)
    }

    operator fun minus(vector: Vector2): Vector2 = Vector2(x - vector.x, y - vector.y)
}
