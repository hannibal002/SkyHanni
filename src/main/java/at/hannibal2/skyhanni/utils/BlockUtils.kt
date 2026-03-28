package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.VectorUtils.plus
import at.hannibal2.skyhanni.utils.VectorUtils.roundToBlock
import at.hannibal2.skyhanni.utils.VectorUtils.times
import at.hannibal2.skyhanni.utils.VectorUtils.toBlockPos
import at.hannibal2.skyhanni.utils.VectorUtils.toVec3
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addRedstoneOres
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

object BlockUtils {

    private val world get() = MinecraftCompat.localWorld

    fun Vec3.getBlockAt(): Block = getBlockStateAt().block

    fun Vec3.getBlockStateAt(): BlockState = world.getBlockState(toBlockPos())

    fun Vec3.isInLoadedChunk(): Boolean =
        world.chunkSource.hasChunk(x.toInt() shr 4, z.toInt() shr 4)

    fun getTextureFromSkull(position: Vec3): String? =
        (world.getBlockEntity(position.toBlockPos()) as? SkullBlockEntity)?.getSkullTexture()

    fun SkullBlockEntity.getSkullTexture(): String? =
        ownerProfile?.partialProfile()?.id?.toString()

    fun BlockState.isBabyCrop(): Boolean {
        val property = (block.stateDefinition.properties.find { it.name == "age" } as? IntegerProperty) ?: return false
        return getValue(property) == 0
    }

    private fun raycast(start: Vec3, direction: Vec3, distance: Double = 50.0): Vec3? {
        val target = start + direction.normalize() * distance
        val result = raycast(start, target)

        return result?.location
    }

    fun raycast(start: Vec3, end: Vec3): BlockHitResult? = world.clip(
        ClipContext(
            start,
            end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            MinecraftCompat.localPlayer,
        ),
    )

    fun getTargetedBlock(): Vec3? =
        Minecraft.getInstance().hitResult?.takeIf { it.type == HitResult.Type.BLOCK }
            ?.location?.roundToBlock()

    fun getTargetedBlockAtDistance(distance: Double) = raycast(
        LocationUtils.playerEyeLocation(),
        MinecraftCompat.localPlayer.lookAngle,
        distance,
    )?.roundToBlock()

    private fun nearbyBlocks(center: Vec3, distance: Double): Iterable<BlockPos> =
        BlockPos.betweenClosed(
            center.subtract(distance).toBlockPos(),
            center.add(distance).toBlockPos(),
        )

    fun nearbyBlocks(
        center: Vec3,
        distance: Double,
        radius: Double = distance,
        condition: (BlockState) -> Boolean,
    ): Map<Vec3, BlockState> = nearbyBlocks(center, distance).mapNotNull {
        val loc = it.toVec3()
        val state = loc.getBlockStateAt()
        if (condition(state) && center.distanceTo(loc) <= radius) {
            loc to state
        } else null
    }.toMap()

    fun nearbyBlocks(
        center: Vec3,
        distance: Double,
        radius: Double = distance,
        filter: Block,
    ): Map<Vec3, BlockState> =
        nearbyBlocks(center, distance, radius, condition = { it.block == filter })

    val redstoneOreBlocks = buildList { addRedstoneOres() }
}
