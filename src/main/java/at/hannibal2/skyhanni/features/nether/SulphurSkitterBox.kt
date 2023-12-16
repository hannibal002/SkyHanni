package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class SulphurSkitterBox {

    private val config get() = SkyHanniMod.feature.crimsonIsle.sulphurSkitterBoxConfig

    //private val rodsList = listOf<NEUInternalName>()
    private var blocksList = listOf<LorenzVec>()
    private val radius = 8

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return
        for (i in 0 .. radius) {
            val from = LocationUtils.playerLocation().add(x = -i, y = -i, z = -i).toBlockPos()
            val to = LocationUtils.playerLocation().add(x = i, y = i, z = i).toBlockPos()
            for (blockPos in BlockPos.getAllInBox(from, to)) {
                val b = Minecraft.getMinecraft().theWorld.getBlockState(blockPos).block
                if (b == Blocks.sponge && blockPos.toLorenzVec().distanceToPlayer() <= 15) {
                    blocksList = blocksList.editCopy { add(blockPos.toLorenzVec()) }
                }
            }
        }
    }


    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        blocksList = blocksList.editCopy { removeIf { Minecraft.getMinecraft().theWorld.getBlockState(it.toBlockPos()).block != Blocks.sponge } }
        if (blocksList.isEmpty()) return
        for (block in blocksList) {
            val pos1 = block.add(-radius, -radius, -radius)
            val pos2 = block.add(radius, radius, radius)
            val axis = AxisAlignedBB(pos1.toBlockPos(), pos2.toBlockPos())
            event.drawFilledBoundingBox_nea(axis, Color(SpecialColour.specialToChromaRGB(config.boxColor)), Color(SpecialColour.specialToChromaRGB(config.boxColor)).alpha.toFloat())
        }
    }

    fun isEnabled() = LorenzUtils.skyBlockIsland == IslandType.CRIMSON_ISLE && config.enabled
}