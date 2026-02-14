package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi.isDianaSpade
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ParticlePathBezierFitter
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.core.particles.ParticleTypes
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PreciseGuessBurrow {
    private val config get() = SkyHanniMod.feature.event.diana

    private val bezierFitter = ParticlePathBezierFitter(3)
    fun getBezierFitterCount(): Int { return bezierFitter.count() }

    private var lastGuess: GuessEntry? = null

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onIslandChange() {
        bezierFitter.reset()
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        val type = event.type
        if (type != ParticleTypes.DRIPPING_LAVA) return
        if (event.count != 2) return
        if (event.speed != -0.5f) return
        lastLavaParticle = SimpleTimeMark.now()
        val currLoc = event.location
        if (lastDianaSpade.passedSince() > 3.seconds) return
        GriffinBurrowHelper.removeSpadeWarnTitle()
        if (bezierFitter.isEmpty()) {
            bezierFitter.addPoint(currLoc)
            return
        }
        val distToLast = bezierFitter.getLastPoint()?.distance(currLoc) ?: return

        if (distToLast == 0.0 || distToLast > 3.0) return

        bezierFitter.addPoint(currLoc)

        if (bezierFitter.count() < 6) {
            val duration = (6 - bezierFitter.count()) * 100
            BurrowWarpHelper.blockWarp(duration.milliseconds)
        }

        val guessPosition = guessBurrowLocation() ?: return

        val guessEntry = GuessEntry(
            listOf(guessPosition.down(0.5).roundToBlock()),
            spadeGuess = true,
        )

        if (lastGuess?.getCurrent() != guessEntry.getCurrent()) {
            DelayedRun.runOrNextTick {
                lastGuess?.let { GriffinBurrowHelper.removeGuess(it, "moving spade guess", logAsPossibleBurrow = false) }
                BurrowGuessEvent(guessEntry, "spade guess").post()
                lastGuess = guessEntry
            }
        }

    }

    private fun guessBurrowLocation(): LorenzVec? = bezierFitter.solve()

    private var lastDianaSpade = SimpleTimeMark.farPast()
    private var lastLavaParticle = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onUseAbility(event: ItemClickEvent) {
        if (!isEnabled()) return
        val item = event.itemInHand ?: return
        if (!item.isDianaSpade) return
        if (event.clickType != ClickType.RIGHT_CLICK) {
            DelayedRun.runOrNextTick { GriffinBurrowHelper.removeInaccurateIfLooking() }
            return
        }
        if (lastLavaParticle.passedSince() < 0.2.seconds) {
            event.cancel()
            return
        }
        bezierFitter.reset()
        lastGuess = null
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
            add("Rounded Guess: " + (guess?.down(0.5)?.roundToBlock()?.toCleanString() ?: "No Guess"))
            add("Particle Locations:")
            addAll(
                bezierFitter.points.mapIndexed { index, lorenzVec ->
                    "$index:  ${lorenzVec.toCleanString()}"
                },
            )
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(74, "event.diana.burrowsSoopyGuess", "event.diana.guess")
    }

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess
}
