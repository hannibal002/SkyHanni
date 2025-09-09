package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addRedstoneOres
import net.minecraft.block.Block
import net.minecraft.state.property.IntProperty
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.block.entity.SkullBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.hit.HitResult

//#if MC > 1.21
import net.minecraft.world.RaycastContext
//#endif

object BlockUtils {

    private val world get() = MinecraftCompat.localWorld

    fun LorenzVec.getBlockAt(): Block = getBlockStateAt().block

    fun LorenzVec.getBlockStateAt(): BlockState = world.getBlockState(toBlockPos())

    //#if MC < 1.21
    //$$ fun LorenzVec.isInLoadedChunk(): Boolean = world.isBlockLoaded(toBlockPos(), false)
    //#else
    fun LorenzVec.isInLoadedChunk(): Boolean =
    world.chunkManager.isChunkLoaded(x.toInt() shr 4, z.toInt() shr 4)
    //#endif

    fun getTextureFromSkull(position: LorenzVec): String? {
        val entity = world.getBlockEntity(position.toBlockPos()) as? SkullBlockEntity ?: return null
        return entity.getSkullTexture()
    }

    fun SkullBlockEntity.getSkullTexture(): String? {
        //#if MC < 1.21
        //$$ return this.serializeNBT().getCompound("Owner").getSkullTexture()
        //#else
        return this.owner?.uuid()?.get()?.toString()
        //#endif
    }

    fun BlockState.isBabyCrop(): Boolean {
        val property = (block.stateManager.properties.find { it.name == "age" } as? IntProperty) ?: return false
        return get(property) == 0
    }

    private fun rayTrace(start: LorenzVec, direction: LorenzVec, distance: Double = 50.0): LorenzVec? {
        val target = start + direction.normalize() * distance
        val result = rayTrace(start, target)

        return result?.pos?.toLorenzVec()
    }

    //#if MC < 1.21
    //$$ fun rayTrace(start: LorenzVec, end: LorenzVec): HitResult? {
    //$$     return world.rayTraceBlocks(start.toVec3(), end.toVec3())
    //$$ }
    //#else
    fun rayTrace(start: LorenzVec, end: LorenzVec): net.minecraft.util.hit.BlockHitResult? {
       return world.raycast(
           RaycastContext(
               start.toVec3(),
               end.toVec3(),
               RaycastContext.ShapeType.COLLIDER,
               RaycastContext.FluidHandling.NONE,
               MinecraftCompat.localPlayer,
           ),
       )
    }
    //#endif

    fun getTargetedBlock(): LorenzVec? {
        val mouseOverObject = MinecraftClient.getInstance().crosshairTarget ?: return null
        if (mouseOverObject.type != HitResult.Type.BLOCK) return null
        return mouseOverObject.pos.toLorenzVec().roundToBlock()
    }

    fun getTargetedBlockAtDistance(distance: Double) = rayTrace(
        LocationUtils.playerEyeLocation(),
        MinecraftCompat.localPlayer.rotationVector.toLorenzVec(),
        distance,
    )?.roundToBlock()

    private fun nearbyBlocks(center: LorenzVec, distance: Int): MutableIterable<BlockPos> {
        val from = center.add(-distance, -distance, -distance).toBlockPos()
        val to = center.add(distance, distance, distance).toBlockPos()
        return BlockPos.iterate(from, to)
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
    ): Map<LorenzVec, BlockState> = nearbyBlocks(center, distance, radius, condition = { it.block == filter })

    val redstoneOreBlocks = buildList { addRedstoneOres() }
}
