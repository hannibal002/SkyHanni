package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.CrimsonIsleReputationApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
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
import kotlin.time.Duration.Companion.seconds

// https://wiki.hypixel.net/Avorius
@SkyHanniModule
object AvoriusHelper {

    private val config get() = SkyHanniMod.feature.crimsonIsle

    private var lastSentMessage = SimpleTimeMark.farPast()

    private val CUP_OF_BLOOD = "CUP_OF_BLOOD".toInternalName()

    private val cupOfBloodPrimitiveStack by lazy { CUP_OF_BLOOD.makePrimitiveStack() }

    /**
     * REGEX-TEST: [NPC] Avorius: I am quite thirsty all the time, it's a rare condition.
     * REGEX-TEST: [NPC] Avorius: There is no sunlight either, it would be quite accommodating for a Vampire.
     * REGEX-TEST: [NPC] Avorius: Why are you looking at me that way? I am not a Vampire, you are!
     */
    private val avoriusLines by RepoPattern.list(
        "crimson.avorius.helper",
        "\\[NPC] Avorius: I am quite thirsty all the time, it's a rare condition\\.",
        "\\[NPC] Avorius: There is no sunlight either, it would be quite accommodating for a Vampire\\.",
        "\\[NPC] Avorius: Why are you looking at me that way\\? I am not a Vampire, you are!",
    )

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (lastSentMessage.passedSince() < 15.seconds) return

        if (!avoriusLines.matches(event.cleanMessage)) return
        if (InventoryUtils.countItemsInLowerInventory { it.getInternalNameOrNull() == CUP_OF_BLOOD } > 0) return

        DelayedRun.runNextTick {
            GetFromSackApi.getFromChatMessageSackItems(
                cupOfBloodPrimitiveStack,
                "Click here to grab a Cup of Blood from sacks!",
            )
        }

        lastSentMessage = SimpleTimeMark.now()
    }

    fun isEnabled() = config.avoriusHelper && CrimsonIsleReputationApi.factionType == FactionType.MAGE
}
