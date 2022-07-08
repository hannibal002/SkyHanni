package at.lorenz.mod.utils

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraftforge.common.util.Constants

object BlockUtils {

    fun LorenzVec.getBlockAt(): Block =
        Minecraft.getMinecraft().theWorld.getBlockState(toBlocPos()).block

    fun LorenzVec.isInLoadedChunk(): Boolean =
        Minecraft.getMinecraft().theWorld.chunkProvider.provideChunk(toBlocPos()).isLoaded

    fun getSkinFromSkull(position: BlockPos?): String? {
        val entity = Minecraft.getMinecraft().theWorld.getTileEntity(position) as TileEntitySkull
        val serializeNBT = entity.serializeNBT()
        return serializeNBT.getCompoundTag("Owner").getCompoundTag("Properties")
            .getTagList("textures", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("Value")
    }
}