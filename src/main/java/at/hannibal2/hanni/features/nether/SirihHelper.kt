package at.hannibal2.hanni.features.nether

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.GetFromSackApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.hanni.features.nether.reputationhelper.FactionType
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

// https://wiki.hypixel.net/Sirih
@HanniModule
object SirihHelper {

    private val config get() = HanniMod.feature.crimsonIsle

    private var lastSentMessage = SimpleTimeMark.farPast()

    private val SULPHUR_ORE = "SULPHUR_ORE".toInternalName()

    /**
     * REGEX-TEST: §e[NPC] §dSirih§f: §rOink.
     */
    private val sirihLine by RepoPattern.pattern(
        "crimson.sirih.helper",
        "§e\\[NPC] §dSirih§f: §rOink\\.",
    )

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        if (lastSentMessage.passedSince() < 1.minutes) return
        if (!sirihLine.matches(event.message)) return

        if (InventoryUtils.countItemsInLowerInventory { it.getInternalNameOrNull() == SULPHUR_ORE } > 0) return

        DelayedRun.runNextTick {
            GetFromSackApi.getFromChatMessageSackItems(
                SULPHUR_ORE.makePrimitiveStack(),
                "Click here to grab Sulphur from sacks!",
            )
        }

        lastSentMessage = SimpleTimeMark.now()
    }

    fun isEnabled() = config.sirihHelper && CrimsonIsleReputationHelper.factionType == FactionType.BARBARIAN
}
