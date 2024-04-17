package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.RenderUtils.inflateBlock
import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class CrystalHollowsWalls {

    private val config get() = SkyHanniMod.feature.mining.crystalHollowsAreaWalls

    fun isEnabled() = config.enabled && IslandType.CRYSTAL_HOLLOWS.isInIsland()

    private enum class Areas(val color: Color) {
        MITHRIL(LorenzColor.GREEN.addOpacity(60)),
        PRECURSOR(LorenzColor.BLUE.addOpacity(60)),
        JUNGLE(LorenzColor.LIGHT_PURPLE.addOpacity(60)),
        GOBLIN(LorenzColor.GOLD.addOpacity(60)),
        HEAT(LorenzColor.RED.addOpacity(60)),
        NUCLEUS(LorenzColor.WHITE.addOpacity(60))
        ;
    }

    private val expandTimes = 20

    private val heatHeight = 64.0
    private val maxHeight = 190.0

    private val minX = 0.0
    private val middleX = 513.0
    private val maxX = 1024.0

    private val minZ = 0.0
    private val middleZ = 513.0
    private val maxZ = 1024.0

    private val yViewOffset get() = -Minecraft.getMinecraft().thePlayer.getEyeHeight().toDouble()

    // Yes Hypixel has misaligned the nucleus
    private val nucleusBB = AxisAlignedBB(
        463.0, heatHeight, 460.0,
        560.0, maxHeight, 563.0
    )

    private val nucleusBBInflate = nucleusBB.inflateBlock(expandTimes)
    private val nucleusBBExpand = nucleusBB.expandBlock(expandTimes)

    private val nucleusBBOffsetY get() = nucleusBB.offset(0.0, yViewOffset, 0.0)

    private fun Double.shiftPX() = this + LorenzVec.expandVector.x * expandTimes
    private fun Double.shiftNX() = this - LorenzVec.expandVector.x * expandTimes

    private fun Double.shiftPY() = this + LorenzVec.expandVector.y * expandTimes
    private fun Double.shiftNY() = this - LorenzVec.expandVector.y * expandTimes

    private fun Double.shiftPZ() = this + LorenzVec.expandVector.z * expandTimes
    private fun Double.shiftNZ() = this - LorenzVec.expandVector.z * expandTimes

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        val position = RenderUtils.getViewerPos(event.partialTicks)
        if (position.y < heatHeight + yViewOffset) {
            drawHeat(event)
        } else if (nucleusBBOffsetY.isVecInside(position.toVec3())) {
            if (!config.nucleus) return
            drawNucleus(event)
        } else if (position.x > middleX) {
            if (position.z > middleZ) {
                drawPrecursor(event)
            } else {
                drawMithril((event))
            }
        } else {
            if (position.z > middleZ) {
                drawGoblin(event)
            } else {
                drawJungle(event)
            }
        }
    }

    private fun drawGoblin(event: LorenzRenderWorldEvent) = RenderUtils.QuadDrawer.draw3D(event.partialTicks) {
        drawArea(true, false, Areas.JUNGLE.color, Areas.PRECURSOR.color)
    }

    private fun drawJungle(event: LorenzRenderWorldEvent) = RenderUtils.QuadDrawer.draw3D(event.partialTicks) {
        drawArea(true, true, Areas.GOBLIN.color, Areas.MITHRIL.color)
    }

    private fun drawPrecursor(event: LorenzRenderWorldEvent) = RenderUtils.QuadDrawer.draw3D(event.partialTicks) {
        drawArea(false, false, Areas.MITHRIL.color, Areas.GOBLIN.color)
    }

    private fun drawMithril(event: LorenzRenderWorldEvent) = RenderUtils.QuadDrawer.draw3D(event.partialTicks) {
        drawArea(false, true, Areas.PRECURSOR.color, Areas.JUNGLE.color)
    }

    private fun drawHeat(event: LorenzRenderWorldEvent) = RenderUtils.QuadDrawer.draw3D(event.partialTicks) {
        val heatHeight = heatHeight.shiftNY()
        draw(
            LorenzVec(nucleusBB.minX, heatHeight, nucleusBB.minZ),
            LorenzVec(nucleusBB.maxX, heatHeight, nucleusBB.minZ),
            LorenzVec(nucleusBB.minX, heatHeight, nucleusBB.maxZ),
            Areas.NUCLEUS.color
        )

        drawHeatAreaForHeat(false, false, Areas.PRECURSOR.color, heatHeight)
        drawHeatAreaForHeat(false, true, Areas.MITHRIL.color, heatHeight)
        drawHeatAreaForHeat(true, false, Areas.GOBLIN.color, heatHeight)
        drawHeatAreaForHeat(true, true, Areas.JUNGLE.color, heatHeight)
    }

    private fun drawNucleus(event: LorenzRenderWorldEvent) {
        val southEastCorner = LorenzVec(nucleusBBInflate.minX, nucleusBBInflate.minY, nucleusBBInflate.minZ)
        val southWestCorner = LorenzVec(nucleusBBInflate.minX, nucleusBBInflate.minY, nucleusBBInflate.maxZ)
        val northEastCorner = LorenzVec(nucleusBBInflate.maxX, nucleusBBInflate.minY, nucleusBBInflate.minZ)
        val northWestCorner = LorenzVec(nucleusBBInflate.maxX, nucleusBBInflate.minY, nucleusBBInflate.maxZ)

        val southWestTopCorner = LorenzVec(nucleusBBInflate.minX, nucleusBBInflate.maxY, nucleusBBInflate.maxZ)
        val southEastTopCorner = LorenzVec(nucleusBBInflate.minX, nucleusBBInflate.maxY, nucleusBBInflate.minZ)
        val northEastTopCorner = LorenzVec(nucleusBBInflate.maxX, nucleusBBInflate.maxY, nucleusBBInflate.minZ)
        val northWestTopCorner = LorenzVec(nucleusBBInflate.maxX, nucleusBBInflate.maxY, nucleusBBInflate.maxZ)

        RenderUtils.QuadDrawer.draw3D(event.partialTicks) {
            draw(
                southEastCorner,
                southWestCorner,
                northEastCorner,
                Areas.HEAT.color
            )
            draw(
                southEastCorner,
                southEastTopCorner,
                LorenzVec(nucleusBBInflate.minX, nucleusBBInflate.minY, middleZ),
                Areas.JUNGLE.color
            )
            draw(
                southEastCorner,
                southEastTopCorner,
                LorenzVec(middleX, nucleusBBInflate.minY, nucleusBBInflate.minZ),
                Areas.JUNGLE.color
            )
            draw(
                northWestCorner,
                northWestTopCorner,
                LorenzVec(nucleusBBInflate.maxX, nucleusBBInflate.minY, middleZ),
                Areas.PRECURSOR.color
            )
            draw(
                northWestCorner,
                northWestTopCorner,
                LorenzVec(middleX, nucleusBBInflate.minY, nucleusBBInflate.maxZ),
                Areas.PRECURSOR.color
            )
            draw(
                southWestCorner,
                southWestTopCorner,
                LorenzVec(nucleusBBInflate.minX, nucleusBBInflate.minY, middleZ),
                Areas.GOBLIN.color,
            )
            draw(
                southWestCorner,
                southWestTopCorner,
                LorenzVec(middleX, nucleusBBInflate.minY, nucleusBBInflate.maxZ),
                Areas.GOBLIN.color
            )
            draw(
                northEastCorner,
                northEastTopCorner,
                LorenzVec(nucleusBBInflate.maxX, nucleusBBInflate.minY, middleZ),
                Areas.MITHRIL.color
            )
            draw(
                northEastCorner,
                northEastTopCorner,
                LorenzVec(middleX, nucleusBBInflate.minY, nucleusBBInflate.minZ),
                Areas.MITHRIL.color
            )
        }
    }

    private fun RenderUtils.QuadDrawer.drawArea(
        isMinXEsleMaxX: Boolean,
        isMinZElseMaxZ: Boolean,
        color1: Color,
        color2: Color
    ) {
        val nucleusX = if (isMinXEsleMaxX) nucleusBBExpand.minX else nucleusBBExpand.maxX
        val middleX = if (isMinXEsleMaxX) middleX.shiftNX() else middleX.shiftPX()
        val x = if (isMinXEsleMaxX) minX else maxX

        val nucleusZ = if (isMinZElseMaxZ) nucleusBBExpand.minZ else nucleusBBExpand.maxZ
        val middleZ = if (isMinZElseMaxZ) middleZ.shiftNZ() else middleZ.shiftPZ()
        val z = if (isMinZElseMaxZ) minZ else maxZ

        val heatHeight = heatHeight.shiftPY()

        val nucleusBase = LorenzVec(nucleusX, heatHeight, nucleusZ)

        val nucleusZSideBase = LorenzVec(middleX, heatHeight, nucleusZ)
        val nucleusXSideBase = LorenzVec(nucleusX, heatHeight, middleZ)

        drawHeatArea(
            Areas.HEAT.color,
            heatHeight,
            nucleusX,
            middleX,
            x,
            nucleusZ,
            middleZ,
            z
        )
        draw(
            nucleusXSideBase,
            LorenzVec(nucleusX, maxHeight, middleZ),
            LorenzVec(x, heatHeight, middleZ),
            color1,
        )
        draw(
            nucleusZSideBase,
            LorenzVec(middleX, maxHeight, nucleusZ),
            LorenzVec(middleX, heatHeight, z),
            color2,
        )
        draw(
            nucleusXSideBase,
            nucleusBase,
            LorenzVec(nucleusX, maxHeight, middleZ),
            Areas.NUCLEUS.color,
        )
        draw(
            nucleusZSideBase,
            nucleusBase,
            LorenzVec(middleX, maxHeight, nucleusZ),
            Areas.NUCLEUS.color,
        )
    }

    private fun RenderUtils.QuadDrawer.drawHeatAreaForHeat(
        isMinXEsleMaxX: Boolean,
        isMinZElseMaxZ: Boolean,
        color: Color,
        heatHeight: Double,
    ) = this.drawHeatArea(
        color,
        heatHeight,
        nucleusX = if (isMinXEsleMaxX) nucleusBB.minX else nucleusBB.maxX,
        middleX = if (isMinXEsleMaxX) middleX else middleX,
        x = if (isMinXEsleMaxX) minX else maxX,
        nucleusZ = if (isMinZElseMaxZ) nucleusBB.minZ else nucleusBB.maxZ,
        middleZ = if (isMinZElseMaxZ) middleX else middleX,
        z = if (isMinZElseMaxZ) minZ else maxZ,
    )

    private fun RenderUtils.QuadDrawer.drawHeatArea(
        color: Color,
        heatHeight: Double,
        nucleusX: Double,
        middleX: Double,
        x: Double,
        nucleusZ: Double,
        middleZ: Double,
        z: Double,
    ) {
        val nucleusBase = LorenzVec(nucleusX, heatHeight, nucleusZ)

        draw(
            nucleusBase,
            LorenzVec(nucleusX, heatHeight, z),
            LorenzVec(middleX, heatHeight, nucleusZ),
            color,
        )
        draw(
            nucleusBase,
            LorenzVec(x, heatHeight, nucleusZ),
            LorenzVec(nucleusX, heatHeight, middleZ),
            color,
        )
        draw(
            nucleusBase,
            LorenzVec(x, heatHeight, nucleusZ),
            LorenzVec(nucleusX, heatHeight, z),
            color,
        )
    }

}
