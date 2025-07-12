package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.hoppity.HoppityEggsConfig
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityMuteEggSounds {

    private var lastInInventory: SimpleTimeMark = SimpleTimeMark.farPast()
    private val config get() = SkyHanniMod.feature.event.hoppityEggs
    private val eggInventory = InventoryDetector(
        pattern = HoppityRabbitTheFishChecker.mealEggInventoryPattern,
        closeInventory = {
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
