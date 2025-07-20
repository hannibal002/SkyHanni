package at.hannibal2.skyhanni.features.event.yearoftheseal

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.onDisable
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeWhileInclusive
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.LineDrawer
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object BeachBallCatchHelper {

    private val config get() = SkyHanniMod.feature.event.yearOfTheSeal

    private val predictors = mutableMapOf<Int, Predictor>()

    private val NORMAL_BEACH_BALL by lazy { SkullTextureHolder.getTexture("NORMAL_BEACH_BALL") }
//     private val GIANT_BEACH_BALL by lazy { SkullTextureHolder.getTexture("GIANT_BEACH_BALL") }

    fun check(entity: EntityArmorStand) {
        if (entity.wearingSkullTexture(NORMAL_BEACH_BALL)) {
            predictors.putIfAbsent(entity.entityId, Predictor(entity.getLorenzVec(), Variant.NORMAL))
            println("normal detected")
            return
        }
//         if (entity.wearingSkullTexture(GIANT_BEACH_BALL)) {
//             predictors.putIfAbsent(entity.entityId, Predictor(entity.getLorenzVec(), Variant.GIANT))
//             println("giant detected")
//             return
//         }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityEnterWorld(event: EntityEnterWorldEvent<EntityArmorStand>) {
        if (!isEnabled()) return
        DelayedRun.runDelayed(2.ticks) { check(event.entity) }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!isEnabled()) return
        predictors.removeIf { (id, predict) ->
            val entity = EntityUtils.getEntityByID(id) ?: return@removeIf true
            predict.newData(entity.getLorenzVec())
            false
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (predictors.isEmpty()) return
        val color = config.bouncyBallLineColor.toSpecialColor()
        LineDrawer.draw3D(event, 4, true) {
            predictors.forEach { (_, predict) ->
                drawPath(predict.prePath, color.darker(), bezierPoint = -1.0)
                drawPath(predict.predictedPath, color, bezierPoint = -1.0)
            }
        }
        event.renderLandingPosition()
    }

    private fun SkyHanniRenderWorldEvent.renderLandingPosition() {
        if (!config.bouncyBallLandingSpot.get()) return
        val player = WorldRenderUtils.exactLocation(MinecraftCompat.localPlayer, partialTicks).add(y = 1)
        for ((e, predictor) in predictors.map { EntityUtils.getEntityByID(it.key) to it.value }) {
            val entity = e ?: continue
            val location = WorldRenderUtils.exactLocation(entity, partialTicks).copy(y = player.y)
            renderBlock(location, player, predictor)
            renderString(predictor, location)
        }
    }

    private fun SkyHanniRenderWorldEvent.renderString(predictor: Predictor, location: LorenzVec) {
        val counter = predictor.bounceCounter
        val (qualityColor, quality) = when {
            counter < 2 -> "§c" to null // aww man
            counter < 8 -> "§f" to "DECENT"
            counter < 18 -> "§a" to "GOOD"
            counter < 32 -> "§5" to "AMAZING"
            counter < 51 -> "§6" to "IMPRESSIVE"
            else -> "§d" to "INSANE"
        }
        val qualityString = quality?.let { " §8- $qualityColor§l$it!" }.orEmpty()
        drawString(location.add(y = 0.7), "$qualityColor§l$counter$qualityString")
    }

    private fun SkyHanniRenderWorldEvent.renderBlock(location: LorenzVec, player: LorenzVec, predictor: Predictor) {
        val distance = location.distance(player)
        drawFilledBoundingBox(
            location.getAABB(predictor.variant),
            when {
                distance < 0.3 -> Color.GREEN
                distance < 0.9 -> Color.ORANGE
                else -> Color.RED
            },
        )
    }

    private fun LorenzVec.getAABB(variant: Variant): AxisAlignedBB = when (variant) {
        Variant.NORMAL -> add(-0.3, -0.3, -0.3).boundingToOffset(0.6, 0.6, 0.6)
        Variant.GIANT -> add(-0.9, -0.9, -0.9).boundingToOffset(1.8, 1.8, 1.8)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onIslandChange(event: IslandChangeEvent) {
        predictors.clear()
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.bouncyBallLine.onDisable { DelayedRun.runDelayed(3.ticks) { predictors.clear() } }
    }

    private fun isEnabled() = config.bouncyBallLine.get()

    private enum class Variant {
        NORMAL,
        GIANT,
    }

    private class Predictor(start: LorenzVec, val variant: Variant) {

        private val data = mutableListOf<LorenzVec>()

        private var startIndex = 0
        private var minY = 0.0

        var predictedPath = emptyList<LorenzVec>()
            private set

        var prePath = emptyList<LorenzVec>()

        private var updated = 0
        var lastPosition: LorenzVec = start

        init {
            newData(start)
        }

        fun newData(new: LorenzVec) {
            updateDirection(new)
            data.add(new)
            if (new.distanceToPlayer() < 2.1) {
                startIndex = data.lastIndex
                minY = new.y
            }
            prePath = data.subList(startIndex, data.lastIndex)
            predictedPath = if (predictedPath.isEmpty()) emptyList() else predictedPath.drop(1)
            updated++
            // Only update the path once every 3 ticks to reduce flickering of the path
            if (updated <= 3) return
            predictedPath = predict(startIndex, minY)
            updated = 0
        }

        var positive = true

        var lastChange = SimpleTimeMark.now()
        var bounceCounter = 0

        private fun updateDirection(newPosition: LorenzVec) {
            if (lastPosition.distance(newPosition) < 0.3) return
            if (lastChange.passedSince() < 800.milliseconds) return
            val diff = (newPosition - lastPosition).y
            val isPositive = diff > 0
            val wasPositive = positive
            if (isPositive && !wasPositive) {
                bounceCounter++
                lastChange = SimpleTimeMark.now()
            }
            positive = isPositive
            lastPosition = newPosition
        }

        fun predict(startIndex: Int, minY: Double): List<LorenzVec> {
            val presentValues = data.lastIndex - startIndex

            val modelList = mapOf<(List<LorenzVec>) -> Model, Int>(::SmallPoly to 1, ::AveragePoly to 2, ::SpreadPoly to 1)
                .mapKeys { it.key(data) }
                .filterKeys { it.minimumToPredict <= presentValues }

            if (modelList.isEmpty()) return listOf(data.last())

            val predictions = modelList.mapKeys { it.key.predict(startIndex, data.lastIndex, minY) }.filterKeys {
                val y = it.last().y
                minY - 1 < y && y < minY + 1
            }

            if (predictions.isEmpty()) return listOf(data.last())

            val targets = predictions.mapKeys { it.key.last() }

            val xTarget = targets.mapKeys { it.key.x }.weightedAverage()
            val zTarget = targets.mapKeys { it.key.z }.weightedAverage()

            val target = predictions.minBy {
                val last = it.key.last()
                xTarget - last.x + zTarget - last.z
            }
            return target.key
        }
    }

    private fun <K : Number, V : Number> Map<K, V>.weightedAverage() =
        entries.sumOf { it.key.toDouble() * it.value.toDouble() } / sumAllValues()

    private abstract class PolyModel(override val given: List<LorenzVec>) : Model {
        abstract fun getT1(start: Int, current: Int, minY: Double): Int
        abstract fun getT2(start: Int, current: Int, minY: Double): Int
        abstract fun getT3(start: Int, current: Int, minY: Double): Int

        open fun yTransform(t: Int): Double = given[t].y
        open fun dX(start: Int, current: Int, minY: Double) = given[current].x - given[current - 1].x
        open fun dZ(start: Int, current: Int, minY: Double) = given[current].z - given[current - 1].z

        override fun predict(start: Int, current: Int, minY: Double): List<LorenzVec> {
            val t1 = getT1(start, current, minY)
            val t2 = getT2(start, current, minY)
            val t3 = getT3(start, current, minY)
            val y1 = yTransform(t1)
            val y2 = yTransform(t2)
            val y3 = yTransform(t3)

            val a = ((y3 - y1) * (t2 - t1) + (y2 - y1) * (t1 - t3)) /
                ((t3 * t3 - t1 * t1) * (t2 - t1) + (t2 * t2 - t1 * t1) * (t1 - t3))
            val b = ((y2 - y1) - a * (t2 * t2 - t1 * t1)) / (t2 - t1)
            val c = y1 - b * t1 - a * t1 * t1

            fun poly(t: Int) = a * t * t + b * t + c

            val dx = dX(start, current, minY)
            val dz = dZ(start, current, minY)

            val r = (current + 1..current + 300).asSequence().map { it to poly(it) }.takeWhileInclusive { it.second > minY }
                .runningFold(given[t1]) { prev, (_, y) -> LorenzVec(prev.x + dx, y, prev.z + dz) }.toList()
            return r
        }
    }

    private class SmallPoly(given: List<LorenzVec>) : PolyModel(given) {
        override val minimumToPredict = 3
        override fun getT1(start: Int, current: Int, minY: Double): Int = current
        override fun getT2(start: Int, current: Int, minY: Double): Int = current - 1
        override fun getT3(start: Int, current: Int, minY: Double): Int = current - 2
    }

    private class AveragePoly(given: List<LorenzVec>) : PolyModel(given) {
        override val minimumToPredict = 7
        override fun getT1(start: Int, current: Int, minY: Double): Int = current - 1
        override fun getT2(start: Int, current: Int, minY: Double): Int = current - 3
        override fun getT3(start: Int, current: Int, minY: Double): Int = current - 5
        override fun yTransform(t: Int): Double = listOf(t - 1, t, t + 1).map { super.yTransform(t) }.average()
        override fun dX(start: Int, current: Int, minY: Double): Double = listOf(
            given[current].x - given[current - 1].x,
            given[current - 1].x - given[current - 2].x,
            given[current - 2].x - given[current - 3].x,
        ).average()

        override fun dZ(start: Int, current: Int, minY: Double): Double = listOf(
            given[current].x - given[current - 1].x,
            given[current - 1].x - given[current - 2].x,
            given[current - 2].x - given[current - 3].x,
        ).average()
    }

    private class SpreadPoly(given: List<LorenzVec>) : PolyModel(given) {
        override val minimumToPredict = 5
        override fun getT1(start: Int, current: Int, minY: Double): Int = current - 1
        override fun getT2(start: Int, current: Int, minY: Double): Int = (current - start) / 2 + start
        override fun getT3(start: Int, current: Int, minY: Double): Int = start + 1
        override fun yTransform(t: Int): Double = listOf(t - 1, t, t + 1).map { super.yTransform(t) }.average()
    }

    // TODO find correct d and g values
    /*     private class ProjectileModel(override val given: List<LorenzVec>) : Model {

            override val minimumToPredict = 2

            private val d = 0.031
            private val g = 8.0

            override fun predict(start: Int, current: Int, minY: Double): List<LorenzVec> {
                val r0 = given[start]
                val v0 = given[start + 1] - given[start]

                fun getVec(t: Int): LorenzVec {
                    val dt = t - start
                    val drag = 1 / d * (1 - exp(-d * dt))
                    return LorenzVec(
                        r0.x + v0.x * drag,
                        r0.y + (v0.y + g / d) * drag - g / d * dt,
                        r0.z + v0.z * drag,
                    )
                }

                val r = (current + 1..current + 300).asSequence().map(::getVec).takeWhileInclusive { it.y > minY }.toList()
                return r
            }

        } */

    private interface Model {
        fun predict(start: Int, current: Int, minY: Double): List<LorenzVec>
        val given: List<LorenzVec>
        val minimumToPredict: Int
    }
}
