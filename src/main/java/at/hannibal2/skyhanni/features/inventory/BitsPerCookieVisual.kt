package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcherWithIndex
import at.hannibal2.skyhanni.utils.RegexUtils.indexOfFirstMatch
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object BitsPerCookieVisual {

    private val config get() = SkyHanniMod.feature.misc.bits

    private val boosterCookie = "BOOSTER_COOKIE".toInternalName()

    private val patternGroup = RepoPattern.group("cookie.bits.new")

    /**
     * REGEX-TEST: §dBooster Cookie
     * REGEX-FAIL: §6Booster Cookie
     */
    private val wrongCookiePattern by patternGroup.pattern(
        "wrong", "§[de]Booster Cookie",
    )

    /**
     * REGEX-TEST: Amount: 1x
     * REGEX-TEST: Amount: 1x
     * REGEX-TEST: Booster Cookie x6
     */
    private val amountPattern by patternGroup.pattern(
        "amount", "(?:Booster Cookie x|Amount: )(?<amount>\\d+).*",
    )

    /**
     * REGEX-TEST: 4 days:
     */
    private val timePattern by patternGroup.pattern(
        "time", "4 days:",
    )

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        if (event.itemStack.getInternalNameOrNull() != boosterCookie) return
        if (wrongCookiePattern.matches(event.itemStack.hoverName.formattedTextCompatLeadingWhiteLessResets())) return
        var timeReplaced = false

        val toolTip = event.toolTip
        val (cookieAmount, loreIndex) = amountPattern.firstMatcherWithIndex(toolTip.map { it.string }) {
            group("amount").toInt() to it
        } ?: (1 to 0)
        val positionIndex = timePattern.indexOfFirstMatch(toolTip.map { it.string })?.also {
            timeReplaced = true
            if (config.bulkBuyCookieTime) {
                toolTip.removeAt(it)
            }
        } ?: (loreIndex + 1)

        val gain = BitsApi.bitsPerCookie() * cookieAmount
        val newAvailable = BitsApi.bitsAvailable + gain
        val duration = 4 * cookieAmount

        var index = positionIndex

        if (timeReplaced) {
            if (config.bulkBuyCookieTime) toolTip.add(index++, "§7§b$duration §7days")
            toolTip.add(index++, "")
        } else {
            toolTip.add(index++, "")
            if (config.bulkBuyCookieTime) toolTip.add(index++, "§8‣ §7Cookie Buff for §b$duration §7days")
        }

        if (config.showBitsOnCookie) toolTip.add(index++, "§8‣ §7Gain §b${gain.addSeparators()} Bits")
        if (config.showBitsChangeOnCookie) toolTip.add(
            index++,
            "§8‣ §7Available Bits: §3${BitsApi.bitsAvailable.addSeparators()} §6→ §3${newAvailable.addSeparators()}",
        )
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock &&
        config.let { it.bulkBuyCookieTime || it.showBitsOnCookie || it.showBitsChangeOnCookie }
}
