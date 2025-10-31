package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.GetFromSackApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SackApi
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.formatInt
import at.hannibal2.hanni.utils.PrimitiveItemStack
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object SuperCraftFeatures {

    /**
     * REGEX-TEST: §eYou Supercrafted §r§r§r§aEnchanted Ender Pearl§r§e!
     * REGEX-TEST: §eYou Supercrafted §r§r§r§9Enchanted Mithril §r§8x3§r§e!
     */
    val craftedPattern by RepoPattern.pattern(
        "inventory.supercrafting.craft.new",
        "§eYou Supercrafted §r§r§r§.(?<item>[^§]+)(?:§r§8x(?<amount>[\\d,]+))?§r§e!",
    )
    private val config get() = HanniMod.feature.inventory.gfs

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!config.superCraftGFS) return
        val (internalName, amount) = craftedPattern.matchMatcher(event.message) {
            NeuInternalName.fromItemName(group("item")) to (group("amount")?.formatInt() ?: 1)
        } ?: return
        if (!SackApi.sackListInternalNames.contains(internalName.asString())) return
        DelayedRun.runNextTick {
            GetFromSackApi.getFromChatMessageSackItems(PrimitiveItemStack(internalName, amount))
        }
    }
}
