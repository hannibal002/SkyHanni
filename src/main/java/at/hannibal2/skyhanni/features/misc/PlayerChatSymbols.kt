package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatSymbols {
    private val config get() = SkyHanniMod.feature.misc.chatSymbols
    private val playerChatPattern = ".*§[f7]: .*".toPattern()
    private val chatUsernamePattern =
        "^(?:\\[\\d+] )?(?:\\S )?(?:\\[\\w.+] )?(?<username>\\w+)(?: \\[.+?])?\$".toPattern()
    private val tabUsernamePattern = "^\\[(?<sblevel>\\d+)] (?:\\[\\w+] )?(?<username>\\w+)".toPattern()
    private val nameSymbols = mutableMapOf<String, String>()

    // some code taken from SBA but most changed so that it works
    @SubscribeEvent
    fun onChatReceived(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        if (event.type != 0.toByte()) return

        val message = event.message
        if (!playerChatPattern.matcher(message).matches()) return

        var username = message.removeColor().split(":")[0]

        if (username.contains(">")) {
            username = username.substring(username.indexOf('>') + 1).trim()
        }

        val matcher = chatUsernamePattern.matcher(username)
        if (!matcher.matches()) return
        username = matcher.group("username")

        val talkingPlayer = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(username)

        if (talkingPlayer != null) {
            nameSymbols[username] = talkingPlayer.displayName.siblings[0].unformattedText
        } else {
            val playerNames = Minecraft.getMinecraft().thePlayer.sendQueue.playerInfoMap

            val result = playerNames.stream()
                .filter { npi -> npi.displayName != null }
                .filter { npi -> usernameFromLine(npi.displayName.formattedText) == username }
                .findAny()

            if (result.isPresent) {
                nameSymbols[username] = result.get().displayName.formattedText
            }
        }

        if (nameSymbols.contains(username)) {
            val usernameWithSymbols = nameSymbols[username]!!

            val split = usernameWithSymbols.split("$username ")
            var emblemText = if (split.size > 1) split[1] else ""
            emblemText = StringUtils.removeResets(emblemText)

            if (emblemText != "") {
                event.chatComponent = StringUtils.replaceFirstChatText(event.chatComponent, "$emblemText ", "")

                StringUtils.modifyFirstChatComponent(event.chatComponent) { component ->
                    if (component is ChatComponentText && component.text.contains(username)) {
                        val oldText = component.text
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

                        component.text = component.text.replace(oldText, newText)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    private fun usernameFromLine(input: String): String {
        val usernameMatcher = tabUsernamePattern.matcher(input.removeColor())
        return if (usernameMatcher.find()) usernameMatcher.group("username") else input
    }
}