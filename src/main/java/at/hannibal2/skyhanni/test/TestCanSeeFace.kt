package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.Resettable
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
import at.hannibal2.skyhanni.utils.FacePointEntry
import at.hannibal2.skyhanni.utils.FacePointSet
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.maxBox
import at.hannibal2.skyhanni.utils.LocationUtils.minBox
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFaceRayWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.fillFace
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TestCanSeeFace {

    data class FaceCheckContext(
        var aabbs: List<AABB> = emptyList(),
        var blockPos: LorenzVec? = null,
        var waitingForPunch: Boolean = false,
        var pointSet: FacePointSet = mutableMapOf(),
        var finished: Boolean = false,
        var debugRenderable: Renderable? = null,
        var lastBlockStateHash: Int = 0,
    ) : Resettable {

        fun refreshAABBsFromBlockState() {
            val blockPos = blockPos ?: return
            val level = Minecraft.getInstance().level ?: return

            val mcBlockPos = blockPos.toBlockPos()
            val currentState = level.getBlockState(mcBlockPos)
            val shape = currentState.getShape(level, mcBlockPos)
            val rawAabbs = shape.toAabbs()
            val currentHash = rawAabbs.hashCode()
            if (currentHash != lastBlockStateHash) {
                lastBlockStateHash = currentHash
                aabbs = if (!shape.isEmpty) rawAabbs.map { bounds ->
                    AABB(
                        blockPos.x + bounds.minX, blockPos.y + bounds.minY, blockPos.z + bounds.minZ,
                        blockPos.x + bounds.maxX, blockPos.y + bounds.maxY, blockPos.z + bounds.maxZ,
                    )
                } else listOf(blockPos.boundingToOffset(1.0, 1.0, 1.0))
            }
        }

        fun resetFromClickedBlock(event: BlockClickEvent) {
            this.reset()
            blockPos = event.position.floor()
            refreshAABBsFromBlockState()
        }

        fun buildSummaryRenderable(duration: Duration?) = Renderable.vertical {
            addString("§7Generated in §b${duration?.format() ?: "?"}§7.")
            pointSet.forEach { addFacePointDisplay(it) }
        }
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
            },
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

    private val faceStates: MutableMap<Direction, FaceState> by lazy {
        Direction.entries.associateWith { FaceState.VISIBLE }.toMutableMap()
    }

    private fun toggleFaceVisibility(face: Direction) {
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
    private val config get() = DevApi.config.devTool.canSeeFace
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

    @HandleEvent
    fun onWorldChange() {
        lastRenderable = null
        faceCheckContext.reset()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
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
                if (!enabled) return@simpleCallback ChatUtils.clickableChat(
                    "The /shtestcanseeface command is disabled. Click here to enable it in the dev tool config!",
                    onClick = {
                        config::enabled.jumpToEditor()
                    },
                    hover = "Click to open the dev tool config",
                    replaceSameMessage = true,
                )
                faceCheckContext.reset()
                faceCheckContext.waitingForPunch = true
                ChatUtils.chat("The next block you punch will be used for the face check.", replaceSameMessage = true)
            }
            literalCallback("stop") {
                faceCheckContext.reset()
                lastRenderable = null
                ChatUtils.chat("Stopped face check, and cleared debug info.", replaceSameMessage = true)
            }
        }
    }

    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!enabled || event.clickType != ClickType.LEFT_CLICK) return
        if (!faceCheckContext.waitingForPunch) return
        faceCheckContext.resetFromClickedBlock(event)
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
        for ((face, points) in faceCheckContext.pointSet) {
            event.tryHighlightFace(face)
            event.drawRaysFromFacePoints(face, points)
        }
    }

    @HandleEvent(GuiRenderEvent::class)
    fun onRenderOverlay() {
        if (!enabled || !debugEnabled) return
        val renderable = faceCheckContext.debugRenderable ?: lastRenderable ?: return
        lastRenderable = renderable
        config.debugPosition.renderRenderable(renderable, "Can See Face Debug")
    }

    private fun recalcContext(force: Boolean = false) {
        val calculationStartTime = SimpleTimeMark.now()
        if (faceCheckContext.aabbs.isEmpty()) return
        if (!force && faceCheckContext.finished) return
        faceCheckContext.refreshAABBsFromBlockState()
        faceCheckContext.pointSet.clear()
        for (aabb in faceCheckContext.aabbs) {
            LocationUtils.canSeeAnyFace(
                min = aabb.minBox(),
                max = aabb.maxBox(),
                stepCount = config.stepCount.get(),
                stepDensity = config.stepDensity.get(),
                pointFill = faceCheckContext.pointSet,
            )
        }
        val calculationEndTime = SimpleTimeMark.now()
        regenDebugRenderable(calculationEndTime - calculationStartTime)
        faceCheckContext.finished = true
        DelayedRun.runDelayed(config.refreshInterval.get().seconds) {
            if (!enabled) return@runDelayed
            recalcContext(true)
        }
    }

    private fun regenDebugRenderable(duration: Duration? = null) {
        if (!enabled || !debugEnabled) return
        faceCheckContext.debugRenderable = faceCheckContext.buildSummaryRenderable(duration).wrapWithOtherToggles()
    }

    private fun Renderable.wrapWithOtherToggles() = Renderable.vertical {
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

    private fun SkyHanniRenderWorldEvent.drawRaysFromFacePoints(
        face: Direction,
        points: Collection<Pair<LorenzVec, Boolean>>,
    ) {
        if (!rayConfig.enabled.get() || faceStates[face] == FaceState.HIDDEN) return
        for ((point, isSeen) in points) {
            if (currentVisibilityState == RayVisibilityState.SEEN && !isSeen) continue
            val pointColor = if (isSeen) rayConfig.seenColor.get() else rayConfig.unseenColor.get()
            drawFaceRayWorld(
                origin = point,
                face = face,
                color = pointColor.toColor(),
                length = rayConfig.length.get().toDouble(),
                thickness = rayConfig.thickness.get().toDouble(),
            )
        }
    }

    private fun SkyHanniRenderWorldEvent.tryHighlightFace(face: Direction) {
        if (!faceHighlightConfig.enabled.get() || faceStates[face] == FaceState.HIDDEN) return
        val points = faceCheckContext.pointSet[face] ?: return
        val faceSeen = points.any { it.second }
        val color = if (faceSeen) faceHighlightConfig.seenColor.get() else faceHighlightConfig.unseenColor.get()
        for (aabb in faceCheckContext.aabbs) {
            fillFace(aabb, face, color.toColor(), alpha = 1f)
        }
    }
}
