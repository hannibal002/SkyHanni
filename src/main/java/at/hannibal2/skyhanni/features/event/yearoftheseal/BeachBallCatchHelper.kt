package at.hannibal2.skyhanni.features.event.yearoftheseal

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.CollectionUtils.takeWhileInclusive
import at.hannibal2.skyhanni.utils.ConditionalUtils.onDisable
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand

@SkyHanniModule
object BeachBallCatchHelper {

    private val config get() = SkyHanniMod.feature.event.yearOfTheSeal

    private val predictors = mutableMapOf<Int, Predictor>()

    private val NORMAL_BEACH_BALL by lazy { SkullTextureHolder.getTexture("NORMAL_BEACH_BALL") }

    fun check(entity: EntityArmorStand) {
        if (!entity.wearingSkullTexture(NORMAL_BEACH_BALL)) return
        if (predictors[entity.entityId] != null) return
        predictors[entity.entityId] = Predictor(entity.getLorenzVec())
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityEnterWorld(event: EntityEnterWorldEvent<EntityArmorStand>) {
        if (!isEnabled()) return
        DelayedRun.runDelayed(2.ticks) { check(event.entity) }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSkyHanniTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        predictors.removeIf { id, predict ->
            val entity = EntityUtils.getEntityByID(id) ?: return@removeIf true
            predict.newData(entity.getLorenzVec())
            false
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSkyHanniRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        val color = config.bouncyBallLineColor.toSpecialColor()
        RenderUtils.LineDrawer.draw3D(event.partialTicks) {
            predictors.forEach { (_, predict) ->
                drawPath(predict.prePath, color.darker(), 4, true, bezierPoint = -1.0)
                drawPath(predict.predictedPath, color, 8, true, bezierPoint = -1.0)
            }
        }
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

    private class Predictor(start: LorenzVec) {

        private val data = mutableListOf<LorenzVec>()

        private var startIndex = 0
        private var minY = 0.0

        var predictedPath = emptyList<LorenzVec>()
            private set

        var prePath = emptyList<LorenzVec>()

        private var updated = 0

        init {
            newData(start)
        }

        fun newData(new: LorenzVec) {
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
