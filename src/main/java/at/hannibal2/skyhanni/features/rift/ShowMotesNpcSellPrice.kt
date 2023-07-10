package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI
import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI.motesNpcPrice
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShowMotesNpcSellPrice {

    private val config get() = SkyHanniMod.feature.rift.motes
    val pattern = ".*(?:§\\w)+You have (?:§\\w)+(?<amount>\\d) Grubber Stacks.*".toPattern()

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!isEnabled()) return

        val itemStack = event.itemStack ?: return

        val motesPerItem = itemStack.motesNpcPrice() ?: return
        val size = itemStack.stackSize
        if (size > 1) {
            val motes = motesPerItem * size
            val withBurger = motes + (config.burgerStacks * 5) * motes / 100
            val perWithBurger = motesPerItem + (config.burgerStacks * 5) * motesPerItem / 100
            val burgerText = if(config.burgerStacks>0) "(${config.burgerStacks}x≡) " else ""
            event.toolTip.add("§6NPC price: $burgerText§d${withBurger.addSeparators()} Motes §7($size x §d${perWithBurger.addSeparators()} Motes§7)")
        } else {
            val perWithBurger = motesPerItem + (config.burgerStacks * 5) * motesPerItem / 100
            event.toolTip.add("§6NPC price: §d${perWithBurger.addSeparators()} Motes")
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!RiftAPI.inRift()) return
        pattern.matchMatcher(event.message) {
            config.burgerStacks = group("amount").toInt()
            chat("§6[SkyHanni] Set your McGrubber's burger stacks to ${group("amount")}.")
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.showPrice
}
