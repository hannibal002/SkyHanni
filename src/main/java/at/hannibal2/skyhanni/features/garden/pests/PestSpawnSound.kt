package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@SkyHanniModule
object PestSpawnSound {
    private val config get() = GardenApi.config.pests.pestSpawn
    private var lastPestSpawnSound = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = GARDEN)
    private fun onPlaySound(event: PlaySoundEvent) {
        if (!event.isPestSpawnSound()) return

        when (config.soundMode) {
            DEFAULT -> return
            MUTED -> event.cancel()
            CUSTOM -> {
                event.cancel()
                repeatSpawnSound()
            }
            PLUMBER -> {
                event.cancel()
                plumberSpawnSound()
            }
        }
        lastPestSpawnSound = SimpleTimeMark.now()
    }

    @JvmStatic
    fun repeatSpawnSound() {
        if (lastPestSpawnSound.passedSince() < 5.seconds) return
        with(config.sound) {
            SoundUtils.repeatSound(
                repeatFrequency.toLong(),
                repeatAmount,
                createSound(name, pitch, isWarning = true)
            )
        }
    }

    private fun plumberSpawnSound() {
        if (lastPestSpawnSound.passedSince() < 5.seconds) return
        playPlumberTheme(config.sound.name)
    }

    private fun playPlumberTheme(soundName: String) {
        SkyHanniMod.launchCoroutine("pest spawn sound") {
            val noteE = createSound(soundName, 0.890899f, isWarning = true)
            val noteC = createSound(soundName, 0.707107f, isWarning = true)
            val noteG = createSound(soundName, 1.059463f, isWarning = true)
            val noteLowG = createSound(soundName, 0.529732f, isWarning = true)

            noteE.playSound()
            delay((166).toLong())
            noteE.playSound()
            delay((333).toLong())
            noteE.playSound()
            delay((333).toLong())
            noteC.playSound()
            delay((166).toLong())
            noteE.playSound()
            delay((333).toLong())
            noteG.playSound()
            delay((666).toLong())
            noteLowG.playSound()
        }
    }

    private fun PlaySoundEvent.isPestSpawnSound(): Boolean =
        soundName == "block.note_block.bass" && distanceToPlayer < 15.0 && volume == 1.0f && pitch == 1.4920635f
}
