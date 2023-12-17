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
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
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
    private val rodsList = mutableListOf<String>()
    private var blocksList = listOf<LorenzVec>()
    private val radius = 8

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return
        if (InventoryUtils.itemInHandId.asString() !in rodsList && config.onlyWithRods) return
        blocksList = emptyList()
        val from = LocationUtils.playerLocation().add(-20, -20, -20).toBlockPos()
        val to = LocationUtils.playerLocation().add(20, 20, 20).toBlockPos()
        for (blockPos in BlockPos.getAllInBox(from, to)) {
            val b = Minecraft.getMinecraft().theWorld.getBlockState(blockPos).block
            if (b == Blocks.sponge && blockPos.toLorenzVec().distanceToPlayer() <= 15) {
                blocksList = blocksList.editCopy { add(blockPos.toLorenzVec()) }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        blocksList = emptyList()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.itemInHandId.asString() !in rodsList && config.onlyWithRods) return
        val block = getClosestBlockToPlayer(blocksList) ?: return
        if (block.distanceToPlayer() >= 20) return
        val pos1 = block.add(-radius, -radius, -radius).toBlockPos()
        val pos2 = block.add(radius, radius, radius).toBlockPos()
        val axis = AxisAlignedBB(pos1, pos2)
        if (BlockPos.getAllInBox(pos1, pos2).any { Minecraft.getMinecraft().theWorld.getBlockState(it).block in listOf(Blocks.lava, Blocks.flowing_lava) }) {
            drawBox(axis, event.partialTicks)
        } else {
            event.drawDynamicText(block.add(y = 1), "§cNot near lava :(", 1.0)
        }
    }

    private fun getClosestBlockToPlayer(list: List<LorenzVec>): LorenzVec? {
        var map = mapOf<Double, LorenzVec>()
        for (block in list) {
            map = map.editCopy {
                put(block.distanceToPlayer(), block)
            }
        }
        return map.entries.minByOrNull { it.key }?.value
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
        rodsList.clear()
        rodsList.addAll(data.lava_fishing_rods)
    }

    fun isEnabled() = LorenzUtils.skyBlockIsland == IslandType.CRIMSON_ISLE && config.enabled
}