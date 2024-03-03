package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SuperCraftFeatures {
    private val regex = "§eYou Supercrafted §r§r§r§[^.*]([^x§]+?)(?:§r§8x(\\d+))?§r§e!".toRegex();
    private val config get() = SkyHanniMod.feature.inventory.gfs

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.superCraftGFS) return
        regex.matchEntire(event.message)?.let {
            if (GetFromSackAPI.sackListInternalNames.contains(NEUInternalName.fromItemName(it.groups[1].toString()).asString())) {
                DelayedRun.runNextTick {
                    GetFromSackAPI.getFromChatMessageSackItems(PrimitiveItemStack(NEUInternalName.fromItemName(it.groups[1].toString()), it.groups[2]?.value?.toInt() ?: 1));
                }
            }
        }
    }
}
