package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.PatcherFixes
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
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
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Camera
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.VertexRendering
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer
import net.minecraft.client.util.BufferAllocator
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import org.joml.Matrix4f
import java.awt.Color
import kotlin.math.sqrt

object WorldRenderUtils {

    private val beaconBeam = createResourceLocation("textures/entity/beacon_beam.png")

    fun SkyHanniRenderWorldEvent.renderBeaconBeam(vec: LorenzVec, rgb: Int) {
        this.renderBeaconBeam(vec.x, vec.y, vec.z, rgb)
    }

    fun SkyHanniRenderWorldEvent.renderBeaconBeam(
        x: Double,
        y: Double,
        z: Double,
        rgb: Int,
    ) {
        matrices.push()
        matrices.translate(x - camera.pos.x, y - camera.pos.y, z - camera.pos.z)
        BeaconBlockEntityRenderer.renderBeam(
            matrices,
            vertexConsumers,
            beaconBeam,
            partialTicks,
            1f,
            MinecraftCompat.localWorld.time,
            0,
            319,
            rgb,
            0.2f,
            0.25f,
        )
        matrices.pop()
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
        val (viewerX, viewerY, viewerZ) = getViewerPos()
        val x = location.x - viewerX
        val y = location.y - viewerY
        val z = location.z - viewerZ
        val distSq = x * x + y * y + z * z

        val realAlpha = if (alpha == -1f) {
            (0.1f + 0.005f * distSq.toFloat()).coerceIn(0.2f..1f)
        } else {
            alpha
        }

        drawFilledBoundingBox(
            Box(x, y, z, x + 1, y + 1, z + 1),
            color,
            realAlpha,
            true,
            seeThroughBlocks = seeThroughBlocks,
        )
        // todo use seeThroughBlocks
        if (distSq > 5 * 5 && beacon) renderBeaconBeam(location.x, location.y + 1, location.z, color.rgb)

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
        val (viewerX, viewerY, viewerZ) = getViewerPos()
        val x = location.x - viewerX
        val y = location.y - viewerY
        val z = location.z - viewerZ
        val distSq = x * x + y * y + z * z

        drawFilledBoundingBox(
            Box(
                x - extraSize, y - extraSizeBottomY, z - extraSize,
                x + 1 + extraSize, y + 1 + extraSizeTopY, z + 1 + extraSize,
            ).expandBlock(),
            color,
            if (inverseAlphaScale) (1f - 0.005f * distSq.toFloat()).coerceIn(minimumAlpha..1f)
            else (0.1f + 0.005f * distSq.toFloat()).coerceIn(minimumAlpha..1f),
            renderRelativeToCamera = true,
            seeThroughBlocks = seeThroughBlocks,
        )

        // todo use seeThroughBlocks
        if (distSq > 5 * 5 && beacon) renderBeaconBeam(location.x, location.y + 1, location.z, color.rgb)
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
        seeThroughBlocks: Boolean = false,
    ) {
        val effectiveAABB = if (!renderRelativeToCamera) {
            val vp = getViewerPos()
            Box(
                aabb.minX - vp.x, aabb.minY - vp.y, aabb.minZ - vp.z,
                aabb.maxX - vp.x, aabb.maxY - vp.y, aabb.maxZ - vp.z,
            )
        } else {
            aabb
        }

        val layer = SkyHanniRenderLayers.getFilled(seeThroughBlocks)
        val buf = SkyHanniRenderLayers.getBufferFromLayer(layer)
        matrices.push()

        // todo drawVertical barriers

        VertexRendering.drawFilledBox(
            matrices,
            buf,
            effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.minZ,
            effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.maxZ,
            c.red / 255f * 0.9f,
            c.green / 255f * 0.9f,
            c.blue / 255f * 0.9f,
            c.alpha / 255f * alphaMultiplier,
        )
        layer.draw(buf.end())
        matrices.pop()

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

    private val bufferAllocator: BufferAllocator = BufferAllocator(1536)

    fun SkyHanniRenderWorldEvent.drawString(
        loc: LorenzVec,
        text: String,
        seeThroughBlocks: Boolean = false,
        color: Color? = null,
    ) {
        val matrix = Matrix4f()
        val cameraPos = camera.pos
        val fr = MinecraftClient.getInstance().textRenderer

        val scale = 0.02666667f

        matrix.translate(
            (loc.x - cameraPos.getX()).toFloat(),
            (loc.y - cameraPos.getY()).toFloat(),
            (loc.z - cameraPos.getZ()).toFloat(),
        ).rotate(camera.rotation).scale(scale, -scale, scale)

        val x = -fr.getWidth(text) / 2f

        val consumers = VertexConsumerProvider.immediate(bufferAllocator)

        fr.draw(
            text,
            x,
            0f,
            color?.rgb ?: LorenzColor.WHITE.toColor().rgb,
            false,
            matrix,
            consumers,
            if (seeThroughBlocks) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL,
            LorenzColor.BLACK.toColor().addAlpha(63).rgb,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
        )
        consumers.draw()
    }

    private fun SkyHanniRenderWorldEvent.drawNametag(str: String, color: Color?) {
        TODO("Someone used this function somewhere. Big mistake, it isn't needed.")
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
        val (viewerX, viewerY, viewerZ) = getViewerPos()
        val x = location.x - viewerX
        val y = location.y - viewerY
        val z = location.z - viewerZ

        val player = MinecraftCompat.localPlayerOrNull ?: return
        val eyeHeight = player.getEyeHeight(player.pose)

        val distToPlayerSq = x * x + y * y + z * z
        var distToPlayer = sqrt(distToPlayerSq)
        distToPlayer = distToPlayer.coerceAtLeast(smallestDistanceVew)

        if (distToPlayer < hideTooCloseAt) return
        maxDistance?.let {
            if (ignoreBlocks && distToPlayer > it) return
        }

        val distRender = distToPlayer.coerceAtMost(50.0)

        var scale = distRender / 12
        scale *= scaleMultiplier

        val resultX = x + (location.x + 0.5 - x) / (distToPlayer / distRender)
        val resultY = if (ignoreY) location.y * distToPlayer / distRender else y + eyeHeight +
            (location.y + 20 * distToPlayer / 300 - (y + eyeHeight)) / (distToPlayer / distRender)
        val resultZ = z + (location.z + 0.5 - z) / (distToPlayer / distRender)

        val renderLocation = LorenzVec(resultX, resultY, resultZ)

        renderText(renderLocation, "§f$text", scale, !ignoreBlocks, true, yOff)
    }

    private fun SkyHanniRenderWorldEvent.renderText(
        location: LorenzVec,
        text: String,
        scale: Double,
        seeThroughBlocks: Boolean,
        shadow: Boolean,
        yOff: Float,
    ) {

        val realScale = (scale * 0.05).toFloat()

        val matrix = Matrix4f()
        val cameraPos = camera.pos
        val fr = MinecraftClient.getInstance().textRenderer

        matrix.translate(
            (location.x - cameraPos.getX()).toFloat(),
            (location.y - cameraPos.getY() + yOff).toFloat(),
            (location.z - cameraPos.getZ()).toFloat(),
        ).rotate(camera.rotation).scale(realScale, -realScale, realScale)

        val x = -fr.getWidth(text) / 2f

        val consumers = VertexConsumerProvider.immediate(bufferAllocator)

        fr.draw(
            text,
            x,
            0f,
            LorenzColor.WHITE.toColor().rgb,
            shadow,
            matrix,
            consumers,
            if (seeThroughBlocks) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL,
            0,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
        )
        consumers.draw()
    }

    fun SkyHanniRenderWorldEvent.drawEdges(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(this) {
            drawEdges(location, color, lineWidth, depth)
        }
    }

    fun SkyHanniRenderWorldEvent.drawEdges(axisAlignedBB: Box, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(this) {
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
    ) = LineDrawer.draw3D(this) {
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
        color: Color,
        lineWidth: Int = 3,
        depth: Boolean = true,
    ) {
        drawHitbox(boundingBox, color, lineWidth,  depth)
    }

    fun SkyHanniRenderWorldEvent.drawHitbox(
        boundingBox: Box,
        color: Color,
        lineWidth: Int = 3,
        depth: Boolean = true,
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
        path: Graph,
        colorLine: Color,
        lineWidth: Int,
        depth: Boolean,
        startAtEye: Boolean = true,
        textSize: Double = 1.0,
        waypointColor: Color =
            (path.lastOrNull()?.name?.getFirstColorCode()?.toLorenzColor() ?: LorenzColor.WHITE).toColor(),
        bezierPoint: Double = 1.0,
        showNodeNames: Boolean = false,
        markLastBlock: Boolean = true,
    ) {
        draw3DPathWithWaypoint(
            path,
            colorLine,
            lineWidth,
            depth,
            startAtEye,
            textSize,
            waypointColor,
            bezierPoint,
            showNodeNames,
            markLastBlock,
        )
    }

    fun SkyHanniRenderWorldEvent.draw3DPathWithWaypoint(
        path: Graph,
        colorLine: Color,
        lineWidth: Int,
        depth: Boolean,
        startAtEye: Boolean = true,
        textSize: Double = 1.0,
        waypointColor: Color =
            (path.lastOrNull()?.name?.getFirstColorCode()?.toLorenzColor() ?: LorenzColor.WHITE).toColor(),
        bezierPoint: Double = 1.0,
        showNodeNames: Boolean = false,
        markLastBlock: Boolean = true,
    ) {
        if (path.isEmpty()) return
        val points = if (startAtEye) {
            listOf(
                this.exactPlayerEyeLocation() + MinecraftCompat.localPlayer.rotationVector
                    .toLorenzVec()
                    /* .rotateXZ(-Math.PI / 72.0) */
                    .times(2),
            )
        } else {
            emptyList()
        } + path.toPositionsList().map { it.add(0.5, 0.5, 0.5) }
        LineDrawer.draw3D(this) {
            drawPath(
                points,
                colorLine,
                lineWidth,
                depth,
                bezierPoint,
            )
        }
        if (showNodeNames) {
            path.filter { it.name?.isNotEmpty() == true }.forEach {
                this.drawDynamicText(it.position, it.name!!, textSize)
            }
        }
        if (markLastBlock) {
            val last = path.last()
            drawWaypointFilled(last.position, waypointColor, seeThroughBlocks = true)
        }
    }

    fun getViewerPos() =
        MinecraftClient.getInstance().gameRenderer.camera?.let { exactLocation(it) } ?: LorenzVec()

    fun Box.expandBlock(n: Int = 1) = expand(LorenzVec.expandVector * n)
    fun Box.inflateBlock(n: Int = 1) = expand(LorenzVec.expandVector * -n)

    fun exactLocation(entity: Entity, partialTicks: Float): LorenzVec {
        if (!entity.isAlive) return entity.getLorenzVec()
        val x = entity.lastRenderX + (entity.x - entity.lastRenderX) * partialTicks
        val y = entity.lastRenderY + (entity.y - entity.lastRenderY) * partialTicks
        val z = entity.lastRenderZ + (entity.z - entity.lastRenderZ) * partialTicks
        return LorenzVec(x, y, z)
    }

    fun exactLocation(camera: Camera): LorenzVec {
        val pos = camera.pos
        return LorenzVec(pos.x, pos.y, pos.z)
    }

    fun SkyHanniRenderWorldEvent.exactLocation(entity: Entity) = exactLocation(entity, partialTicks)

    fun SkyHanniRenderWorldEvent.exactPlayerEyeLocation(): LorenzVec {
        val player = MinecraftCompat.localPlayer
        val eyeHeight = player.standingEyeHeight.toDouble()
        PatcherFixes.onPlayerEyeLine()
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

    private fun bindCamera() {
        TODO()
    }

}
