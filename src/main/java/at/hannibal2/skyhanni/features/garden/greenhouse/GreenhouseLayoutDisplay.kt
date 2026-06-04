package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.GridPosition
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.LayoutDisplayType
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.SlotInfo
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.getWorldPosition
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.renderFakeBlock
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.renderFakeSkullBlock
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

@SkyHanniModule
object GreenhouseLayoutDisplay {

    private val config get() = SkyHanniMod.feature.garden.greenhouse.designImporter

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return

        val layout = GreenhouseLayoutApi.layout ?: return

        val displayType = config.displayType

        layout.grid.forEach { (gridPos, slotInfo) ->
            val worldPos = getWorldPosition(gridPos) ?: return@forEach
            val actualBlock = worldPos.getBlockAt()

            if (displayType.shouldRenderCrop(slotInfo)) {
                val cropBlock = GreenhouseCropUtils.getCropBlock(slotInfo.crop)
                event.renderMutationOrCrop(worldPos, gridPos, slotInfo, cropBlock, actualBlock, displayType)
            }

            if (displayType.shouldRenderSurface()) {
                event.renderSurface(worldPos, gridPos, slotInfo, actualBlock)
            }
        }
    }

    private fun SkyHanniRenderWorldEvent.renderMutationOrCrop(
        worldPos: LorenzVec,
        gridPos: GridPosition,
        slotInfo: SlotInfo,
        cropBlock: BlockState?,
        actualBlock: Block,
        displayType: LayoutDisplayType,
    ) {
        if (slotInfo.plantedCrop.equals(slotInfo.crop, ignoreCase = true)) return

        if (config.showTextLabels) {
            drawString(
                worldPos.add(x = 0.5, y = 2.125, z = 0.5),
                "${slotInfo.type.color.getChatColor()}${slotInfo.crop}",
                scale = 0.53333333 / 2,
                yOffset = -9f,
            )
        }

        if (cropBlock != null) {
            renderCrop(gridPos, slotInfo, cropBlock, actualBlock, displayType)
        } else {
            renderMutation(worldPos, slotInfo)
        }
    }

    private fun SkyHanniRenderWorldEvent.renderCrop(
        gridPos: GridPosition,
        slotInfo: SlotInfo,
        cropBlock: BlockState,
        actualBlock: Block,
        displayType: LayoutDisplayType,
    ) {
        val surface = slotInfo.surface
        val fakeSurfacePos = GreenhouseLayoutApi.getFakeSurfacePosition(gridPos, surface) ?: return

        val isFarmlandOffset = (displayType == LayoutDisplayType.ALL && surface == Blocks.FARMLAND) || actualBlock == Blocks.FARMLAND
        val dy = if (isFarmlandOffset) 0.0625 else 0.0

        renderFakeBlock(cropBlock, fakeSurfacePos.add(y = 1.0 - dy), OverlayTexture.RED_OVERLAY_V)
    }

    private fun SkyHanniRenderWorldEvent.renderMutation(worldPos: LorenzVec, slotInfo: SlotInfo) {
        val skull = GreenhouseCropUtils.getMutationSkull(slotInfo.crop)
        val profile = skull.get(DataComponents.PROFILE) ?: return

        renderFakeSkullBlock(profile, worldPos.add(0.5, 1.5, 0.5), OverlayTexture.RED_OVERLAY_V)
    }

    private fun SkyHanniRenderWorldEvent.renderSurface(
        worldPos: LorenzVec,
        gridPos: GridPosition,
        slotInfo: SlotInfo,
        actualBlock: Block,
    ) {
        val surface = slotInfo.surface
        if (surface == actualBlock) return

        val fakeSurfacePos = GreenhouseLayoutApi.getFakeSurfacePosition(gridPos, surface) ?: return
        renderFakeBlock(surface.defaultBlockState(), fakeSurfacePos, OverlayTexture.RED_OVERLAY_V)

        if (config.showTextLabels) {
            val text = when (surface.name) {
                Blocks.SAND -> "Sand"
                Blocks.SOUL_SAND -> "Soul Sand"
                Blocks.MYCELIUM -> "Mycelium"
                else -> "Farmland"
            }

            drawString(
                worldPos.add(x = 0.5, y = 2.125, z = 0.5),
                "§c$text",
                scale = 0.53333333 / 2,
            )
        }
    }
}
