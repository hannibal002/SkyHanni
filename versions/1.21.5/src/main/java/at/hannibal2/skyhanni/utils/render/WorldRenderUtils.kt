package at.hannibal2.skyhanni.utils.render

//import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
//import at.hannibal2.skyhanni.features.misc.PatcherFixes
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.LocationUtils.getCornersAtHeight
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.expand
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BufferBuilder
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import java.awt.Color

object WorldRenderUtils {

    private val beaconBeam = createResourceLocation("textures/entity/beacon_beam.png")

    private fun SkyHanniRenderWorldEvent.renderBeaconBeam(
        x: Double,
        y: Double,
        z: Double,
        rgb: Int,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawColor(
        location: LorenzVec,
        color: Color,
        beacon: Boolean = false,
        alpha: Float = -1f,
        seeThroughBlocks: Boolean = true,
    ) {
        drawColor(location, color, beacon, alpha, seeThroughBlocks)
    }

    fun SkyHanniRenderWorldEvent.drawColor(
        location: LorenzVec,
        color: LorenzColor,
        beacon: Boolean = false,
        alpha: Float = -1f,
        seeThroughBlocks: Boolean = true,
    ) {
        drawColor(location, color.toColor(), beacon, alpha, seeThroughBlocks)
    }

    fun SkyHanniRenderWorldEvent.drawColor(
        location: LorenzVec,
        color: Color,
        beacon: Boolean = false,
        alpha: Float = -1f,
        seeThroughBlocks: Boolean = true,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawWaypointFilled(
        location: LorenzVec,
        color: Color,
        seeThroughBlocks: Boolean = false,
        beacon: Boolean = false,
        extraSize: Double = 0.0,
        extraSizeTopY: Double = extraSize,
        extraSizeBottomY: Double = extraSize,
        minimumAlpha: Float = 0.2f,
        inverseAlphaScale: Boolean = false,
    ) {
        drawWaypointFilled(
            location,
            color,
            seeThroughBlocks,
            beacon,
            extraSize,
            extraSizeTopY,
            extraSizeBottomY,
            minimumAlpha,
            inverseAlphaScale,
        )
    }

    fun SkyHanniRenderWorldEvent.drawWaypointFilled(
        location: LorenzVec,
        color: Color,
        seeThroughBlocks: Boolean = false,
        beacon: Boolean = false,
        extraSize: Double = 0.0,
        extraSizeTopY: Double = extraSize,
        extraSizeBottomY: Double = extraSize,
        minimumAlpha: Float = 0.2f,
        inverseAlphaScale: Boolean = false,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawFilledBoundingBox(
        aabb: Box,
        c: Color,
        alphaMultiplier: Float = 1f,
        /**
         * If set to `true`, renders the box relative to the camera instead of relative to the world.
         * If set to `false`, will be relativized to [WorldRenderUtils.getViewerPos].
         */
        renderRelativeToCamera: Boolean = false,
        drawVerticalBarriers: Boolean = true,
    ) {
        drawFilledBoundingBox(aabb, c, alphaMultiplier, renderRelativeToCamera, drawVerticalBarriers)
    }

    fun SkyHanniRenderWorldEvent.drawFilledBoundingBox(
        aabb: Box,
        c: Color,
        alphaMultiplier: Float = 1f,
        /**
         * If set to `true`, renders the box relative to the camera instead of relative to the world.
         * If set to `false`, will be relativized to [WorldRenderUtils.getViewerPos].
         */
        renderRelativeToCamera: Boolean = false,
        drawVerticalBarriers: Boolean = true,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawString(
        location: LorenzVec,
        text: String,
        seeThroughBlocks: Boolean = false,
        color: Color? = null,
    ) {
        drawString(location, text, seeThroughBlocks, color)
    }

    fun SkyHanniRenderWorldEvent.drawString(
        location: LorenzVec,
        text: String,
        seeThroughBlocks: Boolean = false,
        color: Color? = null,
    ) {
        TODO()
    }

    private fun SkyHanniRenderWorldEvent.drawNametag(str: String, color: Color?) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawCircle(
        entity: Entity,
        rad: Double,
        color: Color,
    ) {
        drawCircle(entity, rad, color)
    }

    fun SkyHanniRenderWorldEvent.drawCircle(entity: Entity, rad: Double, color: Color) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawCylinderInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        height: Float,
    ) {
        drawCylinderInWorld(color, x, y, z, radius, height)
    }

    fun SkyHanniRenderWorldEvent.drawCylinderInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        height: Float,
    ) {
        drawCylinderInWorld(color, location.x, location.y, location.z, radius, height)
    }

    fun SkyHanniRenderWorldEvent.drawCylinderInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        height: Float,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawPyramid(
        topPoint: LorenzVec,
        baseCenterPoint: LorenzVec,
        baseEdgePoint: LorenzVec,
        color: Color,
        depth: Boolean = true,
    ) {
        drawPyramid(topPoint, baseCenterPoint, baseEdgePoint, color, depth)
    }

    fun SkyHanniRenderWorldEvent.drawPyramid(
        topPoint: LorenzVec,
        baseCenterPoint: LorenzVec,
        baseEdgePoint: LorenzVec,
        color: Color,
        depth: Boolean = true,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawSphereInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
    ) {
        drawSphereInWorld(color, x, y, z, radius)
    }

    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
    ) {
        drawSphereInWorld(color, location.x, location.y, location.z, radius)
    }

    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawSphereWireframeInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
    ) {
        drawSphereWireframeInWorld(color, x, y, z, radius)
    }

    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
    ) {
        drawSphereWireframeInWorld(color, location.x, location.y, location.z, radius)
    }

    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawDynamicText(
        location: LorenzVec,
        text: String,
        scaleMultiplier: Double,
        yOff: Float = 0f,
        hideTooCloseAt: Double = 4.5,
        smallestDistanceVew: Double = 5.0,
        ignoreBlocks: Boolean = true,
        ignoreY: Boolean = false,
        maxDistance: Int? = null,
    ) {
        drawDynamicText(location, text, scaleMultiplier, yOff, hideTooCloseAt, smallestDistanceVew, ignoreBlocks, ignoreY, maxDistance)
    }

    fun SkyHanniRenderWorldEvent.drawDynamicText(
        location: LorenzVec,
        text: String,
        scaleMultiplier: Double,
        yOff: Float = 0f,
        hideTooCloseAt: Double = 4.5,
        smallestDistanceVew: Double = 5.0,
        ignoreBlocks: Boolean = true,
        ignoreY: Boolean = false,
        maxDistance: Int? = null,
    ) {
        TODO()
    }

    private fun SkyHanniRenderWorldEvent.renderText(
        location: LorenzVec,
        text: String,
        scale: Double,
        depthTest: Boolean,
        shadow: Boolean,
        yOff: Float,
    ) {
        TODO()
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawWireframeBoundingBox(
        aabb: Box,
        color: Color,
    ) {
        drawWireframeBoundingBox(aabb, color)
    }

    fun SkyHanniRenderWorldEvent.drawWireframeBoundingBox(
        aabb: Box,
        color: Color,
    ) {
        TODO()
    }


    fun SkyHanniRenderWorldEvent.drawEdges(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(partialTicks) {
            drawEdges(location, color, lineWidth, depth)
        }
    }

    fun SkyHanniRenderWorldEvent.drawEdges(axisAlignedBB: Box, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(partialTicks) {
            drawEdges(axisAlignedBB, color, lineWidth, depth)
        }
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._draw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean,
    ) {
        draw3DLine(p1, p2, color, lineWidth, depth)
    }

    fun SkyHanniRenderWorldEvent.draw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean,
    ) = LineDrawer.draw3D(partialTicks) {
        draw3DLine(p1, p2, color, lineWidth, depth)
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._outlineTopFace(
        boundingBox: Box,
        lineWidth: Int,
        color: Color,
        depth: Boolean,
    ) {
        outlineTopFace(boundingBox, lineWidth, color, depth)
    }

    fun SkyHanniRenderWorldEvent.outlineTopFace(
        boundingBox: Box,
        lineWidth: Int,
        color: Color,
        depth: Boolean,
    ) {
        val (cornerOne, cornerTwo, cornerThree, cornerFour) = boundingBox.getCornersAtHeight(boundingBox.maxY)
        draw3DLine(cornerOne, cornerTwo, color, lineWidth, depth)
        draw3DLine(cornerTwo, cornerThree, color, lineWidth, depth)
        draw3DLine(cornerThree, cornerFour, color, lineWidth, depth)
        draw3DLine(cornerFour, cornerOne, color, lineWidth, depth)
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._drawHitbox(
        boundingBox: Box,
        lineWidth: Int,
        color: Color,
        depth: Boolean,
    ) {
        drawHitbox(boundingBox, lineWidth, color, depth)
    }

    fun SkyHanniRenderWorldEvent.drawHitbox(
        boundingBox: Box,
        lineWidth: Int,
        color: Color,
        depth: Boolean,
    ) {
        val cornersTop = boundingBox.getCornersAtHeight(boundingBox.maxY)
        val cornersBottom = boundingBox.getCornersAtHeight(boundingBox.minY)

        // Draw lines for the top and bottom faces
        for (i in 0..3) {
            this.draw3DLine(cornersTop[i], cornersTop[(i + 1) % 4], color, lineWidth, depth)
            this.draw3DLine(cornersBottom[i], cornersBottom[(i + 1) % 4], color, lineWidth, depth)
        }

        // Draw lines connecting the top and bottom faces
        for (i in 0..3) {
            this.draw3DLine(cornersBottom[i], cornersTop[i], color, lineWidth, depth)
        }
    }

    @Deprecated("Do not use, use proper method instead")
    fun SkyHanniRenderWorldEvent._draw3DPathWithWaypoint(
        //path: Graph,
        colorLine: Color,
        lineWidth: Int,
        depth: Boolean,
        startAtEye: Boolean = true,
        textSize: Double = 1.0,
       // waypointColor: Color =
        //    (path.lastOrNull()?.name?.getFirstColorCode()?.toLorenzColor() ?: LorenzColor.WHITE).toColor(),
        bezierPoint: Double = 1.0,
        showNodeNames: Boolean = false,
        markLastBlock: Boolean = true,
    ) {
        draw3DPathWithWaypoint(
           // path,
            colorLine,
            lineWidth,
            depth,
            startAtEye,
            textSize,
            //waypointColor,
            bezierPoint,
            showNodeNames,
            markLastBlock,
        )
    }

    fun SkyHanniRenderWorldEvent.draw3DPathWithWaypoint(
       // path: Graph,
        colorLine: Color,
        lineWidth: Int,
        depth: Boolean,
        startAtEye: Boolean = true,
        textSize: Double = 1.0,
       // waypointColor: Color =
       //     (path.lastOrNull()?.name?.getFirstColorCode()?.toLorenzColor() ?: LorenzColor.WHITE).toColor(),
        bezierPoint: Double = 1.0,
        showNodeNames: Boolean = false,
        markLastBlock: Boolean = true,
    ) {
        TODO()
    }

    fun getViewerPos(partialTicks: Float) =
        MinecraftClient.getInstance().getCameraEntity()?.let { exactLocation(it, partialTicks) } ?: LorenzVec()

    fun Box.expandBlock(n: Int = 1) = expand(LorenzVec.expandVector * n)
    fun Box.inflateBlock(n: Int = 1) = expand(LorenzVec.expandVector * -n)

    fun exactLocation(entity: Entity, partialTicks: Float): LorenzVec {
        TODO()
    }

    fun SkyHanniRenderWorldEvent.exactLocation(entity: Entity) = exactLocation(entity, partialTicks)

    fun SkyHanniRenderWorldEvent.exactPlayerEyeLocation(): LorenzVec {
        val player = MinecraftCompat.localPlayer
        val eyeHeight = player.standingEyeHeight.toDouble()
       // PatcherFixes.onPlayerEyeLine()
        return exactLocation(player).add(y = eyeHeight)
    }

    fun SkyHanniRenderWorldEvent.exactBoundingBox(entity: Entity): Box {
        if (entity.deceased) return entity.boundingBox
        val offset = exactLocation(entity) - entity.getLorenzVec()
        return entity.boundingBox.offset(offset.x, offset.y, offset.z)
    }

    fun SkyHanniRenderWorldEvent.exactPlayerEyeLocation(player: Entity): LorenzVec {
        val add = if (player.isSneaking) LorenzVec(0.0, 1.54, 0.0) else LorenzVec(0.0, 1.62, 0.0)
        return exactLocation(player) + add
    }

    private fun Color.bindColor(): Unit = TODO()

    private fun bindCamera() {
        TODO()
    }

    fun BufferBuilder.pos(vec: LorenzVec): BufferBuilder = TODO()

    fun translate(vec: LorenzVec): Unit = TODO()

}
