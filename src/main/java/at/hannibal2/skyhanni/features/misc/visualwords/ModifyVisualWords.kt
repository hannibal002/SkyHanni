package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.convertToFormatted
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import kotlin.time.Duration.Companion.minutes

object ModifyVisualWords {
    private val config get() = SkyHanniMod.feature.gui.modifyWords
    var textCache = TimeLimitedCache<String, String>(5.minutes)

    var modifiedWords = mutableListOf<VisualWord>()

    fun modifyText(originalText: String?): String? {
        var modifiedText = originalText ?: return null
        if (!LorenzUtils.onHypixel) return originalText
        if (!config.enabled) return originalText
        if (!LorenzUtils.inSkyBlock && !config.workOutside) return originalText

        if (modifiedWords.isEmpty()) {
            modifiedWords = SkyHanniMod.feature.storage.modifiedWords
        }

        val cachedResult = textCache.getOrNull(originalText)
        if (cachedResult != null) {
            return cachedResult
        }

        if (originalText.startsWith("§§")) {
            modifiedText = modifiedText.removePrefix("§§")
        } else {
            for (modifiedWord in modifiedWords) {
                if (!modifiedWord.enabled) continue
                val phrase = modifiedWord.phrase.convertToFormatted()

                if (phrase.isEmpty()) continue

                modifiedText = modifiedText.replace(
                    phrase,
                    modifiedWord.replacement.convertToFormatted(),
                    modifiedWord.isCaseSensitive()
                )
            }
        }

        textCache.put(originalText, modifiedText)
        return modifiedText
    }
}
