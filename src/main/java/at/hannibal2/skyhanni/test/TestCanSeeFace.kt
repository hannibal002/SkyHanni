package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.maxBox
import at.hannibal2.skyhanni.utils.LocationUtils.minBox
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFaceRayWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.fillFace
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumFacing

typealias FacePointSet = MutableMap<EnumFacing, MutableList<Pair<LorenzVec, Boolean>>>

@SkyHanniModule
object TestCanSeeFace {

    data class FaceCheckContext(
        var vec1: LorenzVec? = null,
        var vec2: LorenzVec? = null,
        var waitingForPunch: Boolean = false,
        var pointSet: FacePointSet = mutableMapOf(),
        var generallySeen: Boolean = false,
        var finished: Boolean = false,
    ) : ResettableStorageSet() {
        fun resetFromBlockVec(blockVec: LorenzVec) {
            this.reset()
            val base = blockVec.floor()
            val aabb = base.boundingToOffset(1.0, 1.0, 1.0)
            vec1 = aabb.minBox()
            vec2 = aabb.maxBox()
        }
    }

    private val config get() = SkyHanniMod.feature.dev.devTool.canSeeFace
    private val faceCheckContext = FaceCheckContext()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestcanseeface") {
            description = "Test if you can see certain faces of a block."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                if (!config.enabled) return@simpleCallback
                faceCheckContext.reset()
                faceCheckContext.waitingForPunch = true
                ChatUtils.chat("The next block you punch will be used for the face check.", replaceSameMessage = true)
            }
            literalCallback("stop") {
                faceCheckContext.reset()
            }
        }
    }

    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!config.enabled) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (!faceCheckContext.waitingForPunch) return
        faceCheckContext.resetFromBlockVec(event.position)
        ChatUtils.chat("Starting face check for block at ${event.position}.", replaceSameMessage = true)
        faceCheckContext.waitingForPunch = false
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!config.enabled) return
        val vec1 = faceCheckContext.vec1 ?: return
        val vec2 = faceCheckContext.vec2 ?: return
        if (faceCheckContext.finished) return
        faceCheckContext.generallySeen = LocationUtils.anyFaceCanBeSeen(
            min = vec1,
            max = vec2,
            stepCount = config.stepCount,
            stepDensity = config.stepDensity,
            pointFill = faceCheckContext.pointSet,
        )
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        val pointSet = faceCheckContext.pointSet.takeIfNotEmpty() ?: return
        for ((face, points) in pointSet) {
            event.tryHighlightFace(faceCheckContext, face)
            event.drawFaceCheckPoints(face, points)
        }
    }

    private fun SkyHanniRenderWorldEvent.drawFaceCheckPoints(
        face: EnumFacing,
        points: List<Pair<LorenzVec, Boolean>>,
    ) {
        if (!config.drawPoints) return
        for ((point, isSeen) in points) {
            val pointColor = if (isSeen) LorenzColor.GREEN else LorenzColor.RED
            val finalColor = pointColor.addOpacity(200)
            drawFaceRayWorld(point, face, finalColor)
        }
    }

    private fun SkyHanniRenderWorldEvent.tryHighlightFace(
        context: FaceCheckContext,
        face: EnumFacing,
    ) {
        if (!config.highlightFaces) return
        val vec1 = context.vec1 ?: return
        val vec2 = context.vec2 ?: return
        val color = if (context.generallySeen) LorenzColor.GREEN else LorenzColor.RED
        val finalColor = color.addOpacity(120)
        val aabb = AxisAlignedBB(
            vec1.x, vec1.y, vec1.z,
            vec2.x, vec2.y, vec2.z,
        )
        fillFace(aabb, face, finalColor, alpha = 1f, renderRelativeToCamera = false)
    }

}
