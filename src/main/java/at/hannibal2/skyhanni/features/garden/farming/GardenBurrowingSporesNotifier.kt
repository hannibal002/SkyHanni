package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenBurrowingSporesNotifier {

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return
        if (!SkyHanniMod.feature.garden.burrowingSporesNotification) return

        if (event.message.endsWith("§6§lVERY RARE CROP! §r§f§r§9Burrowing Spores")) {
            TitleUtils.sendTitle("§9Burrowing Spores!", 5_000)
            // would be sent too often, nothing special then
//            ItemBlink.setBlink(NEUItems.getItemStackOrNull("BURROWING_SPORES"), 5_000)
        }
    }
}