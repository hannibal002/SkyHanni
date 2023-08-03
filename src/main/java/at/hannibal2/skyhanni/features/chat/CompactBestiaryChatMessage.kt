package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.makeAccessible
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ChatLine
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.ReflectionHelper

class CompactBestiaryChatMessage {

    var inBestiary = false
    var bestiaryDescription = mutableListOf<String>()
    var acceptMoreDescription = true
    var command = ""

    var lastBorder: IChatComponent? = null
    var lastEmpty: IChatComponent? = null

    var milestoneMessage: String? = null

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.chat.compactBestiaryMessage) return

        val titleMessage = "                                  §6§lBESTIARY"
        val border = "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"

        val message = event.message

        if (message == border) {
            lastBorder = event.chatComponent
        }
        if (message == " ") {
            lastEmpty = event.chatComponent
        }

        if (message == titleMessage) {
            event.blockedReason = "bestiary"
            val chatGUI = Minecraft.getMinecraft().ingameGUI.chatGUI
            val chatLinesField = ReflectionHelper.findField(chatGUI.javaClass, "chatLines")
            val chatLines = chatLinesField.makeAccessible().get(chatGUI) as MutableList<ChatLine>

            lastBorder?.let { chat -> chatLines.removeIf { it.chatComponent === chat } }
            lastEmpty?.let { chat -> chatLines.removeIf { it.chatComponent === chat } }
            chatGUI.refreshChat()

            lastBorder = null
            lastEmpty = null

            for (sibling in event.chatComponent.siblings) {
                sibling.chatStyle?.chatClickEvent?.let {
                    command = it.value
                }
            }
            inBestiary = true
            bestiaryDescription.add(message.trim())
        } else if (inBestiary) {
            event.blockedReason = "bestiary"
            if (message == border) {
                inBestiary = false

                val title = bestiaryDescription[1]
                LorenzUtils.hoverableChat("§6§lBESTIARY §r$title", bestiaryDescription.dropLast(1), command)
                bestiaryDescription.clear()
                acceptMoreDescription = true

            } else {
                milestoneMessage?.let {
                    LorenzUtils.chat("§6§lBESTIARY MILESTONE $it")
                    milestoneMessage = null
                }
                if (message.endsWith("§6§lBESTIARY MILESTONE")) {
                    acceptMoreDescription = false
                    milestoneMessage = message
                }
                if (acceptMoreDescription) {
                    bestiaryDescription.add(message.trim())
                }
            }
        }
    }
}
