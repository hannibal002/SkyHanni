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

        if (modifiedWords.isEmpty()) {
            modifiedWords = SkyHanniMod.feature.storage.modifiedWords
        }

        val cachedResult = textCache.getIfPresent(originalText)
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

                modifiedText = modifiedText.replace(phrase, modifiedWord.replacement.convertToFormatted())
            }
        }

        textCache.put(originalText, modifiedText)
        return modifiedText
    }
}