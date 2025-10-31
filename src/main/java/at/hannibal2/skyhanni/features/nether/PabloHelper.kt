package at.hannibal2.hanni.features.nether

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.GetFromSackApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.hanni.utils.RegexUtils.matchMatchers
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

// https://wiki.hypixel.net/Pablo
@HanniModule
object PabloHelper {

    private val config get() = HanniMod.feature.crimsonIsle

    /**
     * REGEX-TEST: §e[NPC] §5Pablo§f: §b✆ §f§rBring me that §aEnchanted Dandelion §fas soon as you can!
     */
    private val patterns by RepoPattern.list(
        "crimson.pablo.helper",
        "\\[NPC] Pablo: (?:✆ )?Are you available\\? I desperately need an? (?<flower>[\\w ]+) today\\.",
        "\\[NPC] Pablo: (?:✆ )?Bring me that (?<flower>[\\w ]+) as soon as you can!",
        "\\[NPC] Pablo: (?:✆ )?Could you bring me an? (?<flower>[\\w ]+)\\?",
        "\\[NPC] Pablo: (?:✆ )?I really need an? (?<flower>[\\w ]+) today, do you have one you could spare\\?",
    )

    private var lastSentMessage = SimpleTimeMark.farPast()

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        if (lastSentMessage.passedSince() < 5.minutes) return
        val itemName = patterns.matchMatchers(event.message.removeColor()) {
            group("flower")
        } ?: return

        if (InventoryUtils.countItemsInLowerInventory { it.displayName.contains(itemName) } > 0) return

        DelayedRun.runNextTick {
            GetFromSackApi.getFromChatMessageSackItems(
                itemName.toInternalName().makePrimitiveStack(),
                "Click here to grab an $itemName from sacks!",
            )
        }

        lastSentMessage = SimpleTimeMark.now()
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.pabloHelper
}
