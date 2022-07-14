package at.hannibal2.skyhanni.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.GuiRender.renderString
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CurrentPetDisplay {

    companion object {
        var currentPet: String = ""
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyblock) return

        var blocked = false

        val message = event.message
        if (message.matchRegex("§aYou summoned your §r(.*)§r§a!")) {
            currentPet = message.between("your §r", "§r§a")
            blocked = true
        }
        if (message.matchRegex("§cAutopet §eequipped your §7(.*)§e! §a§lVIEW RULE")) {
            currentPet = message.between("] ", "§e!")
            blocked = true
        }
        if (message.matchRegex("§aYou despawned your §r(.*)§r§a!")) {
            currentPet = ""
            blocked = true
        }

        if (blocked && SkyHanniMod.feature.misc.petDisplay) {
            event.blockedReason = "pets"
        }
    }


    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!LorenzUtils.inSkyblock) return

        if (!SkyHanniMod.feature.misc.petDisplay) return
        if (currentPet == "") return

        SkyHanniMod.feature.misc.petDisplayPos.renderString(currentPet)
    }
}