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
    private val isEnabled get() = config.organDonorAccessoryConfig.muteWhenAllFound && config.waypointsConfig.types.foundCorpse

    private var allCorpsesFound = false

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    private fun onPlaySound(event: PlaySoundEvent) {
        if (!isEnabled || !allCorpsesFound) return
        if (event.soundName == "block.note_block.harp") {
            event.cancel()
        }
    }

    @HandleEvent
    private fun onCorpseFound(event: CorpseFoundEvent) {
        allCorpsesFound = event.isLastCorpse
    }

    @HandleEvent
    private fun onWorldChange() {
        allCorpsesFound = false
    }
}
