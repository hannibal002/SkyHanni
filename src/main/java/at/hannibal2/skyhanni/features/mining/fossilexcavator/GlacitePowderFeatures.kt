package at.hannibal2.hanni.features.mining.fossilexcavator

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.RenderItemTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.cleanName
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object GlacitePowderFeatures {
    private val config get() = HanniMod.feature.mining.fossilExcavator

    private val patternGroup = RepoPattern.group("inventory.item.overlay")

    private val glacitePowderPattern by patternGroup.pattern(
        "glacitepowder",
        "Glacite Powder x(?<amount>.*)"
    )

    @HandleEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return

        glacitePowderPattern.matchMatcher(event.stack.cleanName()) {
            val powder = group("amount").formatLong()
            event.stackTip = "§b${powder.shortFormat()}"
        }
    }

    fun isEnabled() = FossilExcavatorApi.inInventory && config.glacitePowderStack
}
