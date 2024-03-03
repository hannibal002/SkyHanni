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
        "§eYou Supercrafted §r§r§r§[^.*]([^x§]+?)(?:§r§8x(\\d+))?§r§e!"
    )
    private val config get() = SkyHanniMod.feature.inventory.gfs

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.superCraftGFS) return
        craftedPattern.matchMatcher(event.message) {
            if (GetFromSackAPI.sackListInternalNames.contains(NEUInternalName.fromItemName(this.group(1)).asString())) {
                DelayedRun.runNextTick {
                    GetFromSackAPI.getFromChatMessageSackItems(PrimitiveItemStack(NEUInternalName.fromItemName(this.group(1)), this.group(2)?.toInt() ?: 1));
                }
            }
        }
    }
}
