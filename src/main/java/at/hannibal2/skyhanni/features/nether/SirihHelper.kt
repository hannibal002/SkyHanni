package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.time.Duration.Companion.minutes

// https://wiki.hypixel.net/Sirih
@SkyHanniModule
object SirihHelper {

    private val config get() = SkyHanniMod.feature.crimsonIsle

    private val sulphurInternalId = "SULPHUR_ORE".toInternalName()
    private var lastSentMessage = SimpleTimeMark.farPast()

    private const val SIRIH_CHAT_MESSAGE = "§e[NPC] §dSirih§f: §rOink."

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (lastSentMessage.passedSince() < 1.minutes) return
        if (!event.message.contains(SIRIH_CHAT_MESSAGE)) return

        if (InventoryUtils.countItemsInLowerInventory { it.getInternalNameOrNull() == sulphurInternalId } > 0) return

        DelayedRun.runNextTick {
            GetFromSackApi.getFromChatMessageSackItems(
                sulphurInternalId.makePrimitiveStack(),
                "Click here to grab an ${sulphurInternalId.itemName.removeColor()} from sacks!",
            )
        }

        lastSentMessage = SimpleTimeMark.now()
    }

    fun isEnabled() = config.sirihHelper && CrimsonIsleReputationHelper.factionType == FactionType.BARBARIAN
}
