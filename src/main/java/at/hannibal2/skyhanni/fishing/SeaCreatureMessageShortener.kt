package at.hannibal2.skyhanni.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureMessageShortener {

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.fishing.shortenFishingMessage) return

        val seaCreature = SeaCreatureManager.getSeaCreature(event.message)
        if (seaCreature != null) {
            event.blockedReason = "sea_create_caught"
            LorenzUtils.chat("§9You caught a $seaCreature§9!")
            if (seaCreature.fishingExperience == 0) {
                LorenzUtils.debug("no fishing exp set for " + seaCreature.displayName)
            }
        }
    }
}