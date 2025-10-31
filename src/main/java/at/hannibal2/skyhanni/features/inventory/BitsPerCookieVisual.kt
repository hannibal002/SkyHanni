package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.BitsApi
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.RegexUtils.firstMatcherWithIndex
import at.hannibal2.hanni.utils.RegexUtils.indexOfFirstMatch
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object BitsPerCookieVisual {

    private val config get() = HanniMod.feature.misc.bits

    private val boosterCookie = "BOOSTER_COOKIE".toInternalName()

    private val patternGroup = RepoPattern.group("cookie.bits")

    /**
     * REGEX-TEST: §dBooster Cookie
     * REGEX-FAIL: §6Booster Cookie
     */
    private val wrongCookiePattern by patternGroup.pattern(
        "wrong", "§[de]Booster Cookie",
    )

    /**
     * REGEX-TEST: §7Amount: §a1§7x
     * REGEX-TEST: §5§o§7Amount: §a1§7x
     * REGEX-TEST: §5§o§6Booster Cookie §8x6
     */
    private val amountPattern by patternGroup.pattern(
        "amount", "(?:§5§o)?(?:§6Booster Cookie §8x|§7Amount: §a)(?<amount>\\d+).*",
    )

    /**
     * REGEX-TEST: §7§b4 §7days:
     * REGEX-TEST: §5§o§7§b4 §7days:
     */
    private val timePattern by patternGroup.pattern(
        "time", "(?:§5§o)?§7§b4 §7days:",
    )

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return
        if (event.itemStack.getInternalNameOrNull() != boosterCookie) return
        if (wrongCookiePattern.matches(event.itemStack.displayName)) return
        var timeReplaced = false

        val toolTip = event.toolTip
        val (cookieAmount, loreIndex) = amountPattern.firstMatcherWithIndex(toolTip) {
            group("amount").toInt() to it
        } ?: (1 to 0)
        val positionIndex = timePattern.indexOfFirstMatch(toolTip)?.also {
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
