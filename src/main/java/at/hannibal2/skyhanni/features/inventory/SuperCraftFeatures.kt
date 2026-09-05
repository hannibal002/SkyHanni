package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
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
     * REGEX-TEST: §eYou Supercrafted §r§r§r§aEnchanted Ender Pearl§r§e!
     * REGEX-TEST: §eYou Supercrafted §r§r§r§9Enchanted Mithril §r§8x3§r§e!
     */
    private val craftedPattern by RepoPattern.pattern(
        "inventory.supercrafting.craft.new",
        "§eYou Supercrafted §r§r§r§.(?<item>[^§]+)(?:§r§8x(?<amount>[\\d,]+))?§r§e!",
    )
    private val config get() = SkyHanniMod.feature.inventory.gfs

    internal fun parseCraftedItem(message: String): PrimitiveItemStack? = craftedPattern.matchMatcher(message) {
        PrimitiveItemStack(NeuInternalName.fromItemName(group("item")), group("amount")?.formatInt() ?: 1)
    }

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.superCraftGFS) return
        val craftedItem = parseCraftedItem(event.message) ?: return
        if (!SackApi.sackListInternalNames.contains(craftedItem.internalName.asString())) return
        DelayedRun.runNextTick {
            GetFromSackApi.getFromChatMessageSackItems(craftedItem)
        }
    }
}
