package at.lorenz.mod.misc

import at.lorenz.mod.LorenzMod
import at.lorenz.mod.events.LorenzChatEvent
import at.lorenz.mod.utils.GuiRender.renderString
import at.lorenz.mod.utils.LorenzUtils.between
import at.lorenz.mod.utils.LorenzUtils.matchRegex
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CurrentPetDisplay {

    companion object {
        var currentPet: String = ""
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
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

        if (blocked && LorenzMod.feature.misc.petDisplay) {
            event.blockedReason = "pets"
        }
    }


    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!LorenzMod.feature.misc.petDisplay) return
        if (currentPet == "") return

        LorenzMod.feature.misc.petDisplayPos.renderString(currentPet)
    }
}