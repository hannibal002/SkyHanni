package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureMessageShortener {

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!SkyHanniMod.feature.fishing.shortenFishingMessage) return

        val seaCreature = event.seaCreature
        event.chatEvent.blockedReason = "sea_creature_caught"
        LorenzUtils.chat("§9You caught a $seaCreature§9!")
        if (seaCreature.fishingExperience == 0) {
            LorenzUtils.debug("no fishing exp set for " + seaCreature.displayName)
        }
    }
}