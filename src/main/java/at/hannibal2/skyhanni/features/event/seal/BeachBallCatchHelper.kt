package at.hannibal2.skyhanni.features.event.seal

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.CollectionUtils.takeWhileInclusive
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.getPositionLog
import net.minecraft.entity.item.EntityArmorStand
import java.awt.Color

@SkyHanniModule
object BeachBallCatchHelper {

    val register get() = ProfileStorageData.profileSpecific?.movementLog

    private val predictors = mutableMapOf<Int, Predictor>()

    private val NORMAL_BEACH_BALL by lazy {
        //SkullTextureHolder.getTexture("NORMAL_BEACH_BALL")
        "ewogICJ0aW1lc3RhbXAiIDogMTczNjQyNzQ4ODAwNCwKICAicHJvZmlsZUlkIiA6ICIzN2JhNjRkYzkxOTg0OGI4YjZhNDdiYTg0ZDgwNDM3MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTb3lLb3NhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJhZGY5ZDcxMzY3Y2Q2ZTUwNWZiNDhjYWFhNWFjZGNkZmYyYTA5ZjY2YzQ4OGRhZjA0ZDA0NWVlMGJmNTI4ZTEiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="
    }

    fun check(entity: EntityArmorStand) {
        println("Change! ${entity.getStandHelmet()?.getSkullTexture()}")
        if (!entity.wearingSkullTexture(NORMAL_BEACH_BALL)) return
        println("Beach Ball")
        if (register?.get(entity.entityId) != null) return
        register?.set(entity.entityId, mutableListOf())
        predictors[entity.entityId] = Predictor(entity.getLorenzVec())
        println("New Beach Ball!")
    }

    private var prev = emptySet<EntityArmorStand>()

    @HandleEvent
    fun onSkyHanniTick(event: SkyHanniTickEvent) {
        val now = EntityUtils.getEntities<EntityArmorStand>().toSet()
        val diff = now - prev
        prev = now

        diff.forEach {
            check(it)
            DelayedRun.runNextTick { check(it) }
            DelayedRun.runDelayed(2.ticks) { check(it) }
            DelayedRun.runDelayed(3.ticks) { check(it) }
        }

        register?.forEach { (id, list) ->
            val entity = EntityUtils.getEntityByID(id) ?: return@forEach
            list.add(entity.getPositionLog())
        }

        predictors.removeIf { id, predict ->
            val entity = EntityUtils.getEntityByID(id) ?: return@removeIf true
            predict.newData(entity.getLorenzVec())
            false
        }
    }

    @HandleEvent
    fun onSkyHanniRenderWorld(event: SkyHanniRenderWorldEvent) {
        RenderUtils.LineDrawer.draw3D(event.partialTicks) {
/*             register?.forEach { (id, list) ->
                val color = Color(id).addAlpha(50)
                drawPath(list.map { it.position }, color, 3, true, bezierPoint = -1.0)
            } */
            predictors.forEach { (id, predict) ->
                val color = Color(id).addAlpha(255)
                drawPath(predict.prePath, color.addAlpha(50), 3, true, bezierPoint = -1.0)
                drawPath(predict.predictedPath, color, 3, true, bezierPoint = -1.0)
            }
        }
    }

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
            if (new.distanceToPlayer() < 1.0) {
                startIndex = data.lastIndex
                minY = new.y
            }
            prePath = data.subList(startIndex, data.lastIndex)
            predictedPath = if (predictedPath.isEmpty()) emptyList() else predictedPath.drop(1)
            updated++
            // Only update the path once every 3 ticks to reduce flickering of the path
            if (updated <= 3) return
            predictedPath = predict(startIndex, minY)
        }

        fun predict(startIndex: Int, minY: Double): List<LorenzVec> {
            val presentValues = data.lastIndex - startIndex

            val modelList = listOf<(List<LorenzVec>) -> Model>(::SmallPoly, ::AveragePoly, ::SpreadPoly).map { it(data) }
                .filter { it.minimumToPredict <= presentValues }

            if (modelList.isEmpty()) return listOf(data.last())

            val predictions = modelList.map { it.predict(startIndex, data.lastIndex, minY) }.filter {
                val y = it.last().y
                minY - 1 < y && y < minY + 1
            }

            if (predictions.isEmpty()) return listOf(data.last())

            val targets = predictions.map { it.last() }

            val xTarget = targets.map { it.x }.average()
            val zTarget = targets.map { it.z }.average()

            val target = predictions.minBy {
                val last = it.last()
                xTarget - last.x + zTarget - last.z
            }
            return target
        }
    }

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
