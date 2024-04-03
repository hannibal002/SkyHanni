package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.chat.ChatSymbols.SymbolLocationEntry
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.misc.compacttablist.TabStringType
import at.hannibal2.skyhanni.mixins.transformers.AccessorChatComponentText
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.getPlayerNameAndRankFromChatMessage
import at.hannibal2.skyhanni.utils.StringUtils.getPlayerNameFromChatMessage
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// code inspired by SBA but heavily modified to be more functional and actually work
class PlayerChatSymbols {

    private val config get() = SkyHanniMod.feature.chat.chatSymbols
    private val nameSymbols = mutableMapOf<String, String>()

    private val patternGroup = RepoPattern.group("misc.chatsymbols")
    private val symbolsPattern by patternGroup.pattern(
        "symbols",
        "^(?<symbols>(?:(?:§\\w)+\\S)+) "
    )
    private val symbolPattern by patternGroup.pattern(
        "symbol",
        "(?:§.)+(\\S)"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return

        val username = event.message.getPlayerNameFromChatMessage() ?: return

        var usernameWithSymbols = TabListData.getTabList()
            .find { playerName -> TabStringType.usernameFromLine(playerName) == username }

        if (usernameWithSymbols != null) {
            nameSymbols[username] = usernameWithSymbols
        }

        usernameWithSymbols = nameSymbols[username] ?: return

        val split = usernameWithSymbols.split("$username ")
        var emblemText = if (split.size > 1) split[1].removeResets() else ""

        var matcher = symbolsPattern.matcher("$emblemText ")
        emblemText = if (matcher.find()) {
            matcher.group("symbols")
        } else ""

        if (emblemText == "") {
            return
        }

        val emblems = mutableListOf<String>()
        matcher = symbolPattern.matcher(emblemText)
        while (matcher.find()) {
            emblems.add(matcher.group(1))
        }

        for (emblem in emblems) {
            event.chatComponent = StringUtils.replaceFirstChatText(event.chatComponent, "$emblem ", "")
        }

        val rankAndName = event.message.getPlayerNameAndRankFromChatMessage() ?: return

        StringUtils.modifyFirstChatComponent(event.chatComponent) { component ->
            modify(component, emblemText, rankAndName)
        }
    }

    private fun modify(component: IChatComponent, emblemText: String, rankAndName: String): Boolean {
        if (component !is ChatComponentText) return false
        component as AccessorChatComponentText
        if (!component.text_skyhanni().contains(rankAndName)) return false
        val oldText = component.text_skyhanni()

        val newText = getNewText(emblemText, oldText, rankAndName)
        component.setText_skyhanni(component.text_skyhanni().replace(oldText, newText))
        return true
    }

    private fun getNewText(emblemText: String, oldText: String, rankAndName: String): String =
        when (config.symbolLocation) {
            SymbolLocationEntry.LEFT -> oldText.replace(rankAndName, "$emblemText $rankAndName")
            SymbolLocationEntry.RIGHT -> oldText.replace(rankAndName, "$rankAndName $emblemText ")
            SymbolLocationEntry.HIDDEN -> oldText
            else -> oldText
        }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(15, "chat.chatSymbols.symbolLocation") { element ->
            ConfigUtils.migrateIntToEnum(element, SymbolLocationEntry::class.java)
        }
    }
}
