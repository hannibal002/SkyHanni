package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LorenzVec.Companion.toLorenzVec
import kotlin.math.pow

class PolynomialFitter(private val degree: Int) {
    private val xPointMatrix: ArrayList<DoubleArray> = ArrayList()
    private val yPoints: ArrayList<DoubleArray> = ArrayList()
    fun addPoint(x: Double, y: Double) {
        yPoints.add(doubleArrayOf(y))
        val xArray = DoubleArray(degree + 1)
        for (i in xArray.indices) {
            xArray[i] = x.pow(i)
        }
        xPointMatrix.add(xArray)
    }

    fun fit(): DoubleArray {
        val xMatrix = Matrix(xPointMatrix.toTypedArray())
        val yMatrix = Matrix(yPoints.toTypedArray())
        return ((xMatrix.transpose() * xMatrix).inverse() * xMatrix.transpose() * yMatrix).transpose()[0]
    }

    fun reset() {
        xPointMatrix.clear()
        yPoints.clear()
    }
}

open class BezierFitter(private val degree: Int) {
    val points: MutableList<LorenzVec> = mutableListOf()
    private val fitters = arrayOf(PolynomialFitter(degree), PolynomialFitter(degree), PolynomialFitter(degree))
    fun addPoint(point: LorenzVec) {
        require(point.x.isFinite() && point.y.isFinite() && point.z.isFinite()) { "Points may not contain NaN!" }
        val locationArray = point.toDoubleArray()
        for ((i, fitter) in fitters.withIndex()) {
            fitter.addPoint(points.size.toDouble(), locationArray[i])
        }
        points.add(point)
        lastCurve = null
    }

    fun getLastPoint(): LorenzVec? {
        return points.lastOrNull()
    }

    fun isEmpty(): Boolean {
        return points.isEmpty()
    }

    private var lastCurve: BezierCurve? = null
    fun fit(): BezierCurve? {
        // A Degree n polynomial can be solved with n+1 unique points
        // The Bézier curve used is a degree n, so n + 1 points are needed to solve
        if (points.size <= degree) return null

        if (lastCurve != null) return lastCurve

        val coefficients = fitters.map { it.fit() }
        lastCurve = BezierCurve(coefficients)
        return lastCurve
    }

    fun reset() {
        points.clear()
        fitters.map { it.reset() }
        lastCurve = null
    }
}

class ParticlePathBezierFitter(degree: Int) : BezierFitter(degree) {
    fun solve(): LorenzVec? {
        val bezierCurve = fit() ?: return null

        val startPointDerivative = bezierCurve.derivativeAt(0.0)

        // How far away from the first point the control point is
        val controlPointDistance = LocationUtils.computePitchWeight(startPointDerivative)

        val t = 3 * controlPointDistance / startPointDerivative.length()

        return bezierCurve.at(t)
    }
}

class BezierCurve(private val coefficients: List<DoubleArray>) {
    init {
        require(coefficients.size == 3) { "Coefficients must be for a 3d curve!" }
    }

    fun derivativeAt(t: Double): LorenzVec {
        return coefficients.map {
            var result = 0.0
            val reversed = it.reversedArray().dropLast(1)
            for ((i, coeff) in reversed.withIndex()) {
                result = result * t + coeff * (reversed.size - i)
            }
            result
        }.toLorenzVec()
    }

    fun at(t: Double): LorenzVec {
        return coefficients.map {
            var result = 0.0
            val reversed = it.reversed()
            for (coeff in reversed) {
                result = result * t + coeff
            }
            result
        }.toLorenzVec()
    }
}
