package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.plots
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

    fun getFarmBounds(
        plotIndex: Int,
        current: LorenzVec,
        last: LorenzVec,
        enabled: MutableList<Int>
    ): List<LorenzVec>? {
        if (plots[plotIndex].isBarn() || plotIndex == 12) return null
        val xVelocity = current.x - last.x
        val zVelocity = current.z - last.z
        return if (xVelocity.absoluteValue > zVelocity.absoluteValue) {
            var xValueMin = 0.0
            var xValueMax = 0.0

            for (i in 0..4) {
                val isBoundary = isBoundaryPlot(plotIndex - i, Direction.WEST_EAST, Value.MIN, enabled) ?: break
                if (isBoundary) {
                    xValueMin = plots[plotIndex - i].box.minX; break
                }
            }
            for (i in 0..4) {
                val isBoundary = isBoundaryPlot(plotIndex + i, Direction.WEST_EAST, Value.MAX, enabled) ?: break
                if (isBoundary) {
                    xValueMax = plots[plotIndex + i].box.maxX; break
                }
            }

            if (xValueMin == 0.0 || xValueMax == 0.0) return null

            val a = LorenzVec(xValueMin, current.y, current.z)
            val b = LorenzVec(xValueMax, current.y, current.z)
            listOf(a, b)
        } else if (xVelocity.absoluteValue < zVelocity.absoluteValue) {
            // i * 5 because going vertically is always 5 plots before or after the current
            var zValueTop = 0.0
            var zValueBottom = 0.0

            for (i in 0..4) {
                val isBoundary = isBoundaryPlot(plotIndex - (i * 5), Direction.NORTH_SOUTH, Value.TOP, enabled) ?: break
                if (isBoundary) {
                    zValueTop = plots[plotIndex - (i * 5)].box.minZ; break
                }
            }
            for (i in 0..4) {
                val isBoundary =
                    isBoundaryPlot(plotIndex + (i * 5), Direction.NORTH_SOUTH, Value.BOTTOM, enabled) ?: break
                if (isBoundary) {
                    zValueBottom = plots[plotIndex + (i * 5)].box.maxZ; break
                }
            }

            if (zValueTop == 0.0 || zValueBottom == 0.0) return null

            val a = LorenzVec(current.x, current.y, zValueTop)
            val b = LorenzVec(current.x, current.y, zValueBottom)
            listOf(a, b)
        } else null
    }

    private fun getRightIndex(value: Value, enabled: MutableList<Int>, index: Int): Int {
        var int = if (listOf(Value.MIN, Value.MAX).any { it == value }) index else index / 5
        enabled.forEach {
            when (value) {
                Value.MIN -> {
                    if (it < index) int = it
                }
                Value.MAX -> {
                    if (it > index) int = it
                }
                Value.TOP -> {
                    if (it / 5 < int) int = it / 5
                }
                Value.BOTTOM -> {
                    if (it / 5 > int) int = it / 5
                }
            }
        }
        return if (listOf(Value.MIN, Value.MAX).any { it == value }) int else int * 5
    }

    private fun isBoundaryPlot(
        plotIndex: Int,
        direction: Direction,
        value: Value,
        enabled: MutableList<Int>
    ): Boolean? {
        return if (direction == Direction.WEST_EAST) {
            if (value == Value.MIN) {
                if (plotIndex - 1 == -1 || (enabled.find { it == plotIndex } != null && getRightIndex(value, enabled, plotIndex) == plotIndex)) return true // check if next plot is out of bounds
                if (enabled.none { it == plotIndex - 1 } && enabled.size > 0) return null
                //Check if the next plot's border is 240 and therefore in the previous row
                val isNextNewRow = plots[plotIndex - 1].box.maxX.absoluteValue.round(0) == 240.0
                val isNextUnlocked = plots[plotIndex - 1].unlocked
                val isNextBarn = plots[plotIndex - 1].isBarn()
                isNextNewRow || !isNextUnlocked || isNextBarn
            } else {
                if (plotIndex + 1 == 25 || (enabled.find { it == plotIndex } != null && getRightIndex(value, enabled, plotIndex) == plotIndex)) return true // check if next plot is out of bounds
                if (enabled.none { it == plotIndex + 1 } && enabled.size > 0) return null

                val isNextNewRow = (plotIndex + 1) % 5 == 0
                val isNextUnlocked = plots[plotIndex + 1].unlocked
                val isNextBarn = plots[plotIndex + 1].isBarn()
                isNextNewRow || !isNextUnlocked || isNextBarn
            }
        } else if (direction == Direction.NORTH_SOUTH) {
            if (value == Value.TOP) {
                if (plotIndex - 1 == -1 || (plotIndex - 5) < 0 || (enabled.find { it == plotIndex } != null && getRightIndex(value, enabled, plotIndex) == plotIndex)) return true // check if next plot is out of bounds
                if (enabled.none { it == plotIndex - 5 } && enabled.size > 0) return null

                val isNextUnlocked = plots[plotIndex - 5].unlocked
                val isNextBarn = plots[plotIndex - 5].isBarn()
                !isNextUnlocked || isNextBarn
            } else {
                if (plotIndex + 5 > 24 || (enabled.find { it == plotIndex } != null && getRightIndex(value, enabled, plotIndex) == plotIndex)) return true // check if next plot is out of bounds
                if (enabled.none { it == plotIndex + 5 } && enabled.size > 0) return null

                val isNextUnlocked = plots[plotIndex + 5].unlocked
                val isNextBarn = plots[plotIndex + 5].isBarn()
                !isNextUnlocked || isNextBarn
            }
        } else false
    }

    fun enabledContainsPlot(plot: GardenPlotAPI.Plot, enabled: MutableList<Int>): Boolean {
        return enabled.contains(plots.indexOf(plot))
    }

    fun canBeEnabled(plotIndex: Int, enabled: MutableList<Int>): Boolean {
        val plotRow = plotIndex % 5
        val plotColumn = plotIndex / 5
        enabled.forEach { enabledPlot ->
            val difference = (enabledPlot - plotIndex).absoluteValue
            val enabledRow = enabledPlot % 5
            val enabledColumn = enabledPlot / 5
            if (difference >= 6 && (enabledRow != plotRow)) return false
            if (difference <= 5 && (enabledColumn != plotColumn && enabledRow != plotRow)) return false
            if (difference >= 10 && plots.none {
                    (plots.indexOf(it) - plotIndex).absoluteValue == 5 && enabledContainsPlot(it, enabled)
            }) return false
        }
        return true
    }
}
