package at.hannibal2.skyhanni.features.combat.killDetection

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.data.MobData
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.getMotionLorenzVec
import at.hannibal2.skyhanni.utils.getPrevLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import at.hannibal2.skyhanni.utils.vectorFromPoints
import net.minecraft.client.Minecraft
import net.minecraft.entity.projectile.EntityArrow
import net.minecraftforge.event.entity.player.ArrowLooseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import kotlin.math.PI
import kotlin.math.pow

object ArrowDetection {

    private val config get() = SkyHanniMod.feature.dev.mobDebug.mobHitDetecion

    class SkyblockArrow(val base: EntityArrow, val pierce: Int, val canHitEnderman: Boolean) {
        var piercedAmount = 0

    }

    private data class SkyblockArrowSpawn(
        val origin: LorenzVec,
        val direction: LorenzVec,
        val pierce: Int,
        val canHitEnderman: Boolean,
        val spawnTick: Int = MinecraftData.totalTicks,
    ) {
        fun parabola(time: Int) = parabola(origin, direction, time)
        fun isOnParabola(arrow: EntityArrow) = isOnParabola(origin, direction, getLivingTime(), arrow)

        fun getLivingTime() = (MinecraftData.totalTicks - spawnTick)
    }

    private val upComingArrows: Queue<SkyblockArrowSpawn> = LinkedList()

    private val arrowTrail: MutableMap<EntityArrow, MutableList<LorenzVec>> = mutableMapOf()

    fun newArrow(origin: LorenzVec, facingDirection: LorenzVec, pierce: Int, canHitEnderman: Boolean) =
        newArrows(origin, facingDirection, 1, 0.0, pierce, canHitEnderman)

    fun newArrows(
        origin: LorenzVec,
        facingDirection: LorenzVec,
        amount: Int,
        spread: Double,
        pierce: Int,
        canHitEnderman: Boolean,
    ) {
        if (amount < 1) return
        upComingArrows.add(SkyblockArrowSpawn(origin, facingDirection, pierce, canHitEnderman))
        if (amount < 2) return
        if (amount % 2 == 0) throw NotImplementedError("Even Number of Arrows are not supported")
        val spreadInRad = Math.toRadians(spread)
        for (i in 1..amount / 2) {
            upComingArrows.add(
                SkyblockArrowSpawn(
                    origin, facingDirection.rotateXZ(spreadInRad * i), pierce, canHitEnderman
                )
            )
            upComingArrows.add(
                SkyblockArrowSpawn(
                    origin, facingDirection.rotateXZ(-spreadInRad * i), pierce, canHitEnderman
                )
            )
        }
    }


    public val playerArrows = mutableSetOf<SkyblockArrow>()

    private val currentArrowsInWorld = mutableSetOf<EntityArrow>()
    private val previousArrowsInWorld = mutableSetOf<EntityArrow>()

    private val renderRealArrowLineList = mutableListOf<Line>()
    private val renderArrowDetectLineList = mutableListOf<Line>()

    data class Line(val start: LorenzVec, val end: LorenzVec)

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        previousArrowsInWorld.clear()
        previousArrowsInWorld.addAll(currentArrowsInWorld)
        currentArrowsInWorld.clear()
        currentArrowsInWorld.addAll(EntityUtils.getEntities<EntityArrow>())

        // New Arrow
        (currentArrowsInWorld - previousArrowsInWorld).forEach { onArrowSpawn(it) }
        // Arrow Disappeared
        (previousArrowsInWorld - currentArrowsInWorld).forEach { onArrowDeSpawn(it) }

        // currentArrowsInWorld.forEach{arrow ->
        //    val speed = LorenzVec(arrow.posX-arrow.lastTickPosX,arrow.posY-arrow.lastTickPosY,arrow.posZ-arrow.lastTickPosZ).multiply(20).length()
        //    LorenzDebug.log("Arrow Speed: $speed")
        //}
        if (!config.arrowDebug) return
        currentArrowsInWorld.forEach { entity ->
            renderRealArrowLineList.add(
                Line(
                    entity.getPrevLorenzVec(), entity.getLorenzVec()
                )
            )
            arrowTrail.getOrDefault(entity, null)?.add(entity.getLorenzVec()) ?: mutableListOf(entity.getLorenzVec())
        }

        if (event.repeatSeconds(3)) {
            val index = upComingArrows.indexOfLast { it.getLivingTime() > 60 }
            for (i in 0..index) {
                // upComingArrows.remove()
            }
        }
    }

    private fun onArrowSpawn(arrow: EntityArrow) {
        val match = upComingArrows.firstOrNull { it.isOnParabola(arrow) } ?: return
        playerArrows.add(SkyblockArrow(arrow, match.pierce, match.canHitEnderman))
        upComingArrows.remove(match)
        LorenzDebug.log("Added Arrow, needs to find still: ${upComingArrows.count()}")
    }

    private const val ANGLE_TOLERANCE = PI * (4.0 / 3.0)
    private const val DISTANCE_TOLERANCE = 4.0
    private const val TICK_TO_CATCH = 1

    private fun isOnParabola(origin: LorenzVec, direction: LorenzVec, tick: Int, arrow: EntityArrow): Boolean {
        val p = TICK_TO_CATCH

        if (!(parabola(origin, direction, p).distance(arrow.getLorenzVec()) < DISTANCE_TOLERANCE)) return false

        // TODO Debug Angle
        val angleDiffer = arrow.getMotionLorenzVec() // TODO fix that Multiarrow has 0 MotionVector
            .angleInRad(
                vectorFromPoints(
                    parabola(origin, direction, p), parabola(origin, direction, p + 1)
                )
            )
        LorenzDebug.log("Soll: $ANGLE_TOLERANCE, Ist: $angleDiffer")
        return angleDiffer < ANGLE_TOLERANCE
    }

    // Adjusted values from Minecraft uses because EntityArrow.Update is only called every second tick
    private val GRAVITY = 0.024 // Minecraft Default = 0.05
    private val DRAG = 0.995 // Minecraft Default = 0.99

    private fun parabola(origin: LorenzVec, direction: LorenzVec, time: Int): LorenzVec {
        val mt = DRAG.pow(time - 1)
        val x = direction.x * mt * time
        val z = direction.z * mt * time
        val y = (direction.y * mt - (GRAVITY * (mt - 1) / (DRAG - 1))) * time

        val travel = LorenzVec(x, y, z)
        return origin.add(travel)
    }

    private fun onArrowDeSpawn(arrow: EntityArrow) {
        val playerArrow = playerArrows.firstOrNull { it.base == arrow } ?: return
        val hitEntity = MobData.currentSkyblockMobs.firstOrNull {
            it.baseEntity.getPrevLorenzVec().distance(arrow.getLorenzVec()) < 4.0
        }
        if (hitEntity == null) {
            if (config.arrowDebug) {
                LorenzDebug.log("Arrow hit the ground")
            }
        } else {
            EntityKill.addToMobHitList(hitEntity, hitTrigger.Bow)
        }
        playerArrows.remove(playerArrow)
        LorenzDebug.log(arrowTrail[arrow].toString())
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        playerArrows.clear()
    }

    @SubscribeEvent
    fun onTickForHighlight(event: LorenzRenderWorldEvent) {
        if (!config.arrowDebug) return
        playerArrows.forEach {
            event.drawWaypointFilled(it.base.getLorenzVec(), LorenzColor.GOLD.toColor())
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!config.arrowDebug) return
        upComingArrows.forEach {
            event.draw3DLine(
                it.origin, it.direction.normalize().multiply(50)
                    .add(it.origin), LorenzColor.LIGHT_PURPLE.toColor(), 5, true
            )
            for (i in 0..50) {
                event.draw3DLine(
                    it.parabola(i), it.parabola(i + 1), LorenzColor.RED.toColor(), 5, true
                )
                val color = if (i != TICK_TO_CATCH) LorenzColor.DARK_RED else LorenzColor.YELLOW
                event.drawWaypointFilled(it.parabola(i), color.toColor())
            }
        }
        val player = Minecraft.getMinecraft().thePlayer
        val position = player.getPositionEyes(event.partialTicks).toLorenzVec()
        val direction = player.getLook(event.partialTicks).toLorenzVec()
        event.draw3DLine(position, position.add(direction), LorenzColor.DARK_GREEN.toColor(), 5, true)

        renderRealArrowLineList.forEach {
            event.draw3DLine(it.start, it.end, LorenzColor.GREEN.toColor(), 5, true)
            event.drawWaypointFilled(it.end, LorenzColor.DARK_GREEN.toColor())
        }
        renderArrowDetectLineList.forEach {
            event.draw3DLine(it.start, it.end, LorenzColor.YELLOW.toColor(), 10, true)
        }
    }

    @SubscribeEvent
    fun onArrowLooseEvent(event: ArrowLooseEvent) {
        LorenzDebug.log(event.charge.toString())
    }

}
