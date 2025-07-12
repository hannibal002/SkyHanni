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
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

// https://wiki.hypixel.net/Sirih
@SkyHanniModule
object SirihHelper {

    private val config get() = SkyHanniMod.feature.crimsonIsle

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
    fun onChat(event: SkyHanniChatEvent) {
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
