package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence

object VisualWordsHook {
    @JvmStatic
    @get:JvmName("isCaxtonLoaded")
    val caxtonLoaded by lazy { PlatformUtils.isModInstalled("caxton") }

    @JvmStatic
    fun modifyOrderedText(value: FormattedCharSequence): FormattedCharSequence =
        ModifyVisualWords.transformText(value) ?: value

    @JvmStatic
    fun modifyString(value: String): String {
        val replaced = ModifyVisualWords.transformText(OrderedTextUtils.legacyTextToOrderedText(value)) ?: return value
        return OrderedTextUtils.orderedTextToLegacyString(replaced)
    }

    @JvmStatic
    fun modifyFormattedText(value: FormattedText): FormattedText =
        ModifyVisualWords.transformFormattedText(value) ?: value

    @JvmStatic
    fun <T> withoutWordChanges(block: () -> T): T {
        ModifyVisualWords.changeWords = false
        try {
            return block()
        } finally {
            ModifyVisualWords.changeWords = true
        }
    }
}
