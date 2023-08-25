package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object SackAPI {

    data class SackChange(val delta: Int, val internalName: NEUInternalName, val sacks: List<String>)

    private val sackChangeRegex = Regex("""([+-][\d,]+) (.+) \((.+)\)""")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!event.message.removeColor().startsWith("[Sacks]")) return

        val sackAddText = event.chatComponent.siblings.firstNotNullOfOrNull { sibling ->
            sibling.chatStyle?.chatHoverEvent?.value?.formattedText?.removeColor()?.takeIf {
                it.startsWith("Added")
            }
        } ?: ""
        val sackRemoveText = event.chatComponent.siblings.firstNotNullOfOrNull { sibling ->
            sibling.chatStyle?.chatHoverEvent?.value?.formattedText?.removeColor()?.takeIf {
                it.startsWith("Removed")
            }
        } ?: ""

        val sackChangeText = sackAddText + sackRemoveText
        if (sackChangeText.isEmpty()) return

        val otherItemsAdded = sackAddText.contains("other items")
        val otherItemsRemoved = sackRemoveText.contains("other items")

        val sackChanges = ArrayList<SackChange>()
        for (match in sackChangeRegex.findAll(sackChangeText)) {
            val delta = match.groups[1]!!.value.replace(",", "").toInt()
            val item = match.groups[2]!!.value
            val sacks = match.groups[3]!!.value.split(", ")

            val internalName = NEUInternalName.fromItemName(item)
            sackChanges.add(SackChange(delta, internalName, sacks))
        }
        SackChangeEvent(sackChanges, otherItemsAdded, otherItemsRemoved).postAndCatch()
    }

    @SubscribeEvent
    fun sackChange(event: SackChangeEvent) {
        val sackData = ProfileStorageData.profileSpecific?.sacks?.sackContents ?: return

        val justChanged = mutableListOf<NEUInternalName>()

        for (change in event.sackChanges) {
            justChanged.add(change.internalName)
            println("${change.internalName} from the ${change.sacks} changed by ${change.delta}")

            if (sackData.containsKey(change.internalName)) {
                val oldData = sackData[change.internalName]
                sackData[change.internalName] = SackItem(oldData!!.amount + change.delta, oldData.isOutdated)
            } else {
                val amount = if (change.delta > 0) change.delta else 0
                sackData[change.internalName] = SackItem(amount, true)
            }
        }

        if (event.isMissingInfo) {
            for (item in sackData) {
                if (item.key in justChanged) continue
                val oldData = sackData[item.key]
                sackData[item.key] = SackItem(oldData!!.amount, true)
            }
        }
    }
}

data class SackItem(
    val amount: Int,
    val isOutdated: Boolean
)
