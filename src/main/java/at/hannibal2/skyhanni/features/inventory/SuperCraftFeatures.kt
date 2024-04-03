package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SuperCraftFeatures {
    private val craftedPattern by RepoPattern.pattern(
        "inventory.supercrafting.craft",
        "§eYou Supercrafted §r§r§r§.(?<item>[^§]+)(?:§r§8x(?<amount>[0-9,]+))?§r§e!"
    )
    private val config get() = SkyHanniMod.feature.inventory.gfs

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.superCraftGFS) return
        val (internalName, amount) = craftedPattern.matchMatcher(event.message) {
            NEUInternalName.fromItemName(this.group("item")) to (this.group("amount")?.toInt() ?: 1)
        } ?: return
        if (!GetFromSackAPI.sackListInternalNames.contains(internalName.asString())) return
        DelayedRun.runNextTick {
            GetFromSackAPI.getFromChatMessageSackItems(PrimitiveItemStack(internalName, amount))
        }
    }
}
