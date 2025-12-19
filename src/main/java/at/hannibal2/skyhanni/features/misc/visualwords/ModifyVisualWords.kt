package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.collection.TimeAndSizeLimitedCache
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils.requiredStyleChangeString
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.StringDecomposer
import java.util.Optional
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ModifyVisualWords {
    private val config get() = SkyHanniMod.feature.gui.modifyWords

    val textCache = TimeAndSizeLimitedCache<FormattedCharSequence, FormattedCharSequence>(131072, 5.minutes)
    val stringVisitableCache = TimeAndSizeLimitedCache<FormattedText, FormattedText>(65565, 5.minutes)

    // Replacements the user added manually via /shwords
    var userModifiedWords = mutableListOf<VisualWordText>()

    // Replacements the mod added automatically for some features, april jokes, etc.
    private val modModifiedWords = mutableListOf<VisualWordText>()
    private var finalWordsList = listOf<VisualWordText>()

    fun update() {
        finalWordsList = modModifiedWords + userModifiedWords
        textCache.clear()
        stringVisitableCache.clear()
        SkyHanniMod.visualWordsData.modifiedWords = userModifiedWords.map { visualWordText -> visualWordText.toVisualWord() }.toMutableList()
        Minecraft.getInstance().gui?.chat?.refreshTrimmedMessages()
    }

    var changeWords = true

    fun transformText(orderedText: FormattedCharSequence?): FormattedCharSequence? {
        if (orderedText == null) return null

        if (!config.enabled) return null
        if (!changeWords) return null

        if (userModifiedWords.isEmpty() && SkyHanniMod.visualWordsData.modifiedWords.isNotEmpty()) {
            userModifiedWords.addAll(SkyHanniMod.visualWordsData.modifiedWords.map { VisualWordText.fromVisualWord(it) })
            update()
        }

        if (userModifiedWords.isEmpty()) return null

        return textCache.getOrPut(orderedText) {

            var characters = mutableListOf<StyledCharacter>()
            var replace = true

            orderedText.accept { index, style, codePoint ->
                if (codePoint == -1) {
                    replace = false
                    return@accept true
                }
                characters.add(StyledCharacter(codePoint, style, index == 0))
                true
            }

            if (replace) characters = doReplacements(characters)

            val outputTexts = mutableListOf<FormattedCharSequence>()
            var lastStyle: Style? = null
            val textStringBuilder = StringBuilder()

            for (character in characters) {
                if (character.style != lastStyle) {
                    if (textStringBuilder.isNotEmpty())
                        outputTexts.add(FormattedCharSequence.forward(textStringBuilder.toString(), lastStyle))

                    lastStyle = character.style

                    textStringBuilder.clear()
                }
                textStringBuilder.appendCodePoint(character.codePoint)
            }

            if (textStringBuilder.isNotEmpty()) {
                outputTexts.add(FormattedCharSequence.forward(textStringBuilder.toString(), lastStyle))
            }

            FormattedCharSequence.composite(outputTexts)
        }
    }

    fun transformStringVisitable(stringVisitable: FormattedText?) : FormattedText? {
        if (stringVisitable == null) return null

        if (!config.enabled) return null
        if (!changeWords) return null

        if (userModifiedWords.isEmpty() && SkyHanniMod.visualWordsData.modifiedWords.isNotEmpty()) {
            userModifiedWords.addAll(SkyHanniMod.visualWordsData.modifiedWords.map { VisualWordText.fromVisualWord(it) })
            update()
        }

        if (userModifiedWords.isEmpty()) return null

        return stringVisitableCache.getOrPut(stringVisitable) {
            var characters = mutableListOf<StyledCharacter>()
            stringVisitable.visit(
                { style, string ->
                    characters.addAll(string.toStyledCharacterList(style, false))
                    Optional.empty<Boolean>()
                },
                Style.EMPTY
            )

            characters = doReplacements(characters)

            val outputParts = mutableListOf<FormattedText>()

            var lastStyle = Style.EMPTY
            val stringBuilder = StringBuilder()

            for (character in characters) {
                if (lastStyle != character.style) {

                    outputParts.add(FormattedText.of(stringBuilder.toString(), lastStyle))
                    lastStyle = character.style
                    stringBuilder.clear()
                }

                stringBuilder.appendCodePoint(character.codePoint)
            }

            if (stringBuilder.isNotEmpty()) {
                outputParts.add(FormattedText.of(stringBuilder.toString(), lastStyle))
            }

            FormattedText.composite(outputParts)
        }
    }

    private fun doReplacements(characters: MutableList<StyledCharacter>): MutableList<StyledCharacter> {

        var workingCharacters = characters

        for (word in finalWordsList) {
            if (!word.enabled) continue

            val subResultList = mutableListOf<StyledCharacter>()

            val first = word.from.firstOrNull() ?: continue

            var index = 0
            while (index < workingCharacters.size) {
                var replaced = false
                if (
                    index <= workingCharacters.size - word.from.size &&
                    workingCharacters[index].codePoint == first.codePoint &&
                    stylesAreOverlapping(first.style, workingCharacters[index].style)
                ) {
                    var subIndex = 1
                    while (subIndex < word.from.size) {

                        val char = word.from[subIndex]
                        val styledCharacter = workingCharacters[index + subIndex]

                        if (
                            char.codePoint != styledCharacter.codePoint ||
                            !stylesAreOverlapping(char.style, styledCharacter.style)
                        ) break

                        subIndex++
                    }

                    if (subIndex == word.from.size) {
                        subResultList.addAll(word.to.map { it.withParentStyle(workingCharacters[index].style) })
                        index += subIndex
                        replaced = true
                    }
                }

                if (!replaced) {
                    subResultList.add(workingCharacters[index])
                    index += 1
                }
            }

            workingCharacters = subResultList
        }

        return workingCharacters
    }

    private fun stylesAreOverlapping(testStyle: Style, testedStyle: Style) = (testStyle.color == testedStyle.color || testStyle.color == null) &&
            !(testStyle.isBold && !testedStyle.isBold) &&
            !(testStyle.isItalic && !testedStyle.isItalic) &&
            !(testStyle.isObfuscated && !testedStyle.isObfuscated) &&
            !(testStyle.isUnderlined && !testedStyle.isUnderlined) &&
            !(testStyle.isStrikethrough && !testedStyle.isStrikethrough)
}

data class StyledCharacter(
    val codePoint: Int,
    val style: Style,
    val first: Boolean = false
) {

    fun withParentStyle(parentStyle: Style) = StyledCharacter(codePoint, style.applyTo(parentStyle), first)
}

data class VisualWordText(
    val from: List<StyledCharacter>,
    val to: List<StyledCharacter>,
    val enabled: Boolean,
    val caseSensitive: Boolean
) {

    fun toVisualWord() = VisualWord(
        from.toLegacyString().replace("§", "&&"),
        to.toLegacyString().replace("§", "&&"),
        enabled,
        caseSensitive
    )

    companion object {

        fun fromVisualWord(visualWord: VisualWord) = VisualWordText(
            visualWord.phrase.replace("&&", "§").toStyledCharacterList(),
            visualWord.replacement.replace("&&", "§").toStyledCharacterList(),
            visualWord.enabled,
            visualWord.isCaseSensitive()
        )
    }
}

private fun List<StyledCharacter>.toLegacyString(): String {
    val builder = StringBuilder()
    var lastStyle = Style.EMPTY
    for (character in this) {
        if (lastStyle != character.style) {
            builder.append(requiredStyleChangeString(lastStyle, character.style, true))
            lastStyle = character.style
        }
        builder.appendCodePoint(character.codePoint)
    }
    return builder.toString()
}

private fun String.toStyledCharacterList(style: Style = Style.EMPTY, hasFirst: Boolean = true): List<StyledCharacter> {
    val newList = mutableListOf<StyledCharacter>()

    StringDecomposer.iterateFormatted(this, style) {  index: Int, style: Style, codePoint: Int ->
        newList.add(StyledCharacter(codePoint, style, index == 0 && hasFirst))
        true
    }

    return newList
}
