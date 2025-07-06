package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector

@SkyHanniModule
object HoppityMuteEggSounds {

    private val config get() = SkyHanniMod.feature.event.hoppityEggs
    private val eggInventory = InventoryDetector(HoppityRabbitTheFishChecker.mealEggInventoryPattern)

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.muteEggSounds || !eggInventory.isInside()) return
        if (!event.isEggSound()) return
        event.cancel()
    }

    private fun PlaySoundEvent.isEggSound(): Boolean =
        soundName == "block.note_block.bit" && distanceToPlayer < 2.0 && volume == 1.0f

}
