package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.convertToFormatted
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

object ModifyVisualWords {
    private val config get() = SkyHanniMod.feature.gui.modifyWords
    var textCache: Cache<String, String> = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build()

    var modifiedWords = mutableListOf<VisualWord>()

    fun modifyText(originalText: String?): String? {
        var modifiedText = originalText ?: return null
        if (!LorenzUtils.onHypixel) return originalText
        if (!config.enabled) return originalText
        if (!LorenzUtils.inSkyBlock && !config.workOutside) return originalText
        if (VisualWordGui.isInGui()) return originalText

        if (modifiedWords.isEmpty()) {
            modifiedWords = SkyHanniMod.feature.storage.modifiedWords
        }

        val cachedResult = textCache.getIfPresent(originalText)
        if (cachedResult != null) {
            return cachedResult
        }

        var replacements = 0

        for (modifiedWord in modifiedWords) {
            if (!modifiedWord.enabled) continue
            val phrase = modifiedWord.phrase.convertToFormatted()

            if (phrase.isEmpty()) continue

            replacements += 1
            modifiedText = modifiedText.replace(phrase, modifiedWord.replacement.convertToFormatted())
        }
        // if not many are done it is better to not cache it
        if (replacements > 2) textCache.put(originalText, modifiedText)
        return modifiedText
    }
}