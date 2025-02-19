package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.event.diana.DianaConfig.GuessLogic
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi.isDianaSpade
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.LorenzVec.Companion.toLorenzVec
import at.hannibal2.skyhanni.utils.PolynomialFitter
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.util.EnumParticleTypes
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PreciseGuessBurrow {
    private val config get() = SkyHanniMod.feature.event.diana

    private val particleLocations = mutableListOf<LorenzVec>()
    private var guessPoint: LorenzVec? = null

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onWorldChange(event: IslandChangeEvent) {
        guessPoint = null
        particleLocations.clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        val type = event.type
        if (type != EnumParticleTypes.DRIP_LAVA) return
        if (event.count != 2) return
        if (event.speed != -0.5f) return
        val currLoc = event.location
        if (lastDianaSpade.passedSince() > 3.seconds) return
        lastLavaParticle = SimpleTimeMark.now()
        if (particleLocations.isEmpty()) {
            particleLocations.add(currLoc)
            return
        }
        val distToLast = particleLocations.last().distance(currLoc)
        if (distToLast == 0.0 || distToLast > 3.0) return
        particleLocations.add(currLoc)

        val guessPosition = guessBurrowLocation() ?: return

        BurrowGuessEvent(guessPosition.down(0.5).roundLocationToBlock(), precise = true).post()
    }

    private fun guessBurrowLocation(): LorenzVec? {
        // A Degree n polynomial can be solved with n+1 unique points
        // The Bézier curve used is a degree 3, so 4 points are needed to solve
        if (particleLocations.size < 4) return null
        val fitters = arrayOf(PolynomialFitter(3), PolynomialFitter(3), PolynomialFitter(3))
        for ((index, location) in particleLocations.withIndex()) {
            val x = index.toDouble()
            val locationArray = location.toDoubleArray()
            for ((i, fitter) in fitters.withIndex()) {
                fitter.addPoint(x, locationArray[i])
            }
        }

        val coefficients = fitters.map { it.fit() }
        val startPointDerivative = coefficients.map { it[1] }.toLorenzVec()

        // How far away from the first point the control point is
        val controlPointDistance = sqrt(24 * sin(getPitchFromDerivative(startPointDerivative) - PI) + 25)

        val t = 3 * controlPointDistance / startPointDerivative.length()

        return coefficients.map { it[0] + it[1] * t + it[2] * t.pow(2) + it[3] * t.pow(3) }.toLorenzVec()
    }

    private fun getPitchFromDerivative(derivative: LorenzVec): Double {
        val xzLength = sqrt(derivative.x.pow(2) + derivative.z.pow(2))
        val pitchRadians = -atan2(derivative.y, xzLength)
        // Solve y = atan2(sin(x) - 0.75, cos(x)) for x from y
        var guessPitch = pitchRadians
        var resultPitch = atan2(sin(guessPitch) - 0.75, cos(guessPitch))
        var windowMax = PI / 2
        var windowMin = -PI / 2
        repeat(100) {
            if (resultPitch < pitchRadians) {
                windowMin = guessPitch
                guessPitch = (windowMin + windowMax) / 2
            } else {
                windowMax = guessPitch
                guessPitch = (windowMin + windowMax) / 2
            }
            resultPitch = atan2(sin(guessPitch) - 0.75, cos(guessPitch))
            if (resultPitch == pitchRadians) return guessPitch
        }
        return guessPitch
    }

    private var lastDianaSpade = SimpleTimeMark.farPast()
    private var lastLavaParticle = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onUseAbility(event: ItemClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val item = event.itemInHand ?: return
        if (!item.isDianaSpade) return
        if (lastLavaParticle.passedSince() < 0.5.seconds) {
            event.cancel()
            return
        }
        particleLocations.clear()
        lastDianaSpade = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Precise Burrow Guess")

        if (!DianaApi.isDoingDiana()) {
            event.addIrrelevant("not doing diana")
            return
        }

        val guess = guessBurrowLocation()
        event.addIrrelevant {
            add("Burrow Guess: " + (guess?.toCleanString() ?: "No Guess"))
            add("Rounded Guess: " + (guess?.down(0.5)?.roundLocationToBlock()?.toCleanString() ?: "No Guess"))
            add("Particle Locations:")
            addAll(
                particleLocations.mapIndexed { index, lorenzVec ->
                    "$index:  ${lorenzVec.toCleanString()}"
                },
            )
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(74, "event.diana.burrowsSoopyGuess", "event.diana.guess")
    }

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess && config.guessLogic == GuessLogic.PRECISE_GUESS
}
