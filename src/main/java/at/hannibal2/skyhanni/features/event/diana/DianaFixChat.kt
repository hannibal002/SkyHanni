package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi.isDianaSpade
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DianaFixChat {

    private val config get() = SkyHanniMod.feature.event.diana

    private var hasSetParticleQuality = false
    private var lastParticleQualityPrompt = SimpleTimeMark.farPast()
    private var errorCounter = 0
    private var successfulCounter = 0

    private var lastSpadeUse = SimpleTimeMark.farPast()
    private var lastErrorTime = SimpleTimeMark.farPast()
    private var lastGuessPoint = SimpleTimeMark.farPast()
    private var foundGuess = false

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (lastSpadeUse.passedSince() > 1.minutes) return

        if (foundGuess) {
            lastErrorTime = SimpleTimeMark.farPast()
            return
        }
        // particles don't work if a valid target point is close
        if (GriffinBurrowHelper.targetLocation != null) return
        val spadeUse = lastSpadeUse.passedSince()
        if (spadeUse <= 3.seconds) return

        if (lastErrorTime == lastSpadeUse) return
        lastErrorTime = lastSpadeUse

        noGuessFound()
    }

    private fun noGuessFound() {
        errorCounter++
        if (errorCounter == 1) {
            if (successfulCounter < 5) {
                ChatUtils.chat("Could not find Diana guess using particles, please try again.")
            }
            return
        }

        if (!hasSetParticleQuality) {
            if (lastParticleQualityPrompt.passedSince() > 30.seconds) {
                lastParticleQualityPrompt = SimpleTimeMark.now()
                ChatUtils.clickableChat(
                    "§cError detecting Diana Guess! §eClick here to set the particle quality to extreme!",
                    onClick = {
                        hasSetParticleQuality = true
                        HypixelCommands.particleQuality("extreme")
                        errorCounter = 0
                        ChatUtils.chat("Now try again!")
                    },
                )
            }
        } else {
            ErrorManager.logErrorStateWithData(
                "Could not find Diana guess point",
                "Diana guess point failed to load after /pq",
                "errorCounter" to errorCounter,
                "successfulCounter" to successfulCounter,
            )
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val item = event.itemInHand ?: return
        if (!item.isDianaSpade) return

        if (lastSpadeUse.passedSince() > 5.seconds) {
            lastSpadeUse = SimpleTimeMark.now()
            foundGuess = false
        }
    }

    @HandleEvent
    fun onBurrowGuess(event: BurrowGuessEvent) {
        foundGuess = true

        if (hasSetParticleQuality) {
            ChatUtils.chat("Changing the particle quality worked, good job!")
        }

        hasSetParticleQuality = false
        errorCounter = 0

        // This ensures we only count successes after new spade clicks, not the repeated moved guess locations
        if (lastGuessPoint != lastSpadeUse) {
            lastGuessPoint = lastSpadeUse
            lastGuessPoint = SimpleTimeMark.now()
            successfulCounter++
        }
    }

    @HandleEvent
    fun onWorldChange() {
        successfulCounter = 0
    }

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess
}
