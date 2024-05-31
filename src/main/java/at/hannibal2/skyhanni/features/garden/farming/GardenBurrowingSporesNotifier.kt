package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GardenBurrowingSporesNotifier {

    private val patternGroup = RepoPattern.group("gardenburrowingsporesnotifier")
    private val messagePattern by patternGroup.pattern(
        "message",
        "§6§lVERY RARE CROP! §r§f§r§9Burrowing Spores$"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.config.burrowingSporesNotification) return

        if (messagePattern.find(event.message)) {
            LorenzUtils.sendTitle("§9Burrowing Spores!", 5.seconds)
            // would be sent too often, nothing special then
//            ItemBlink.setBlink(NEUItems.getItemStackOrNull("BURROWING_SPORES"), 5_000)
        }
    }
}
