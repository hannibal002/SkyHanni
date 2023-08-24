package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class SackAPI {

    data class SackChange(val delta: Int, val internalName: NEUInternalName, val sacks: List<String>)

    private val sackChangeRegex = Regex("""([+-][\d,]+) (.+) \((.+)\)""")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!event.message.removeColor().startsWith("[Sacks]")) return

        val sackChanges = ArrayList<SackChange>()

        var sackChangeText = try {
            event.chatComponent.siblings.firstNotNullOf {
                it.chatStyle?.chatHoverEvent?.value?.formattedText
            }
        } catch (e: NoSuchElementException) {
            return
        }

        sackChangeText = sackChangeText.trim().removeColor()

        for (match in sackChangeRegex.findAll(sackChangeText)) {
            val delta = match.groups[1]!!.value.replace(",", "").toInt()
            val item = match.groups[2]!!.value
            val sacks = match.groups[3]!!.value.split(", ")

            val internalName = NEUInternalName.fromItemName(item)

            sackChanges.add(SackChange(delta, internalName, sacks))
        }

        SackChangeEvent(sackChanges).postAndCatch()
    }
}
