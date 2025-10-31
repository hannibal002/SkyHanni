package at.hannibal2.hanni.features.event.diana

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.features.event.diana.DianaConfig.GuessLogic
import at.hannibal2.hanni.data.ClickType
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.events.ItemClickEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.diana.BurrowGuessEvent
import at.hannibal2.hanni.features.event.diana.DianaApi.isDianaSpade
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.ParticlePathBezierFitter
import at.hannibal2.hanni.utils.SimpleTimeMark
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object PreciseGuessBurrow {
    private val config get() = HanniMod.feature.event.diana

    private val bezierFitter = ParticlePathBezierFitter(3)
    private var newBurrow = true

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onIslandChange() {
        bezierFitter.reset()
        newBurrow = true
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        val type = event.type
        if (type != EnumParticleTypes.DRIP_LAVA) return
        if (event.count != 2) return
        if (event.speed != -0.5f) return
        lastLavaParticle = SimpleTimeMark.now()
        val currLoc = event.location
        if (lastDianaSpade.passedSince() > 3.seconds) return
        if (bezierFitter.isEmpty()) {
            bezierFitter.addPoint(currLoc)
            return
        }
        val distToLast = bezierFitter.getLastPoint()?.distance(currLoc) ?: return

        if (distToLast == 0.0 || distToLast > 3.0) return

        bezierFitter.addPoint(currLoc)

        val guessPosition = guessBurrowLocation() ?: return

        BurrowGuessEvent(guessPosition.down(0.5).roundToBlock(), precise = bezierFitter.count() > 5, new = newBurrow).post()
        newBurrow = false
    }

    private fun guessBurrowLocation(): LorenzVec? = bezierFitter.solve()

    private var lastDianaSpade = SimpleTimeMark.farPast()
    private var lastLavaParticle = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onUseAbility(event: ItemClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val item = event.itemInHand ?: return
        if (!item.isDianaSpade) return
        if (lastLavaParticle.passedSince() < 0.2.seconds) {
            event.cancel()
            return
        }
        bezierFitter.reset()
        lastDianaSpade = SimpleTimeMark.now()
        newBurrow = true
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

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess && config.guessLogic == GuessLogic.PRECISE_GUESS
}
