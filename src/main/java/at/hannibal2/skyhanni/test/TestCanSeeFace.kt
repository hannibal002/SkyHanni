package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.maxBox
import at.hannibal2.skyhanni.utils.LocationUtils.minBox
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFaceRayWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.fillFace
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumFacing
import kotlin.time.Duration.Companion.seconds

typealias PointSet = MutableList<Pair<LorenzVec, Boolean>>
typealias FacePointEntry = Map.Entry<EnumFacing, PointSet>
typealias FacePointSet = MutableMap<EnumFacing, PointSet>

@SkyHanniModule
object TestCanSeeFace {

    data class FaceCheckContext(
        var vec1: LorenzVec? = null,
        var vec2: LorenzVec? = null,
        var waitingForPunch: Boolean = false,
        var pointSet: FacePointSet = mutableMapOf(),
        var generallySeen: Boolean = false,
        var finished: Boolean = false,
        var summaryRenderable: Renderable? = null,
    ) : ResettableStorageSet() {
        fun resetFromBlockVec(blockVec: LorenzVec) {
            this.reset()
            val base = blockVec.floor()
            val aabb = base.boundingToOffset(1.0, 1.0, 1.0)
            vec1 = aabb.minBox()
            vec2 = aabb.maxBox()
        }

        fun buildSummaryRenderable(): Renderable = VerticalContainerRenderable(
            buildList {
                pointSet.forEach { addFacePointDisplay(it) }
            }
        )
    }

    private fun MutableList<Renderable>.addFacePointDisplay(fpe: FacePointEntry) {
        val (face, points) = fpe
        add(StringRenderable(""))
        add(StringRenderable("Face: ${face.toString().firstLetterUppercase()}"))
        addFaceToggle(face)
        if (faceStates[face] == FaceState.HIDDEN) {
            add(StringRenderable("§7§oFace is hidden - vecs collapsed."))
            return
        }
        val pointsFormat = buildString {
            append("Points: ${points.size}")
            val visibleFormat = "§a${points.count { it.second }}"
            val hiddenFormat = "§c${points.size - points.count { it.second }}"
            append(" §7( $visibleFormat §7/ $hiddenFormat §7)")
        }
        add(StringRenderable(pointsFormat))
        addAll(points.buildDisplay())
    }

    private fun PointSet.buildDisplay(): List<Renderable> =
        this.take(config.vectorsPerFace).mapIndexed { index, (point, isSeen) ->
            val format = if (isSeen) "§a§l✓§r" else "§c§l✗§r"
            val vecFormat = point.shortFormatVec()
            StringRenderable(" Point $index: $vecFormat $format")
        }

    fun MutableList<Renderable>.addFaceToggle(face: EnumFacing) = addRenderableButton(
        label = "Toggle",
        current = faceStates[face] ?: FaceState.VISIBLE,
        getName = { it.toString() },
        onChange = {
            toggleFaceVisibility(face)
        },
    )

    enum class FaceState(private val displayName: String) {
        VISIBLE("Visible"),
        HIDDEN("Hidden"),
        ;

        override fun toString(): String = displayName
    }

    val faceStates: MutableMap<EnumFacing, FaceState> by lazy {
        EnumFacing.entries.associateWith { FaceState.VISIBLE }.toMutableMap()
    }

    fun toggleFaceVisibility(face: EnumFacing) {
        faceStates[face] = when (faceStates[face]) {
            FaceState.VISIBLE -> FaceState.HIDDEN
            else -> FaceState.VISIBLE
        }
        regenDebugRenderable()
    }

    private fun LorenzVec.shortFormatVec(): String {
        val xFormat = this.x.roundTo(2)
        val yFormat = this.y.roundTo(2)
        val zFormat = this.z.roundTo(2)
        return "($xFormat, $yFormat, $zFormat)"
    }

    private var lastRenderable: Renderable? = null
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
        regenDebugRenderable()
        faceCheckContext.finished = true
        DelayedRun.runDelayed(config.refreshInterval.seconds) {
            faceCheckContext.pointSet.clear()
            faceCheckContext.finished = false
            faceCheckContext.generallySeen = false
            faceCheckContext.summaryRenderable = null
        }
    }

    private fun regenDebugRenderable() {
        if (!config.enabled || !config.debugInfo) return
        faceCheckContext.summaryRenderable = faceCheckContext.buildSummaryRenderable().wrapWithOtherToggles()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        val pointSet = faceCheckContext.pointSet.takeIfNotEmpty() ?: return
        for ((face, points) in pointSet) {
            event.tryHighlightFace(faceCheckContext, face)
            event.drawRaysFromFacePoints(face, points)
        }
    }

    enum class VisibilityType(private val displayName: String) {
        ALL("All Faces"),
        SEEN("Seen Faces"),
        ;

        override fun toString(): String = displayName
    }

    var currentVisibilityType: VisibilityType = VisibilityType.ALL

    private fun Renderable.wrapWithOtherToggles() = VerticalContainerRenderable(
        buildList {
            addRenderableButton(
                label = "Ray Visibility",
                current = currentVisibilityType,
                onChange = {
                    currentVisibilityType = it
                    regenDebugRenderable()
                },
            )
            add(this@wrapWithOtherToggles)
        }
    )

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!config.enabled) return
        val renderable = faceCheckContext.summaryRenderable ?: lastRenderable ?: return
        lastRenderable = renderable
        config.debugPosition.renderRenderable(renderable, "Can See Face Debug")
    }

    private fun SkyHanniRenderWorldEvent.drawRaysFromFacePoints(
        face: EnumFacing,
        points: List<Pair<LorenzVec, Boolean>>,
    ) {
        if (!config.drawPoints || faceStates[face] == FaceState.HIDDEN) return
        for ((point, isSeen) in points) {
            if (currentVisibilityType == VisibilityType.SEEN && !isSeen) continue
            val pointColor = if (isSeen) LorenzColor.GREEN else LorenzColor.RED
            drawFaceRayWorld(
                origin = point,
                face = face,
                color = pointColor.addOpacity(200),
                length = config.rayLength.toDouble(),
                thickness = config.rayThickness.toDouble(),
                seeThroughBlock = true
            )
        }
    }

    private fun SkyHanniRenderWorldEvent.tryHighlightFace(
        context: FaceCheckContext,
        face: EnumFacing,
    ) {
        if (!config.highlightFaces || faceStates[face] == FaceState.HIDDEN) return
        val vec1 = context.vec1 ?: return
        val vec2 = context.vec2 ?: return
        val points = context.pointSet[face] ?: return
        val faceSeen = points.any { it.second }
        val color = if (faceSeen) LorenzColor.GREEN else LorenzColor.RED
        val finalColor = color.addOpacity(120)
        val aabb = AxisAlignedBB(
            vec1.x, vec1.y, vec1.z,
            vec2.x, vec2.y, vec2.z,
        )
        fillFace(aabb, face, finalColor, alpha = 1f)
    }

}
