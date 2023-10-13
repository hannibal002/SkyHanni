package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GardenBurrowingSporesNotifier {

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return
        if (!SkyHanniMod.feature.garden.burrowingSporesNotification) return

        if (event.message.endsWith("§6§lVERY RARE CROP! §r§f§r§9Burrowing Spores")) {
            LorenzUtils.sendTitle("§9Burrowing Spores!", 5.seconds)
            // would be sent too often, nothing special then
//            ItemBlink.setBlink(NEUItems.getItemStackOrNull("BURROWING_SPORES"), 5_000)
        }
    }
}