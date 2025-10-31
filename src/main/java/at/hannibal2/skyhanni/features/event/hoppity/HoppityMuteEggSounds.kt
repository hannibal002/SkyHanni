package at.hannibal2.hanni.features.event.hoppity

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.event.hoppity.HoppityEggsConfig
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryDetector
import at.hannibal2.hanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

@HanniModule
object HoppityMuteEggSounds {

    private var lastInInventory: SimpleTimeMark = SimpleTimeMark.farPast()
    private val config get() = HanniMod.feature.event.hoppityEggs
    private val eggInventory = InventoryDetector(
        pattern = HoppityRabbitTheFishChecker.mealEggInventoryPattern,
        onCloseInventory = {
            lastInInventory = SimpleTimeMark.now()
        }
    )

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!eggInventory.isInside() && lastInInventory.passedSince() > 2.seconds) return
        if (!event.isEggSound()) return
        when (config.soundMode) {
            HoppityEggsConfig.EggSoundMode.NO_MODIFICATION -> return
            HoppityEggsConfig.EggSoundMode.MUTE -> return event.cancel()
            HoppityEggsConfig.EggSoundMode.REVERT -> return event.replaceWithOther("random.eat")
        }
    }

    private fun PlaySoundEvent.isEggSound(): Boolean =
        soundName == "block.note_block.bit" && distanceToPlayer < 2.0 && volume == 1.0f
}
