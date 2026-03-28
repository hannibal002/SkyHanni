package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawPyramid
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color

@SkyHanniModule
object DeferredDrawer {

    private val boxesNoDepth = mutableListOf<DeferredBox>()
    private val boxesDepth = mutableListOf<DeferredBox>()
    private val pyramidsNoDepth = mutableListOf<DeferredPyramid>()
    private val pyramidsDepth = mutableListOf<DeferredPyramid>()
    private val stringsNoDepth = mutableListOf<DeferredString>()
    private val stringsDepth = mutableListOf<DeferredString>()

    @HandleEvent(priority = 999)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        event.isCurrentlyDeferring = false
        boxesNoDepth.forEach { box ->
            event.drawFilledBoundingBox(
                box.aabb,
                box.color,
                box.alphaMultiplier,
                renderRelativeToCamera = true,
                seeThroughBlocks = true,
            )
        }
        boxesNoDepth.clear()
        boxesDepth.forEach { box ->
            event.drawFilledBoundingBox(
                box.aabb,
                box.color,
                box.alphaMultiplier,
                renderRelativeToCamera = true,
                seeThroughBlocks = false,
            )
        }
        boxesDepth.clear()
        pyramidsNoDepth.forEach { pyramid ->
            event.drawPyramid(
                pyramid.topPoint,
                pyramid.baseCenterPoint,
                pyramid.baseEdgePoint,
                pyramid.color,
                depth = false,
            )
        }
        pyramidsNoDepth.clear()
        pyramidsDepth.forEach { pyramid ->
            event.drawPyramid(
                pyramid.topPoint,
                pyramid.baseCenterPoint,
                pyramid.baseEdgePoint,
                pyramid.color,
                depth = true,
            )
        }
        pyramidsDepth.clear()
        stringsNoDepth.forEach { string ->
            event.drawString(
                string.location,
                string.text,
                string.component,
                seeThroughBlocks = true,
                string.color,
                string.scale,
                string.shadow,
                string.yOffset,
                string.backgroundColor,
            )
        }
        stringsNoDepth.clear()
        stringsDepth.forEach { string ->
            event.drawString(
                string.location,
                string.text,
                string.component,
                seeThroughBlocks = false,
                string.color,
                string.scale,
                string.shadow,
                string.yOffset,
                string.backgroundColor,
            )
        }
        stringsDepth.clear()
    }

    fun deferBox(
        aabb: AABB,
        color: Color,
        alphaMultiplier: Float,
        depth: Boolean = true,
    ) {
        val deferredBox = DeferredBox(aabb, color, alphaMultiplier)
        if (depth) {
            boxesDepth.add(deferredBox)
        } else {
            boxesNoDepth.add(deferredBox)
        }
    }

    fun deferPyramid(
        topPoint: Vec3,
        baseCenterPoint: Vec3,
        baseEdgePoint: Vec3,
        color: Color,
        depth: Boolean = true,
    ) {
        val deferredPyramid = DeferredPyramid(topPoint, baseCenterPoint, baseEdgePoint, color)
        if (depth) {
            pyramidsDepth.add(deferredPyramid)
        } else {
            pyramidsNoDepth.add(deferredPyramid)
        }
    }

    fun deferString(
        location: Vec3,
        text: String,
        color: Color?,
        scale: Double,
        shadow: Boolean,
        yOffset: Float,
        backgroundColor: Int,
        depth: Boolean,
    ) {
        val deferredString = DeferredString(location, text, null, color, scale, shadow, yOffset, backgroundColor)
        if (depth) {
            stringsDepth.add(deferredString)
        } else {
            stringsNoDepth.add(deferredString)
        }
    }

    fun deferString(
        location: Vec3,
        component: Component,
        color: Color?,
        scale: Double,
        shadow: Boolean,
        yOffset: Float,
        backgroundColor: Int,
        depth: Boolean,
    ) {
        val deferredString = DeferredString(location, null, component, color, scale, shadow, yOffset, backgroundColor)
        if (depth) {
            stringsDepth.add(deferredString)
        } else {
            stringsNoDepth.add(deferredString)
        }
    }

    data class DeferredBox(
        val aabb: AABB,
        val color: Color,
        val alphaMultiplier: Float,
    )

    data class DeferredPyramid(
        val topPoint: Vec3,
        val baseCenterPoint: Vec3,
        val baseEdgePoint: Vec3,
        val color: Color,
    )

    data class DeferredString(
        val location: Vec3,
        val text: String?,
        val component: Component?,
        val color: Color?,
        val scale: Double,
        val shadow: Boolean,
        val yOffset: Float,
        val backgroundColor: Int,
    )
}
