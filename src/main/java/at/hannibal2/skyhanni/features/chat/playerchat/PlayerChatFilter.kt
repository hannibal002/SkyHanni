package at.hannibal2.skyhanni.features.chat.playerchat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PlayerSendChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MultiFilter
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class PlayerChatFilter {

    private val filters = mutableMapOf<String, MultiFilter>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        filters.clear()
        var countCategories = 0
        var countFilters = 0

        try {
            val data = event.getConstant("PlayerChatFilter")!!

            for (category in data["filters"].asJsonArray) {
                val jsonObject = category.asJsonObject
                val description = jsonObject["description"].asString
                val filter = MultiFilter()
                filter.load(jsonObject)
                filters[description] = filter

                countCategories++
                countFilters += filter.count()
            }

            LorenzUtils.debug("Loaded $countFilters filters in $countCategories categories from repo")

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onPlayerChat(event: PlayerSendChatEvent) {
        if (!SkyHanniMod.feature.chat.chatFilter) return
        if (event.channel != PlayerMessageChannel.ALL) return

        val message = event.message.lowercase()
        for (filter in filters) {
            filter.value.matchResult(message)?.let {
                filter(event, it)
                return
            }
        }
    }

    private fun filter(event: PlayerSendChatEvent, filter: String) {
        val pattern = Pattern.compile("(.*)?$filter(.*)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(event.message)
        matcher.matches()
        val beginning = matcher.group(1)
        val end = matcher.group(2)

        event.chatComponents.clear()

        val endSplit = end.split(" ")

        for (word in beginning.split(" ")) {
            if (word.isEmpty()) continue
            event.chatComponents.add(coloredChat(word, EnumChatFormatting.GRAY))
        }

        event.chatComponents.add(coloredChat(filter.trim(), EnumChatFormatting.WHITE))

        for (word in endSplit) {
            if (word.isEmpty()) continue
            event.chatComponents.add(coloredChat(word, EnumChatFormatting.GRAY))
        }
    }

    private fun coloredChat(string: String, color: EnumChatFormatting): ChatComponentText {
        val text = ChatComponentText(string)
        text.chatStyle.color = color
        return text
    }
}