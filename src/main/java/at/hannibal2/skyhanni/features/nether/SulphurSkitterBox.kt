package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.crimsonisle.SulphurSkitterBoxConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
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
    private var rodsList = setOf<NEUInternalName>()
    private var blocksList = setOf<BlockPos>()
    private val radius = 8

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return
        if (InventoryUtils.itemInHandId !in rodsList && config.onlyWithRods) return
        val from = LocationUtils.playerLocation().add(-20, -20, -20).toBlockPos()
        val to = LocationUtils.playerLocation().add(20, 20, 20).toBlockPos()
        blocksList = BlockPos.getAllInBox(from, to).filter {
            val b = Minecraft.getMinecraft().theWorld.getBlockState(it).block
            b == Blocks.sponge && it.toLorenzVec().distanceToPlayer() <= 15
        }.filter {
            val pos1 = it.add(-radius, -radius, -radius)
            val pos2 = it.add(radius, radius, radius)
            BlockPos.getAllInBox(pos1, pos2).any { p ->
                Minecraft.getMinecraft().theWorld.getBlockState(p).block in listOf(Blocks.lava, Blocks.flowing_lava)
            }
        }.toSet()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        blocksList = emptySet()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.itemInHandId !in rodsList && config.onlyWithRods) return
        val block = getClosestBlockToPlayer(blocksList) ?: return
        if (block.toLorenzVec().distanceToPlayer() >= 20) return
        val pos1 = block.add(-radius, -radius, -radius)
        val pos2 = block.add(radius, radius, radius)
        val axis = AxisAlignedBB(pos1, pos2)

        drawBox(axis, event.partialTicks)
    }

    private fun getClosestBlockToPlayer(list: Set<BlockPos>): BlockPos? {
        return list.minByOrNull { it.toLorenzVec().distanceToPlayer() }
    }

    private fun drawBox(axis: AxisAlignedBB, partialTicks: Float) {
        when (config.boxType) {
            SulphurSkitterBoxConfig.BoxType.FULL -> {
                RenderUtils.drawFilledBoundingBox_nea(axis.expandBlock(),
                    Color(SpecialColour.specialToChromaRGB(config.boxColor), true),
                    partialTicks = partialTicks,
                    renderRelativeToCamera = false)
            }

            SulphurSkitterBoxConfig.BoxType.WIREFRAME -> {
                RenderUtils.drawWireframeBoundingBox_nea(axis.expandBlock(), Color(SpecialColour.specialToChromaRGB(config.boxColor), true), partialTicks)
            }

            else -> {
                RenderUtils.drawWireframeBoundingBox_nea(axis.expandBlock(), Color(SpecialColour.specialToChromaRGB(config.boxColor), true), partialTicks)
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items")
        rodsList = data.lava_fishing_rods
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.enabled
}