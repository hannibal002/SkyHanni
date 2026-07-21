package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object TextUtils {
    /**
     * Creates a comma-separated list using natural formatting (a, b, and c).
     * this = the list of strings to join into a string, containing 0 or more elements.
     * @param delimiterColor - the ChatFormatting of the delimiter, applied to each delimiter (commas and "and").
     * @param originalEntryColor - the ChatFormatting of the string entries.
     * @return a string representing the list joined with the Oxford comma and the word "and".
     */
    fun List<String>.createCommaSeparatedList(
        originalEntryColor: ChatFormatting = ChatFormatting.GRAY,
        delimiterColor: ChatFormatting = ChatFormatting.GRAY,
    ): Component {
        if (isEmpty()) return Component.empty()
        if (size == 1) return this[0].asComponent()
        val startingList = this
        if (size == 2) {
            return componentBuilder {
                appendWithColor(startingList[0], originalEntryColor)
                appendWithColor(" and ", delimiterColor)
                appendWithColor(startingList[1], originalEntryColor)
            }
        }
        val lastNormalIndex = size - 2
        var currIndex = 0
        return componentBuilder {
            for (entry in startingList) {
                if (currIndex != lastNormalIndex) {
                    appendWithColor(entry, originalEntryColor)
                    appendWithColor(", ", delimiterColor)
                } else {
                    if (currIndex == size - 2) {
                        appendWithColor(entry, originalEntryColor)
                        appendWithColor(", and", delimiterColor)
                    }
                    if (currIndex == size - 1) {
                        appendWithColor(entry, originalEntryColor)
                    }
                }
                currIndex++
            }
        }
    }
}
