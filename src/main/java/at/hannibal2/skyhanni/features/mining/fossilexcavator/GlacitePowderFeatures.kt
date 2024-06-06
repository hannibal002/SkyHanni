package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GlacitePowderFeatures {
    private val config get() = SkyHanniMod.feature.mining.fossilExcavator

    private val patternGroup = RepoPattern.group("inventory.item.overlay")

    private val glacitePowderPattern by patternGroup.pattern(
        "glacitepowder",
        "Glacite Powder x(?<amount>.*)"
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return

        glacitePowderPattern.matchMatcher(event.stack.cleanName()) {
            val powder = group("amount").formatLong()
            event.stackTip = "§b${NumberUtil.format(powder)}"
        }
    }

    fun isEnabled() = FossilExcavatorAPI.inInventory && config.glacitePowderStack
}
