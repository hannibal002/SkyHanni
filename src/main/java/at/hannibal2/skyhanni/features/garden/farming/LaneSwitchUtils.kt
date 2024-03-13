package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import kotlin.math.absoluteValue

object LaneSwitchUtils {

    enum class Direction {
        WEST_EAST,
        NORTH_SOUTH,
        ;
    }

    enum class Value {
        MIN,
        MAX,
        TOP,
        BOTTOM,
        ;
    }

    fun getFarmBounds(plotIndex: Int, current: LorenzVec, last: LorenzVec): List<LorenzVec>? {
        if (GardenPlotAPI.plots[plotIndex].isBarn() || plotIndex == 12) return null
        val xVelocity = current.x - last.x
        val zVelocity = current.z - last.z
        return if (xVelocity.absoluteValue > zVelocity.absoluteValue) {
            var xValueMin = 0.0
            var xValueMax = 0.0

            for (i in 0..4) {
                if (isBoundaryPlot(plotIndex - i, Direction.WEST_EAST, Value.MIN)) {
                    xValueMin = GardenPlotAPI.plots[plotIndex - i].box.minX; break
                }
            }
            for (i in 0..4) {
                if (isBoundaryPlot(plotIndex + i, Direction.WEST_EAST, Value.MAX)) {
                    xValueMax = GardenPlotAPI.plots[plotIndex + i].box.maxX; break
                }
            }

            val a = LorenzVec(xValueMin, current.y, current.z)
            val b = LorenzVec(xValueMax, current.y, current.z)
            listOf(a, b)
        } else if (xVelocity.absoluteValue < zVelocity.absoluteValue) {
            // i * 5 because going vertically is always 5 plots before or after the current
            var zValueTop = 0.0
            var zValueBottom = 0.0

            for (i in 0..4) {
                if (isBoundaryPlot(plotIndex - (i * 5), Direction.NORTH_SOUTH, Value.TOP)) {
                    zValueTop = GardenPlotAPI.plots[plotIndex - (i * 5)].box.minZ; break
                }
            }
            for (i in 0..4) {
                if (isBoundaryPlot(plotIndex + (i * 5), Direction.NORTH_SOUTH, Value.BOTTOM)) {
                    zValueBottom = GardenPlotAPI.plots[plotIndex + (i * 5)].box.maxZ; break
                }
            }

            val a = LorenzVec(current.x, current.y, zValueTop)
            val b = LorenzVec(current.x, current.y, zValueBottom)
            listOf(a, b)
        } else null
    }

    private fun isBoundaryPlot(plotIndex: Int, direction: Direction, value: Value): Boolean {
        return if (direction == Direction.WEST_EAST) {
            if (value == Value.MIN) {
                if (plotIndex - 1 == -1) return true // check if next plot is out of bounds
                //Check if the next plot's border is 240 and therefore in the previous row
                val isNextNewRow = GardenPlotAPI.plots[plotIndex - 1].box.maxX.absoluteValue.round(0) == 240.0
                val isNextUnlocked = GardenPlotAPI.plots[plotIndex - 1].unlocked
                val isNextBarn = GardenPlotAPI.plots[plotIndex - 1].isBarn()
                isNextNewRow || !isNextUnlocked || isNextBarn
            } else {
                if (plotIndex + 1 == 25) return true // check if next plot is out of bounds
                val isNextNewRow = (plotIndex + 1) % 5 == 0
                val isNextUnlocked = GardenPlotAPI.plots[plotIndex + 1].unlocked
                val isNextBarn = GardenPlotAPI.plots[plotIndex + 1].isBarn()
                isNextNewRow || !isNextUnlocked || isNextBarn
            }
        } else if (direction == Direction.NORTH_SOUTH) {
            if (value == Value.TOP) {
                if (plotIndex - 1 == -1 || (plotIndex - 5) < 0) return true // check if next plot is out of bounds
                val isNextUnlocked = GardenPlotAPI.plots[plotIndex - 5].unlocked
                val isNextBarn = GardenPlotAPI.plots[plotIndex - 5].isBarn()
                !isNextUnlocked || isNextBarn
            } else {
                if (plotIndex + 5 > 24) return true // check if next plot is out of bounds
                val isNextUnlocked = GardenPlotAPI.plots[plotIndex + 5].unlocked
                val isNextBarn = GardenPlotAPI.plots[plotIndex + 5].isBarn()
                !isNextUnlocked || isNextBarn
            }
        } else false
    }
}
