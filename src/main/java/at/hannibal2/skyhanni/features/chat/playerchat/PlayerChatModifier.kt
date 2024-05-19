package at.hannibal2.skyhanni.features.chat.playerchat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.utils.StringUtils.applyIfPossible
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatModifier {

    private val config get() = SkyHanniMod.feature.chat.playerMessage
    private val patterns = mutableListOf<Regex>()

    private val patternGroup = RepoPattern.group("chat.modifier")
    private val replaceFirstOldPattern by patternGroup.pattern(
        "firstold",
        "§[7ab6]((?:\\w+){2,16})'s"
    )
    private val replaceSecondOldPattern by patternGroup.pattern(
        "secondold",
        "§[7ab6]((?:\\w+){2,16}) (§.)"
    )
    private val replaceFirstNewPattern by patternGroup.pattern(
        "firstnew",
        "§b\$1's"
    )
    private val replaceSecondNewPattern by patternGroup.pattern(
        "secondnew",
        "§b\$1 \$2"
    )

    //TODO add to repo patterns
    init {
        patterns.add("§[ab6]\\[(?:VIP|MVP)(?:§.|\\+)*] {1,2}(?:§[7ab6])?(\\w{2,16})".toRegex()) // ranked player with prefix everywhere
        patterns.add("§[7ab6](\\w{2,16})§r(?!§7x)(?!\$)".toRegex()) // all players without rank prefix in notification messages
    }

    @SubscribeEvent
    fun onChat(event: SystemMessageEvent) {
        event.applyIfPossible { cutMessage(it) }
    }

    private fun findClickableTexts(chatComponent: IChatComponent, clickEvents: MutableList<ClickEvent>) {
        for (sibling in chatComponent.siblings) {
            findClickableTexts(sibling, clickEvents)
        }
        val clickEvent = chatComponent.chatStyle.chatClickEvent ?: return
        clickEvent.action ?: return
        if (clickEvents.any { it.value == clickEvent.value }) return
        clickEvents.add(clickEvent)
    }

    private fun findHoverTexts(chatComponent: IChatComponent, hoverEvents: MutableList<HoverEvent>) {
        for (sibling in chatComponent.siblings) {
            findHoverTexts(sibling, hoverEvents)
        }
        val hoverEvent = chatComponent.chatStyle.chatHoverEvent ?: return
        hoverEvent.action ?: return
        if (hoverEvents.any { it.value == hoverEvent.value }) return
        hoverEvents.add(hoverEvent)
    }

    private fun cutMessage(input: String): String {
        var string = input

        if (config.playerRankHider) {
            for (pattern in patterns) {
                string = string.replace(pattern, "§b$1")
            }
            //TODO seraid fix later
            string = string.replace("§[7ab6]((?:\\w+){2,16})'s", "§b$1's")
            string = string.replace("§[7ab6]((?:\\w+){2,16}) (§.)", "§b$1 $2")
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
