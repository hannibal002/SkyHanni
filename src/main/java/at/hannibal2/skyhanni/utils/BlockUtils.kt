package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addRedstoneOres
import net.minecraft.block.Block
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos

object BlockUtils {

    private val world get() = MinecraftCompat.localWorld

    fun LorenzVec.getBlockAt(): Block = getBlockStateAt().block

    fun LorenzVec.getBlockStateAt(): IBlockState = world.getBlockState(toBlockPos())

    //#if MC < 1.21
    fun LorenzVec.isInLoadedChunk(): Boolean = world.isBlockLoaded(toBlockPos(), false)
    //#else
    //$$ fun LorenzVec.isInLoadedChunk(): Boolean =
    //$$ world.chunkManager.isChunkLoaded(x.toInt() shr 4, z.toInt() shr 4)
    //#endif

    fun getTextureFromSkull(position: LorenzVec): String? {
        val entity = world.getTileEntity(position.toBlockPos()) as? TileEntitySkull ?: return null
        return entity.getSkullTexture()
    }

    fun TileEntitySkull.getSkullTexture(): String? {
        //#if MC < 1.21
        return this.serializeNBT().getCompoundTag("Owner").getSkullTexture()
        //#else
        //$$ return this.owner?.id?.get()?.toString()
        //#endif
    }

    fun IBlockState.isBabyCrop(): Boolean {
        val property = (block.blockState.properties.find { it.name == "age" } as? PropertyInteger) ?: return false
        return getValue(property) == 0
    }

    fun rayTrace(start: LorenzVec, direction: LorenzVec, distance: Double = 50.0): LorenzVec? {
        val target = start + direction.normalize() * distance
        val result = world.rayTraceBlocks(start.toVec3(), target.toVec3())

        return result?.blockPos?.toLorenzVec()
    }

    fun getBlockLookingAt(distance: Double = 10.0) = rayTrace(
        LocationUtils.playerEyeLocation(),
        MinecraftCompat.localPlayer.lookVec.toLorenzVec(),
        distance,
    )

    fun nearbyBlocks(center: LorenzVec, distance: Int): MutableIterable<BlockPos> {
        val from = center.add(-distance, -distance, -distance).toBlockPos()
        val to = center.add(distance, distance, distance).toBlockPos()
        return BlockPos.getAllInBox(from, to)
    }

    fun nearbyBlocks(
        center: LorenzVec,
        distance: Int,
        radius: Int = distance,
        condition: (IBlockState) -> Boolean,
    ): Map<LorenzVec, IBlockState> = nearbyBlocks(center, distance).mapNotNull {
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
    ): Map<LorenzVec, IBlockState> = nearbyBlocks(center, distance, radius, condition = { it.block == filter })

    val redstoneOreBlocks = buildList { addRedstoneOres() }
}
