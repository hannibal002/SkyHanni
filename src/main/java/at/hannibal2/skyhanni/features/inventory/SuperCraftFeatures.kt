package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SuperCraftFeatures {

    /**
     * REGEX-TEST: You Supercrafted Enchanted Ender Pearl!
     * REGEX-TEST: You Supercrafted Enchanted Mithril x3!
     */
    val craftedPattern by RepoPattern.pattern(
        "inventory.supercrafting.craft.colorless",
        "You Supercrafted (?<item>.*)(?:x(?<amount>[\\d,]+))?!",
    )
    private val config get() = SkyHanniMod.feature.inventory.gfs

    @HandleEvent
    private fun onSystemMessage(event: SystemMessageEvent.Allow) {
        if (!config.superCraftGFS) return
        val (internalName, amount) = craftedPattern.matchMatcher(event.cleanMessage) {
            NeuInternalName.fromItemName(group("item")) to (group("amount")?.formatInt() ?: 1)
        } ?: return
        if (!SackApi.sackListInternalNames.contains(internalName.asString())) return
        DelayedRun.runNextTick {
            GetFromSackApi.getFromChatMessageSackItems(PrimitiveItemStack(internalName, amount))
        }
    }
}
