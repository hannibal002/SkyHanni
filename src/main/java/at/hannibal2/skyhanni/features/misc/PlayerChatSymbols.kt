package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorChatComponentText
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.getPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatSymbols {
    private val config get() = SkyHanniMod.feature.misc.chatSymbols
    private val tabUsernamePattern = "^\\[(?<sblevel>\\d+)] (?:\\[\\w+] )?(?<username>\\w+)".toPattern()
    private val nameSymbols = mutableMapOf<String, String>()

    // code inspired by SBA but heavily modified to be more functional and actually work
    @SubscribeEvent
    fun onChatReceived(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return

        val message = event.message

        val username = message.getPlayerName()
        if (username == "-") return

        val talkingPlayer = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(username)

        if (talkingPlayer != null) {
            nameSymbols[username] = talkingPlayer.displayName.siblings[0].unformattedText
        } else {
            val playerNames = Minecraft.getMinecraft().thePlayer.sendQueue.playerInfoMap

            val result = playerNames.stream()
                .filter { playerName -> playerName.displayName != null }
                .filter { playerName -> usernameFromLine(playerName.displayName.formattedText) == username }
                .findAny()

            if (result.isPresent) {
                nameSymbols[username] = result.get().displayName.formattedText
            }
        }

        if (nameSymbols.contains(username)) {
            val usernameWithSymbols = nameSymbols[username]!!

            val split = usernameWithSymbols.split("$username ")
            var emblemText = if (split.size > 1) split[1] else ""
            emblemText = emblemText.removeResets()

            if (emblemText != "") {
                event.chatComponent = StringUtils.replaceFirstChatText(event.chatComponent, "$emblemText ", "")

                StringUtils.modifyFirstChatComponent(event.chatComponent) { component ->
                    if (component is ChatComponentText) {
                        component as AccessorChatComponentText
                        if ( component.text_skyhanni().contains(username)) {
                            val oldText = component.text_skyhanni()

                            val newText = when (config.symbolLocation) {
                                0 -> "$emblemText $oldText"
                                1 -> {
                                    // fixing it for when you type a message as the chat isn't split the same
                                    if (oldText.contains("§f:")) {
                                        val ownChatSplit = oldText.split("§f:")
                                        if (ownChatSplit.size > 1) {
                                            "${ownChatSplit[0]} $emblemText §f:${ownChatSplit[1]}"
                                        } else oldText
                                    } else "$oldText $emblemText "
                                }
                                else -> oldText
                            }

                            component.setText_skyhanni(component.text_skyhanni().replace(oldText, newText))
                            return@modifyFirstChatComponent true
                        }
                        return@modifyFirstChatComponent false
                    }
                    return@modifyFirstChatComponent false
                }
            }
        }
    }

    private fun usernameFromLine(input: String): String {
        val usernameMatcher = tabUsernamePattern.matcher(input.removeColor())
        return if (usernameMatcher.find()) usernameMatcher.group("username") else input
    }
}