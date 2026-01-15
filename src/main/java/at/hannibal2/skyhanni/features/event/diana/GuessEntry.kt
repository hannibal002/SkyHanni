package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.toLorenzVec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class GuessEntry(
    val guesses: List<LorenzVec>,
    var burrowType: BurrowType = BurrowType.UNKNOWN,
    var currentIndex: Int = 0,
    val spadeGuess: Boolean = false,
    var ignoreParticleCheckUntil: SimpleTimeMark = SimpleTimeMark.now(),
    var ignoreInvalidBlock: Boolean = false,
) {
    fun getCurrent(): LorenzVec = guesses[currentIndex]
    fun contains(vec: LorenzVec): Boolean {
        return guesses.contains(vec)
    }

    fun checkRemove(reason: StringBuilder): Boolean {
        // remove guesses older than 30 minutes
        GriffinBurrowHelper.getTimer(this)?.passedSince()?.let {
            if (it > 30.minutes) {
                reason.append("expired (30min active) ")
                return true
            }
        }

        if (shouldKeepGuess()) return false

        var shouldMove = false
        if (!GriffinBurrowHelper.isBlockValid(this.getCurrent()) && !spadeGuess && !ignoreInvalidBlock) {
            reason.append("invalid block ")
            shouldMove = true
        }

        if (GriffinBurrowHelper.shouldBurrowParticlesBeVisible(timeInPast = 1.seconds) &&
            !GriffinBurrowParticleFinder.containsBurrow(this.getCurrent()) && // burrow is not found
            this.getCurrent().distanceSq(MinecraftCompat.localPlayer.position().toLorenzVec()) < 900 // within 30 blocks
        ) {
            reason.append("particles not found when they should have been ")
            shouldMove = true
        }

        if (shouldMove && !attemptMove()) return true

        return false
    }

    fun attemptMove(): Boolean {
        val nextIndex = currentIndex + 1
        if (nextIndex in guesses.indices) {
            currentIndex = nextIndex
            GriffinBurrowHelper.update()
            BurrowGuessEvent(this, "moving").post()
            return true
        } else return false
    }

    private fun shouldKeepGuess(): Boolean {

        // burrows that are known from the previous dug even if particles don't update
        // or inaccurate precise guesses
        if (ignoreParticleCheckUntil.passedSince() < 0.milliseconds) return true

        // don't attempt to move mob burrows if a mob is alive
        if (GriffinBurrowHelper.mobAlive && this.burrowType == BurrowType.MOB) return true

        return false
    }

    override fun toString(): String = buildString {
        // Format the main guess (current index)
        val current = getCurrent()
        val typeStr = if (burrowType == BurrowType.UNKNOWN) "Guess" else burrowType.name
        append("$typeStr[${current.x}, ${current.y}, ${current.z}]")

        // Format additional guesses if any
        val otherGuesses = guesses.filterIndexed { index, _ -> index != currentIndex }
        if (otherGuesses.isNotEmpty()) {
            if (isNotEmpty()) append(", additional: ")
            append(
                otherGuesses.joinToString(", ") { vec ->
                    val type = if (burrowType == BurrowType.UNKNOWN) "Guess" else burrowType.name
                    "$type[${vec.x}, ${vec.y}, ${vec.z}]"
                },
            )
        }

        // Add other non-default values
        val nonDefaults = mutableListOf<String>()

        if (currentIndex != 0) {
            nonDefaults.add("index=$currentIndex")
        }

        if (spadeGuess) {
            nonDefaults.add("inaccurate=true")
        }

        if (ignoreParticleCheckUntil != SimpleTimeMark.now()) {
            nonDefaults.add("ignoreUntil=${ignoreParticleCheckUntil.timeUntil()}")
        }

        if (ignoreInvalidBlock) {
            nonDefaults.add("ignoreBlock=true")
        }

        if (nonDefaults.isNotEmpty()) {
            if (isNotEmpty()) append(", ")
            append(nonDefaults.joinToString(", "))
        }
    }
}
