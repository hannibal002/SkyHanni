package at.hannibal2.skyhanni.utils

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

object BlockUtils {

    private val world get() = MinecraftCompat.localWorld

    fun LorenzVec.getBlockAt(): Block = getBlockStateAt().block

    fun LorenzVec.getBlockStateAt(): BlockState = world.getBlockState(toBlockPos())

    fun LorenzVec.isInLoadedChunk(): Boolean =
        world.chunkSource.hasChunk(x.toInt() shr 4, z.toInt() shr 4)

    fun getTextureFromSkull(position: LorenzVec): String? {
        val entity = world.getBlockEntity(position.toBlockPos()) as? SkullBlockEntity ?: return null
        return entity.getSkullTexture()
    }

    fun SkullBlockEntity.getSkullTexture(): String? {
        return this.ownerProfile?.partialProfile()?.id?.toString()
    }

    fun BlockState.isBabyCrop(): Boolean {
        val property = (block.stateDefinition.properties.find { it.name == "age" } as? IntegerProperty) ?: return false
        return getValue(property) == 0
    }

    private fun raycast(start: LorenzVec, direction: LorenzVec, distance: Double = 50.0): LorenzVec? {
        val target = start + direction.normalize() * distance
        val result = raycast(start, target)

        return result?.location?.toLorenzVec()
    }

    fun raycast(start: LorenzVec, end: LorenzVec): BlockHitResult? = world.clip(
        ClipContext(
            start.toVec3(),
            end.toVec3(),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            MinecraftCompat.localPlayer,
        ),
    )

    fun getTargetedBlock(): LorenzVec? =
        Minecraft.getInstance().hitResult?.takeIf { it.type == HitResult.Type.BLOCK }
            ?.location?.toLorenzVec()?.roundToBlock()

    fun getTargetedBlockAtDistance(distance: Double) = raycast(
        LocationUtils.playerEyeLocation(),
        MinecraftCompat.localPlayer.lookAngle.toLorenzVec(),
        distance,
    )?.roundToBlock()

    private fun nearbyBlocks(center: LorenzVec, distance: Int): MutableIterable<BlockPos> {
        val from = center.add(-distance, -distance, -distance).toBlockPos()
        val to = center.add(distance, distance, distance).toBlockPos()
        return BlockPos.betweenClosed(from, to)
    }

    fun nearbyBlocks(
        center: LorenzVec,
        distance: Int,
        radius: Int = distance,
        condition: (BlockState) -> Boolean,
    ): Map<LorenzVec, BlockState> = nearbyBlocks(center, distance).mapNotNull {
        val loc = it.toLorenzVec()
        val state = loc.getBlockStateAt()
        if (condition(state) && center.distance(loc) <= radius) {
            loc to state
        } else null
    }.toMap()

    fun nearbyBlocks(
        center: LorenzVec,
        distance: Int,
        radius: Int = distance,
        filter: Block,
    ): Map<LorenzVec, BlockState> =
        nearbyBlocks(center, distance, radius, condition = { it.block == filter })

    val redstoneOreBlocks = buildList { addRedstoneOres() }
}
