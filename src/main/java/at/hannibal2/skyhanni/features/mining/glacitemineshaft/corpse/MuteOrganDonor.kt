package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.mining.CorpseFoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

@SkyHanniModule
object MuteOrganDonor {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.organDonorAccessoryConfig

    private var allCorpsesFound = false

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.muteWhenAllFound || !allCorpsesFound) return
        if (event.soundName == "block.note_block.harp") {
            event.cancel()
        }
    }

    @HandleEvent(priority = 1)
    fun onCorpseFound(event: CorpseFoundEvent) {
        allCorpsesFound = event.isLastCorpse
        if (config.muteWhenAllFound && event.isLastCorpse) {
            ChatUtils.chat("The Organ Donor's sounds have been muted as all Frozen Corpses in this Mineshaft have been found.")
        }
    }

    @HandleEvent
    fun onWorldChange() {
        allCorpsesFound = false
    }
}
