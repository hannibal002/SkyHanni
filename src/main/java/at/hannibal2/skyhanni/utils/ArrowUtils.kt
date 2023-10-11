package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.mobs.EntityKill
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import net.minecraft.client.Minecraft
import net.minecraft.entity.projectile.EntityArrow
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.Queue
import kotlin.math.pow

object ArrowUtils {

    private val config get() = SkyHanniMod.feature.dev

    class SkyblockArrow(val base: EntityArrow, val pierce: Int, val canHitEnderman: Boolean) {
        var piercedAmount = 0

    }

    private data class SkyblockArrowSpawn(
        val origin: LorenzVec,
        val direction: LorenzVec,
        val pierce: Int,
        val canHitEnderman: Boolean
    ) {
        fun parabola(time: Int) = parabola(origin, direction, time)
    }

    private val upComingArrows: Queue<SkyblockArrowSpawn> = LinkedList()

    fun newArrows(origin: LorenzVec, facingDirection: LorenzVec, pierce: Int, canHitEnderman: Boolean) =
        newArrows(origin, facingDirection, 1, 0.0, pierce, canHitEnderman)

    fun newArrows(
        origin: LorenzVec,
        facingDirection: LorenzVec,
        amount: Int,
        spread: Double,
        pierce: Int,
        canHitEnderman: Boolean
    ) {
        if (amount < 1) return
        upComingArrows.add(SkyblockArrowSpawn(origin, facingDirection, pierce, canHitEnderman))
        //TODO(Multi Arrow)
    }


    public val playerArrows = mutableSetOf<SkyblockArrow>()

    private val currentArrowsInWorld = mutableSetOf<EntityArrow>()
    private val previousArrowsInWorld = mutableSetOf<EntityArrow>()

    private val renderLineList = mutableListOf<Line>()

    data class Line(val start: LorenzVec, val end: LorenzVec)

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        previousArrowsInWorld.clear()
        previousArrowsInWorld.addAll(currentArrowsInWorld)
        currentArrowsInWorld.clear()
        currentArrowsInWorld.addAll(EntityUtils.getEntities<EntityArrow>())

        //New Arrow
        (currentArrowsInWorld - previousArrowsInWorld).forEach { onArrowSpawn(it) }
        //Arrow Disappeared
        (previousArrowsInWorld - currentArrowsInWorld).forEach { onArrowDeSpawn(it) }

        //currentArrowsInWorld.forEach{arrow ->
        //    val speed = LorenzVec(arrow.posX-arrow.lastTickPosX,arrow.posY-arrow.lastTickPosY,arrow.posZ-arrow.lastTickPosZ).multiply(20).length()
        //    LorenzDebug.log("Arrow Speed: $speed")
        //}
        currentArrowsInWorld.forEach {
            renderLineList.add(
                Line(
                    LorenzVec(it.prevPosX, it.prevPosY, it.prevPosZ),
                    LorenzVec(it.posX, it.posY, it.posZ)
                )
            )
        }
    }

    private fun onArrowSpawn(arrow: EntityArrow) {
        val match = upComingArrows.firstOrNull { isOnParabola(it.origin, it.direction, arrow) } ?: return
        playerArrows.add(SkyblockArrow(arrow, match.pierce, match.canHitEnderman))
        upComingArrows.remove(match)
        LorenzDebug.log("Added Arrow, needs to find still: ${upComingArrows.count()}")
    }
    //arrow.position.toLorenzVec().subtract(it.origin)
    //                .dotPorduct(it.direction.normalize()).absoluteValue < 1.5

    private fun isOnParabola(origin: LorenzVec, direction: LorenzVec, arrow: EntityArrow): Boolean {
        var onIt = false
        for (i in 0..50) { //TODO Quick Solution but I have a better but more complicated in mind
            if(parabola(origin,direction,i).distance(arrow.position.toLorenzVec())  < 3.0){
                onIt = true
            }
        }
        //TODO(account for orientation)
        return onIt
    }

    private const val GRAVITY = 0.05 //0.05 in Ticks
    private const val DRAG = 0.99 //velocity is every Tick reduced by 1%

    /**parabola(origin, direction, time) = origin + [time * direction - (0, GRAVITY * time^2, 0)] * DRAG^time
     * Time is in Ticks*/
    private fun parabola(origin: LorenzVec, direction: LorenzVec, time: Int): LorenzVec {
        val gravityEffect = LorenzVec(0.0, GRAVITY * time * time, 0.0)
        val change = direction.multiply(time)
        val changeWithGravity = change.subtract(gravityEffect)
        val dampening = DRAG.pow(time)
        val travel = changeWithGravity.multiply(dampening)
        return origin.add(travel)
    }

    private fun onArrowDeSpawn(arrow: EntityArrow) {
        val playerArrow = playerArrows.firstOrNull { it.base == arrow } ?: return
        val hitEntity = EntityKill.currentEntityLiving.firstOrNull{LorenzVec(it.prevPosX,it.prevPosY,it.prevPosZ).distance(arrow.position.toLorenzVec()) < 4.0 }
        if(hitEntity == null){
            if(config.arrowDebug){
            LorenzDebug.log("Arrow hit the ground")}
        }else{
            EntityKill.checkAndAddToMobHitList(hitEntity)
        }
        playerArrows.remove(playerArrow)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        playerArrows.clear()
    }

    @SubscribeEvent
    fun onTickForHighlight(event: RenderWorldLastEvent) {
        if(!config.arrowDebug) return
        playerArrows.forEach {
            RenderUtils.drawCylinderInWorld(
                LorenzColor.GOLD.toColor(),
                it.base.position.x.toDouble(),
                it.base.position.y.toDouble(),
                it.base.position.z.toDouble(),
                1.0.toFloat(),
                1.0.toFloat(),
                event.partialTicks
            )
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if(!config.arrowDebug) return
        upComingArrows.forEach {
            event.draw3DLine(
                it.origin,
                it.direction.normalize().multiply(50).add(it.origin),
                LorenzColor.LIGHT_PURPLE.toColor(),
                5,
                true
            )
            for (i in 0..50) {
                event.draw3DLine(
                    it.parabola(i),
                    it.parabola(i + 1),
                    LorenzColor.RED.toColor(),
                    5,
                    true
                )
            }
        }
        val player = Minecraft.getMinecraft().thePlayer
        val position = player.getPositionEyes(event.partialTicks).toLorenzVec()
        val direction = player.getLook(event.partialTicks).toLorenzVec()
        event.draw3DLine(position, position.add(direction), LorenzColor.DARK_GREEN.toColor(), 5, true)

        renderLineList.forEach {
            event.draw3DLine(it.start, it.end, LorenzColor.GREEN.toColor(), 5, true)
        }
    }
}