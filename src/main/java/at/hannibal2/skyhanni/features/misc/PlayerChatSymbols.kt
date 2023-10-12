package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.misc.compacttablist.TabStringType
import at.hannibal2.skyhanni.mixins.transformers.AccessorChatComponentText
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.getPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// code inspired by SBA but heavily modified to be more functional and actually work
class PlayerChatSymbols {
    private val config get() = SkyHanniMod.feature.misc.chatSymbols
    private val nameSymbols = mutableMapOf<String, String>()

    @SubscribeEvent
    fun onChatReceived(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return

        val username = event.message.getPlayerName() ?: return

        updateSymbolFromTabList(username)

        val usernameWithSymbols = nameSymbols[username] ?: return

        val split = usernameWithSymbols.split("$username ")
        var emblemText = if (split.size > 1) split[1] else ""
        emblemText = emblemText.removeResets()

        if (emblemText == "") return
        event.chatComponent = StringUtils.replaceFirstChatText(event.chatComponent, "$emblemText ", "")

        StringUtils.modifyFirstChatComponent(event.chatComponent) { component ->
            modify(component, username, emblemText)
        }
    }

    private fun updateSymbolFromTabList(username: String) {
        val talkingPlayer = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(username)
        nameSymbols[username] = if (talkingPlayer != null) {
            talkingPlayer.displayName.siblings[0].unformattedText
        } else {
            TabListData.getTabList()
                .find { playerName -> TabStringType.usernameFromLine(playerName) == username } ?: return
        }
    }

    private fun modify(component: IChatComponent, username: String, emblemText: String): Boolean {
        if (component !is ChatComponentText) return false
        component as AccessorChatComponentText
        if (!component.text_skyhanni().contains(username)) return false
        val oldText = component.text_skyhanni()

        component.setText_skyhanni(component.text_skyhanni().replace(oldText, getNewText(emblemText, oldText)))
        return true
    }

    private fun getNewText(emblemText: String, oldText: String): String = when (config.symbolLocation) {
        0 -> "$emblemText $oldText"
        1 -> iconAfterName(oldText, emblemText)

        else -> oldText
    }

    private fun iconAfterName(oldText: String, emblemText: String): String {
        if (!oldText.contains("§f:")) return "$oldText $emblemText "

        // fixing it for when you type a message as the chat isn't split the same
        val ownChatSplit = oldText.split("§f:")
        if (ownChatSplit.size <= 1) return oldText
        return "${ownChatSplit[0]} $emblemText §f:${ownChatSplit[1]}"
    }
}