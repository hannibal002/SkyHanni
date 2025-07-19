package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.maxBox
import at.hannibal2.skyhanni.utils.LocationUtils.minBox
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFaceRayWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.fillFace
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumFacing
import kotlin.time.Duration.Companion.seconds

typealias PointSet = TimeLimitedSet<Pair<LorenzVec, Boolean>>
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
        var debugRenderable: Renderable? = null,
    ) : ResettableStorageSet() {
        fun resetFromBlockVec(blockVec: LorenzVec) {
            this.reset()
            val base = blockVec.floor()
            // Todo account for variable block sizes?
            val aabb = base.boundingToOffset(1.0, 1.0, 1.0)
            vec1 = aabb.minBox()
            vec2 = aabb.maxBox()
        }

        fun buildSummaryRenderable() = Renderable.vertical(
            renderables = buildList { pointSet.forEach { addFacePointDisplay(it) } }
        )
    }

    private fun MutableList<Renderable>.addFacePointDisplay(fpe: FacePointEntry) {
        val (face, points) = fpe
        add(Renderable.text(""))
        add(Renderable.text("Face: ${face.toString().firstLetterUppercase()}"))
        addRenderableButton(
            label = "Toggle",
            current = faceStates[face] ?: FaceState.VISIBLE,
            getName = { it.toString() },
            onChange = { toggleFaceVisibility(face) },
        )
        if (faceStates[face] == FaceState.HIDDEN) {
            add(Renderable.text("§7§oFace is hidden - vectors collapsed."))
            return
        }

        val pointsFormat = buildString {
            append("Points: ${points.size}")
            val visibleFormat = "§a${points.count { it.second }}"
            val hiddenFormat = "§c${points.size - points.count { it.second }}"
            append(" §7( $visibleFormat §7/ $hiddenFormat §7)")
        }
        add(Renderable.text(pointsFormat))
        addAll(
            points.take(config.vectorsPerFace.get()).mapIndexed { index, (point, isSeen) ->
                val format = if (isSeen) "§a§l✓§r" else "§c§l✗§r"
                val vecFormat = point.shortFormatVec()
                Renderable.text(" Point $index: $vecFormat $format")
            }
        )
    }

    enum class RayVisibilityState(private val displayName: String) {
        ALL("All Rays"),
        SEEN("Seen Rays"),
        ;

        override fun toString(): String = displayName
    }

    enum class FaceState(private val displayName: String) {
        VISIBLE("Visible"),
        HIDDEN("Hidden"),
        ;

        override fun toString(): String = displayName
    }

    private val faceStates: MutableMap<EnumFacing, FaceState> by lazy {
        EnumFacing.entries.associateWith { FaceState.VISIBLE }.toMutableMap()
    }

    private fun toggleFaceVisibility(face: EnumFacing) {
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

    private var currentVisibilityState: RayVisibilityState = RayVisibilityState.ALL
    private var lastRenderable: Renderable? = null
    private val config get() = SkyHanniMod.feature.dev.devTool.canSeeFace
    private val rayConfig get() = config.rays
    private val faceHighlightConfig get() = config.faceHighlight
    private val enabled get() = config.enabled.get()
    private val debugEnabled get() = config.debugInfo.get()
    private val faceCheckContext = FaceCheckContext()

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onAnyToggled(config) {
            regenDebugRenderable()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<EntityPlayerSP>) {
        if (!enabled || !event.isLocalPlayer) return
        if (!config.refreshOnMove.get()) return
        recalcContext(true)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestcanseeface") {
            description = "Test if you can see certain faces of a block."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                if (!enabled) {
                    ChatUtils.clickableChat(
                        "The /shtestcanseeface command is disabled. Click here to enable it in the dev tool config!",
                        onClick = {
                            config::enabled.jumpToEditor()
                        },
                        hover = "Click to open the dev tool config",
                        replaceSameMessage = true
                    )
                    return@simpleCallback
                }
                faceCheckContext.reset()
                faceCheckContext.waitingForPunch = true
                ChatUtils.chat("The next block you punch will be used for the face check.", replaceSameMessage = true)
            }
            literalCallback("stop") {
                faceCheckContext.reset()
                lastRenderable = null
            }
        }
    }

    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!enabled || event.clickType != ClickType.LEFT_CLICK) return
        if (!faceCheckContext.waitingForPunch) return
        faceCheckContext.resetFromBlockVec(event.position)
        ChatUtils.chat("Starting face check for block at ${event.position}.", replaceSameMessage = true)
        recalcContext(force = true)
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!enabled) return
        recalcContext()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!enabled) return
        val pointSet = faceCheckContext.pointSet.takeIfNotEmpty() ?: return
        for ((face, points) in pointSet) {
            event.tryHighlightFace(faceCheckContext, face)
            event.drawRaysFromFacePoints(face, points)
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!enabled || !debugEnabled) return
        val renderable = faceCheckContext.debugRenderable ?: lastRenderable ?: return
        lastRenderable = renderable
        config.debugPosition.renderRenderable(renderable, "Can See Face Debug")
    }

    private fun recalcContext(force: Boolean = false) {
        val vec1 = faceCheckContext.vec1 ?: return
        val vec2 = faceCheckContext.vec2 ?: return
        if (!force && faceCheckContext.finished) return
        faceCheckContext.generallySeen = LocationUtils.canSeeAnyFace(
            min = vec1,
            max = vec2,
            stepCount = config.stepCount.get(),
            stepDensity = config.stepDensity.get(),
            pointFill = faceCheckContext.pointSet,
        )
        regenDebugRenderable()
        faceCheckContext.finished = true
        DelayedRun.runDelayed(config.refreshInterval.get().seconds) {
            recalcContext(true)
        }
    }

    private fun regenDebugRenderable() {
        if (!enabled || !debugEnabled) return
        faceCheckContext.debugRenderable = faceCheckContext.buildSummaryRenderable().wrapWithOtherToggles()
    }

    private fun Renderable.wrapWithOtherToggles() = Renderable.vertical(
        buildList {
            addRenderableButton(
                label = "Ray Visibility",
                current = currentVisibilityState,
                onChange = {
                    currentVisibilityState = it
                    regenDebugRenderable()
                },
            )
            add(this@wrapWithOtherToggles)
        }
    )

    private fun SkyHanniRenderWorldEvent.drawRaysFromFacePoints(
        face: EnumFacing,
        points: Collection<Pair<LorenzVec, Boolean>>,
    ) {
        if (!rayConfig.enabled.get() || faceStates[face] == FaceState.HIDDEN) return
        for ((point, isSeen) in points) {
            if (currentVisibilityState == RayVisibilityState.SEEN && !isSeen) continue
            val pointColor = if (isSeen) rayConfig.seenColor.get() else rayConfig.unSeenColor.get()
            drawFaceRayWorld(
                origin = point,
                face = face,
                color = pointColor.toColor(),
                length = rayConfig.length.get().toDouble(),
                thickness = rayConfig.thickness.get().toDouble(),
                seeThroughBlock = true
            )
        }
    }

    private fun SkyHanniRenderWorldEvent.tryHighlightFace(
        context: FaceCheckContext,
        face: EnumFacing,
    ) {
        if (!faceHighlightConfig.enabled.get() || faceStates[face] == FaceState.HIDDEN) return
        val vec1 = context.vec1 ?: return
        val vec2 = context.vec2 ?: return
        val points = context.pointSet[face] ?: return
        val faceSeen = points.any { it.second }
        val color = if (faceSeen) faceHighlightConfig.seenColor.get() else rayConfig.unSeenColor.get()
        val aabb = AxisAlignedBB(
            vec1.x, vec1.y, vec1.z,
            vec2.x, vec2.y, vec2.z,
        )
        fillFace(aabb, face, color.toColor(), alpha = 1f)
    }

}
