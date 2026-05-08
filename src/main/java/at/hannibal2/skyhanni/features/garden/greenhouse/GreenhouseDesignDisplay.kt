package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.greenhouse
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

@SkyHanniModule
object GreenhouseDesignDisplay {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    private var data: GreenhouseData? = null

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {


        val plot = GardenPlotApi.getCurrentPlot() ?: return
        if (!plot.greenhouse) return

        4 - 91

        val box = plot.box
        43
        val topLeft = LorenzVec(box.minX - 43, 73.0, box.maxZ - 43)

        val data =
            GreenhouseData("NYs5DoAwEAM_ZK24oc1T1gQIR2ho83jEJnjkZmQHRLQYUKFGgx4TFIRHhzE9afbLuoX9OC9SaY106qy3_hG6gigNpTnNTrP7duUrzDtSXg")
        data.grid.forEach { pos, info ->
            val worldPos = topLeft + LorenzVec(pos.x, 0, pos.y)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        val plot = GardenPlotApi.getCurrentPlot() ?: return
        if (!plot.greenhouse) return
        if (data == null) data =
            GreenhouseData("NYs5DoAwEAM_ZK24oc1T1gQIR2ho83jEJnjkZmQHRLQYUKFGgx4TFIRHhzE9afbLuoX9OC9SaY106qy3_hG6gigNpTnNTrP7duUrzDtSXg")

        data?.let { data ->
            val box = plot.box

            data.grid.forEach { (pos, info) ->
                val topLeft = LorenzVec(box.maxX - 44, 73.0, box.maxZ - 44).roundTo(0)

                val xFix = if (pos.x == 0) 0.001 else -0.001
                val zFix = if (pos.y == 0) 0.001 else -0.001

                var worldPos = topLeft - LorenzVec(pos.x.toDouble() - xFix, 0.0, pos.y.toDouble() - zFix)

                println(info.surface == Blocks.FARMLAND && worldPos.getBlockAt() != Blocks.FARMLAND)
                val yFix = if (info.surface == Blocks.FARMLAND && worldPos.getBlockAt() != Blocks.FARMLAND) 1 / 16 + 0.001 else 0.001
                worldPos = worldPos.add(y = yFix)

                if (info.surface != worldPos.getBlockAt()) event.renderFakeBlock(info.surface.defaultBlockState(), worldPos)
            }
        }
    }

    fun SkyHanniRenderWorldEvent.renderFakeBlock(
        state: BlockState,
        pos: LorenzVec,
    ) {
        val mc = Minecraft.getInstance()
        val dispatcher = mc.blockRenderer

        matrices.pushPose()

        matrices.translate(
            pos.x - camera.position().x,
            pos.y - camera.position().y,
            pos.z - camera.position().z,
        )

        dispatcher.renderSingleBlock(
            state,
            matrices,
            vertexConsumers,
            0xF000F0,
            0,
        )

        matrices.popPose()
    }
}
