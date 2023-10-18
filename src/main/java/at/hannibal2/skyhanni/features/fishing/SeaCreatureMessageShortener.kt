package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureMessageShortener {
    private val config get() = SkyHanniMod.feature.fishing

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.shortenFishingMessage && !config.compactDoubleHook) return
        val seaCreature = event.seaCreature
        event.chatEvent.blockedReason = "sea_creature_caught"

        var message = if (config.shortenFishingMessage) {
            "§9You caught a ${seaCreature.displayName}§9!"
        } else event.chatEvent.message

        if (config.compactDoubleHook && event.doubleHook) {
            message = "§e§lDOUBLE HOOK! $message"
        }
        LorenzUtils.chat(message)

        if (seaCreature.fishingExperience == 0) {
            LorenzUtils.debug("no fishing exp set for " + seaCreature.name)
        }
    }
}