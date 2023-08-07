package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureMessageShortener {
    private var nextIsDoubleHook: Boolean = false

    private val config get() = SkyHanniMod.feature.fishing

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.shortenFishingMessage && !config.compactDoubleHook) return
        if (doubleHookMessages.contains(event.message)) {
            event.blockedReason = "double_hook"
            nextIsDoubleHook = true
        }
    }

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.shortenFishingMessage && !config.compactDoubleHook) return
        val seaCreature = event.seaCreature
        event.chatEvent.blockedReason = "sea_creature_caught"

        var message = if (config.shortenFishingMessage) {
            "§9You caught a $seaCreature§9!"
        } else event.chatEvent.message

        if (config.compactDoubleHook && nextIsDoubleHook) {
            nextIsDoubleHook = false
            message = "§e§lDOUBLE HOOK! $message"
        }
        LorenzUtils.chat(message)

        if (seaCreature.fishingExperience == 0) {
            LorenzUtils.debug("no fishing exp set for " + seaCreature.displayName)
        }
    }

    companion object {
        private val doubleHookMessages = setOf(
            "§eIt's a §r§aDouble Hook§r§e! Woot woot!",
            "§eIt's a §r§aDouble Hook§r§e!"
        )
    }
}