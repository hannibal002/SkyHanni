package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.isRoman
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ReplaceRomanNumerals {
    // Using toRegex here since toPattern doesn't seem to provide the necessary functionality
    private val splitRegex = "((§\\w)|(\\s+)|(\\W))+|(\\w*)".toRegex()
    private val cachedStrings = TimeLimitedCache<String, String>(5.seconds)

    @HandleEvent(priority = HandleEvent.LOW)
    fun onRepoReload(event: RepositoryReloadEvent) {
        cachedStrings.clear()
    }

    fun replaceLine(line: String): String {
        if (!isEnabled()) return line

        return cachedStrings.getOrPut(line) {
            line.replace()
        }
    }

    private fun String.replace() = splitRegex.findAll(this).map { it.value }.joinToString("") {
        it.takeIf { it.isValidRomanNumeral() && it.removeFormatting().romanToDecimal() != 2000 }?.coloredRomanToDecimal() ?: it
    }

    private fun String.removeFormatting() = removeColor().replace(",", "")

    private fun String.isValidRomanNumeral() = removeFormatting().let { it.isRoman() && it.isNotEmpty() }

    private fun String.coloredRomanToDecimal() = removeFormatting().let { replace(it, it.romanToDecimal().toString()) }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && SkyHanniMod.feature.misc.replaceRomanNumerals.get()

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Replace Roman Numerals")
        event.addIrrelevant {
            val map = cachedStrings.toMap()
            add("cachedStrings: (${map.size})")
            for ((original, changed) in map) {
                if (original == changed) {
                    add("unchanged: '$original'")
                } else {
                    add("'$original' -> '$changed'")
                }
            }
        }
    }
}
