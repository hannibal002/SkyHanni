package at.hannibal2.skyhanni.features.chat.playerchat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.utils.ComponentMatcher
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.replace
import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class PlayerChatModifier {

    private val config get() = SkyHanniMod.feature.chat.playerMessage
    private val patterns = listOf<Pair<Pattern, ComponentMatcher.() -> IChatComponent>>(
        "\\[(?:VIP|MVP)(?:§.|\\+)*] {1,2}(?:§[7ab6])?(?<name>\\w{2,16})".toPattern() to {
            ChatComponentText("§b").appendSibling(componentOrThrow("name"))
        },
        "§[7ab6](?<name>\\w{2,16})§r(?!§7x)(?!\$)".toPattern() to {
            ChatComponentText("§b").appendSibling(componentOrThrow("name"))
        },
        "§[7ab6](?<name>\\w{2,16}'s)".toPattern() to {
            ChatComponentText("§b").appendSibling(componentOrThrow("name"))
        },
        "§[7ab6](?<name>\\w{2,16} §.)".toPattern() to {
            ChatComponentText("§b").appendSibling(componentOrThrow("name"))
        }
    )

    @SubscribeEvent
    fun onChat(event: SystemMessageEvent) {
        val newMessage = cutMessage(event.chatComponent.intoSpan())

        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, newMessage.intoComponent()) ?: return
    }

    private fun cutMessage(input: ComponentSpan): ComponentSpan {
        var string = input

        if (config.playerRankHider) {
            for ((pattern, func) in patterns) {
                string = string.replace(pattern, func).intoSpan()
            }
        }

        string = MarkedPlayerManager.replaceInChat(string)

        return string
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "chat.playerRankHider", "chat.playerMessage.playerRankHider")
        event.move(3, "chat.chatFilter", "chat.playerMessage.chatFilter")
    }
}
