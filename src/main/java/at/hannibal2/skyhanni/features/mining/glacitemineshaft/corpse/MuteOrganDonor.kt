package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.mining.CorpseFoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteOrganDonor {

    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft

    private var allCorpsesFound = false

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.organDonorAccessoryConfig.muteWhenAllFound || !config.corpseLocator.enabled || !allCorpsesFound) return
        if (event.soundName == "block.note_block.harp") {
            event.cancel()
        }
    }

    @HandleEvent
    fun onCorpseFound(event: CorpseFoundEvent) {
        allCorpsesFound = event.isLastCorpse
    }

    @HandleEvent
    fun onWorldChange() {
        allCorpsesFound = false
    }
}
