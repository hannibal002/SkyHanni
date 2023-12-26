package at.hannibal2.skyhanni.utils

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object LerpUtils {
    fun clampZeroOne(f: Float): Float {
        return max(0.0, min(1.0, f.toDouble())).toFloat()
    }

    fun sigmoid(`val`: Float): Float {
        return (1 / (1 + exp(-`val`.toDouble()))).toFloat()
    }

    private const val sigmoidStr = 8f
    private val sigmoidA = -1 / (sigmoid(-0.5f * sigmoidStr) - sigmoid(0.5f * sigmoidStr))
    private val sigmoidB = sigmoidA * sigmoid(-0.5f * sigmoidStr)
    fun sigmoidZeroOne(f: Float): Float {
        var f = f
        f = clampZeroOne(f)
        return sigmoidA * sigmoid(sigmoidStr * (f - 0.5f)) - sigmoidB
    }

    fun lerp(a: Float, b: Float, amount: Float): Float {
        return b + (a - b) * clampZeroOne(amount)
    }
}

