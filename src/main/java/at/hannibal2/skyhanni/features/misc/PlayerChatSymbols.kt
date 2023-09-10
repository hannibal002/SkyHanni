package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatSymbols {
    private val config get() = SkyHanniMod.feature.misc.chatSymbols
    private val playerChatPattern = ".*§[f7]: .*".toPattern()
    private val usernamePattern =
        "^(?:\\[\\d+] )?(?<symbol>\\S)? ?(?:\\[\\w.+] )?(?<username>\\w+)(?: \\[.+?])?\$".toPattern()

    private val nameSymbols = mutableMapOf<String, String>()


    @SubscribeEvent
    fun onChatReceived(event: ClientChatReceivedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        if (event.type != 0.toByte()) return

        val message = event.message.formattedText
        if (!playerChatPattern.matcher(message).matches()) return

        var username = message.removeColor().split(":")[0]

        if (username.contains(">")) {
            username = username.substring(username.indexOf('>') + 1).trim()
        }

        val matcher = usernamePattern.matcher(username)
        if (!matcher.matches()) return
        username = matcher.group("username")

        val talkingPlayer = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(username)

        if (talkingPlayer != null) {
            nameSymbols[username] = talkingPlayer.displayName.siblings[0].unformattedText
        } else {
            val playerNames = Minecraft.getMinecraft().thePlayer.sendQueue.playerInfoMap

            val result = playerNames.stream()
                .filter { npi -> npi.displayName != null }
                .filter { npi -> TabStringType.usernameFromLine(npi.displayName.formattedText) == username }
                .findAny()

            if (result.isPresent) {
                nameSymbols[username] = result.get().displayName.formattedText
            }
        }
    }
}

// todo when compact tab list is merged remove this. leaving as an enum so that transition is easier
enum class TabStringType {
;
    companion object {
        private val usernamePattern = "^\\[(?<sblevel>\\d+)] (?:\\[\\w+] )?(?<username>\\w+)".toPattern()

        fun usernameFromLine(input: String): String {
            val usernameMatcher = usernamePattern.matcher(input.removeColor())
            return if (usernameMatcher.find()) usernameMatcher.group("username") else input
        }
    }
}