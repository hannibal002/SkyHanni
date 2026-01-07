package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestSpawnConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestSpawnSound {
    private val config get() = GardenApi.config.pests.pestSpawn
    private var lastPestSpawnSound = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSound(event: PlaySoundEvent) {
        if (!event.isPestSpawnSound()) return

        when (config.soundMode) {
            PestSpawnConfig.PestSpawnSoundMode.DEFAULT -> return
            PestSpawnConfig.PestSpawnSoundMode.MUTED -> event.cancel()
            PestSpawnConfig.PestSpawnSoundMode.CUSTOM -> {
                event.cancel()
                repeatSpawnSound()
            }
            PestSpawnConfig.PestSpawnSoundMode.PLUMBER -> {
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
                createSound(name, pitch)
            )
        }
    }

    private fun plumberSpawnSound() {
        if (lastPestSpawnSound.passedSince() < 5.seconds) return
        playPlumberTheme(config.sound.name)
    }

    private fun playPlumberTheme(soundName: String) {
        SkyHanniMod.launchCoroutine("pest spawn sound") {
            val noteE = createSound(soundName, 0.890899f)
            val noteC = createSound(soundName, 0.707107f)
            val noteG = createSound(soundName, 1.059463f)
            val noteLowG = createSound(soundName, 0.529732f)

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
        soundName == "note.bassattack" && distanceToPlayer < 2.0 && volume == 1.0f && pitch == 1.4920635f
}
