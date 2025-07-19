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
import net.minecraft.client.render.Camera
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.VertexRendering
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import org.joml.Matrix4f
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
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

        if (this.isCurrentlyDeferring) {
            DeferredDrawer.deferBox(
                effectiveAABB,
                c,
                alphaMultiplier,
                depth = !seeThroughBlocks,
            )
            return
        }

        val layer = SkyHanniRenderLayers.getFilled(seeThroughBlocks)
        val buf = vertexConsumers.getBuffer(layer)
        matrices.push()

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
        matrices.pop()
    }

    fun SkyHanniRenderWorldEvent.drawString(
        location: LorenzVec,
        text: String,
        seeThroughBlocks: Boolean = false,
        color: Color? = null,
        scale: Double = 0.53333333,
        shadow: Boolean = false,
        yOffset: Float = 0f,
        backGroundColor: Int = LorenzColor.BLACK.toColor().addAlpha(63).rgb,
    ) {
        if (this.isCurrentlyDeferring) {
            DeferredDrawer.deferString(
                location,
                text,
                color,
                scale,
                shadow,
                yOffset,
                backGroundColor,
                !seeThroughBlocks,
            )
            return
        }

        val matrix = Matrix4f()
        val cameraPos = camera.pos
        val fr = MinecraftClient.getInstance().textRenderer
        val adjustedScale = (scale * 0.05).toFloat()

        matrix.translate(
            (location.x - cameraPos.getX()).toFloat(),
            (location.y - cameraPos.getY() + yOffset * adjustedScale).toFloat(),
            (location.z - cameraPos.getZ()).toFloat(),
        ).rotate(camera.rotation).scale(adjustedScale, -adjustedScale, adjustedScale)

        val x = -fr.getWidth(text) / 2f

        fr.draw(
            text,
            x,
            0f,
            color?.rgb ?: LorenzColor.WHITE.toColor().rgb,
            shadow,
            matrix,
            vertexConsumers,
            if (seeThroughBlocks) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL,
            backGroundColor,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
        )
    }

    fun SkyHanniRenderWorldEvent.drawCircleWireframe(entity: Entity, rad: Double, color: Color) {
        val entityLocation = exactLocation(entity)
        val x = entityLocation.x
        val y = entityLocation.y
        val z = entityLocation.z

        val segments = 64
        LineDrawer.draw3D(this, 5, false) {
            for (i in 0 until segments) {
                val theta1 = 2.0 * Math.PI * i / segments
                val theta2 = 2.0 * Math.PI * (i + 1) / segments

                val x1 = x + rad * cos(theta1)
                val z1 = z + rad * sin(theta1)

                val x2 = x + rad * cos(theta2)
                val z2 = z + rad * sin(theta2)

                draw3DLine(LorenzVec(x1, y, z1), LorenzVec(x2, y, z2), color)
            }
        }
    }

    fun SkyHanniRenderWorldEvent.drawCircleFilled(
        entity: Entity,
        rad: Double,
        color: Color,
        depth: Boolean = true,
        segments: Int = 32,
    ) {
        val exactLocation = exactLocation(entity)
        drawCircleFilled(exactLocation.x, exactLocation.y, exactLocation.z, rad, color, depth, segments)
    }

    fun SkyHanniRenderWorldEvent.drawCircleFilled(
        locX: Double,
        locY: Double,
        locZ: Double,
        rad: Double,
        color: Color,
        depth: Boolean = true,
        segments: Int = 32,
    ) {
        val layer = SkyHanniRenderLayers.getTriangleFan(!depth)
        val buf = vertexConsumers.getBuffer(layer)
        matrices.push()

        val viewerPos = getViewerPos()
        val x = locX - viewerPos.x
        val y = locY - viewerPos.y
        val z = locZ - viewerPos.z

        for (i in 0 until segments) {
            val theta1 = 2.0 * Math.PI * i / segments
            val theta2 = 2.0 * Math.PI * (i + 1) / segments

            val x1 = x + rad * cos(theta1)
            val z1 = z + rad * sin(theta1)

            val x2 = x + rad * cos(theta2)
            val z2 = z + rad * sin(theta2)

            buf.vertex(x.toFloat(), y.toFloat(), z.toFloat()).color(color.red, color.green, color.blue, color.alpha)
                .vertex(x1.toFloat(), y.toFloat(), z1.toFloat())
                .color(color.red, color.green, color.blue, color.alpha)
                .vertex(x2.toFloat(), y.toFloat(), z2.toFloat())
                .color(color.red, color.green, color.blue, color.alpha)
        }

        matrices.pop()
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
        locX: Double,
        locY: Double,
        locZ: Double,
        radius: Float,
        height: Float,
    ) {
        val segments = 64

        val layer = SkyHanniRenderLayers.getFilled(false)
        val buf = vertexConsumers.getBuffer(layer)
        matrices.push()

        val (viewerX, viewerY, viewerZ) = getViewerPos()
        val x = locX - viewerX
        val y = locY - viewerY
        val z = locZ - viewerZ

        for (i in 0 until segments) {
            val angle = 2.0 * Math.PI * i / segments

            val xOffset = radius * cos(angle)
            val zOffset = radius * sin(angle)

            buf.vertex((x + xOffset).toFloat(), y.toFloat(), (z + zOffset).toFloat())
                .color(color.red, color.green, color.blue, color.alpha)
            buf.vertex((x + xOffset).toFloat(), (y + height).toFloat(), (z + zOffset).toFloat())
                .color(color.red, color.green, color.blue, color.alpha)
        }
        buf.vertex((x + radius).toFloat(), y.toFloat(), (z + 0).toFloat())
            .color(color.red, color.green, color.blue, color.alpha)
        buf.vertex((x + radius).toFloat(), (y + height).toFloat(), (z + 0).toFloat())
            .color(color.red, color.green, color.blue, color.alpha)

        matrices.pop()

        drawCircleFilled(locX, locY, locZ, radius.toDouble(), color, depth = true, segments = segments)
        drawCircleFilled(locX, locY + height, locZ, radius.toDouble(), color, depth = true, segments = segments)
    }

    fun SkyHanniRenderWorldEvent.drawPyramid(
        topPoint: LorenzVec,
        baseCenterPoint: LorenzVec,
        baseEdgePoint: LorenzVec,
        color: Color,
        depth: Boolean = true,
    ) {
        if (this.isCurrentlyDeferring) {
            DeferredDrawer.deferPyramid(topPoint, baseCenterPoint, baseEdgePoint, color, depth)
            return
        }

        val layer = SkyHanniRenderLayers.getTriangles(!depth)
        val buf = vertexConsumers.getBuffer(layer)
        matrices.push()

        val viewerPos = getViewerPos()
        val newTop = topPoint - viewerPos
        val baseCenter = baseCenterPoint - viewerPos
        val baseEdge = baseEdgePoint - viewerPos

        val edgeVec = baseEdge - baseCenter
        val topVecNorm = (newTop - baseCenter).normalize()
        val corner1 = baseEdge
        val corner2 = topVecNorm.crossProduct(edgeVec).normalize() * edgeVec.length() + baseCenter
        val corner3 = baseCenter - edgeVec
        val corner4 = edgeVec.crossProduct(topVecNorm).normalize() * edgeVec.length() + baseCenter

        fun tri(a: LorenzVec, b: LorenzVec, c: LorenzVec) {
            buf.vertex(a.x.toFloat(), a.y.toFloat(), a.z.toFloat()).color(color.red, color.green, color.blue, color.alpha)
            buf.vertex(b.x.toFloat(), b.y.toFloat(), b.z.toFloat()).color(color.red, color.green, color.blue, color.alpha)
            buf.vertex(c.x.toFloat(), c.y.toFloat(), c.z.toFloat()).color(color.red, color.green, color.blue, color.alpha)
        }

        tri(newTop, corner1, corner2)
        tri(newTop, corner2, corner3)
        tri(newTop, corner3, corner4)
        tri(newTop, corner4, corner1)

        tri(corner1, corner2, corner3)
        tri(corner1, corner3, corner4)

        matrices.pop()
    }

    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        segments: Int = 32,
    ) {
        drawSphereInWorld(color, location.x, location.y, location.z, radius, segments)
    }

    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        locX: Double,
        locY: Double,
        locZ: Double,
        radius: Float,
        segments: Int = 32,
    ) {
        val layer = SkyHanniRenderLayers.getQuads(false)
        val buf = vertexConsumers.getBuffer(layer)
        matrices.push()

        val (viewerX, viewerY, viewerZ) = getViewerPos()
        val x = locX - viewerX
        val y = locY - viewerY
        val z = locZ - viewerZ

        for (phi in 0 until segments) {
            for (theta in 0 until segments * 2) {
                val x1 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                val y1 = y + radius * cos(Math.PI * phi / segments)
                val z1 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                val x2 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                val y2 = y + radius * cos(Math.PI * (phi + 1) / segments)
                val z2 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                val x3 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                val y3 = y + radius * cos(Math.PI * (phi + 1) / segments)
                val z3 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                val x4 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                val y4 = y + radius * cos(Math.PI * phi / segments)
                val z4 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                buf.vertex(x1.toFloat(), y1.toFloat(), z1.toFloat())
                    .color(color.red, color.green, color.blue, color.alpha)
                buf.vertex(x2.toFloat(), y2.toFloat(), z2.toFloat())
                    .color(color.red, color.green, color.blue, color.alpha)
                buf.vertex(x3.toFloat(), y3.toFloat(), z3.toFloat())
                    .color(color.red, color.green, color.blue, color.alpha)
                buf.vertex(x4.toFloat(), y4.toFloat(), z4.toFloat())
                    .color(color.red, color.green, color.blue, color.alpha)
            }
        }

        matrices.pop()
    }

    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        segments: Int = 32,
    ) {
        drawSphereWireframeInWorld(color, location.x, location.y, location.z, radius, segments)
    }

    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        segments: Int = 32,
    ) {
        for (phi in 0 until segments) {
            LineDrawer.draw3D(this, 2, true) {
                for (theta in 0 until segments * 2) {
                    val x1 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                    val y1 = y + radius * cos(Math.PI * phi / segments)
                    val z1 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                    val x2 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                    val y2 = y + radius * cos(Math.PI * (phi + 1) / segments)
                    val z2 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                    val x3 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                    val y3 = y + radius * cos(Math.PI * (phi + 1) / segments)
                    val z3 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                    val x4 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                    val y4 = y + radius * cos(Math.PI * phi / segments)
                    val z4 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                    val p1 = LorenzVec(x1, y1, z1)
                    val p2 = LorenzVec(x2, y2, z2)
                    val p3 = LorenzVec(x3, y3, z3)
                    val p4 = LorenzVec(x4, y4, z4)
                    drawPath(listOf(p1, p2, p3, p4), color, -1.0)
                }
            }
        }
    }

    fun SkyHanniRenderWorldEvent.drawDynamicText(
        location: LorenzVec,
        text: String,
        scaleMultiplier: Double,
        yOff: Float = 0f,
        hideTooCloseAt: Double = 4.5,
        smallestDistanceVew: Double = 5.0,
        seeThroughBlocks: Boolean = true,
        ignoreY: Boolean = false,
        maxDistance: Int? = null,
    ) {
        val (viewerX, viewerY, viewerZ) = getViewerPos()

        val x = location.x
        val y = location.y
        val z = location.z

        val player = MinecraftCompat.localPlayerOrNull ?: return
        val eyeHeight = player.getEyeHeight(player.pose)

        val dX = (x - viewerX) * (x - viewerX)
        val dY = (y - (viewerY + eyeHeight)) * (y - (viewerY + eyeHeight))
        val dZ = (z - viewerZ) * (z - viewerZ)
        val distToPlayerSq = dX + dY + dZ
        var distToPlayer = sqrt(distToPlayerSq)
        // TODO this is optional maybe?
        distToPlayer = distToPlayer.coerceAtLeast(smallestDistanceVew)

        if (distToPlayer < hideTooCloseAt) return
        maxDistance?.let {
            if (!seeThroughBlocks && distToPlayer > it) return
        }

        val distRender = distToPlayer.coerceAtMost(50.0)

        var scale = distRender / 12
        scale *= scaleMultiplier

        val resultX = viewerX + (x + 0.5 - viewerX) / (distToPlayer / distRender)
        val resultY = if (ignoreY) y * distToPlayer / distRender else viewerY + eyeHeight +
            (y + 20 * distToPlayer / 300 - (viewerY + eyeHeight)) / (distToPlayer / distRender)
        val resultZ = viewerZ + (z + 0.5 - viewerZ) / (distToPlayer / distRender)

        val renderLocation = LorenzVec(resultX, resultY, resultZ)

        drawString(renderLocation, "§f$text", seeThroughBlocks, null, scale, true, yOff, 0)
    }

    fun SkyHanniRenderWorldEvent.drawEdges(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(this, lineWidth, depth) {
            drawEdges(location, color)
        }
    }

    fun SkyHanniRenderWorldEvent.drawEdges(axisAlignedBB: Box, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(this, lineWidth, depth) {
            drawEdges(axisAlignedBB, color)
        }
    }

    fun SkyHanniRenderWorldEvent.draw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean,
    ) = LineDrawer.draw3D(this, lineWidth, depth) {
        draw3DLine(p1, p2, color)
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

    fun SkyHanniRenderWorldEvent.drawLineToEye(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        draw3DLine(
            exactPlayerEyeLocation() + MinecraftCompat.localPlayer.rotationVector.toLorenzVec().times(2),
            location,
            color,
            lineWidth,
            depth,
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
        LineDrawer.draw3D(this, lineWidth, depth) {
            drawPath(
                points,
                colorLine,
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

    fun getViewerPos(ignored: Float) = getViewerPos()

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
}
