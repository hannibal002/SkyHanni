package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.TimeAndSizeLimitedCache
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils.requiredStyleChangeString
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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
    val componentCache = TimeAndSizeLimitedCache<Component, Component>(65565, 5.minutes)

    /** Replacements added manually by the user via /shwords. */
    var userModifiedWords = mutableListOf<VisualWordText>()

    /** Replacements added automatically by the mod for features, april fools, etc. */
    private val modModifiedWords = mutableListOf<VisualWordText>()
    private var finalWordsList = emptyList<VisualWordText>()

    fun update() {
        finalWordsList = modModifiedWords + userModifiedWords
        textCache.clear()
        componentCache.clear()
        SkyHanniMod.visualWordsData.modifiedWords =
            userModifiedWords.map { it.toVisualWord() }.toMutableList()
        Minecraft.getInstance().gui.chat.refreshTrimmedMessages()
    }

    var changeWords = true

    private fun modifyVisualWordsEnabled(): Boolean {
        if (!config.enabled || !changeWords) return false
        if (userModifiedWords.isEmpty() && SkyHanniMod.visualWordsData.modifiedWords.isNotEmpty()) {
            userModifiedWords.addAll(SkyHanniMod.visualWordsData.modifiedWords.map { VisualWordText.fromVisualWord(it) })
            update()
        }
        return userModifiedWords.isNotEmpty()
    }

    private fun visitAndReplace(visitable: FormattedText): Component {
        val rawCharacters = mutableListOf<StyledCharacter>()
        visitable.visit(
            { style, string ->
                rawCharacters.addAll(string.toStyledCharacterList(style, false))
                Optional.empty<Boolean>()
            },
            Style.EMPTY,
        )
        return Component.empty().also { result ->
            doReplacements(rawCharacters).toStyleRuns().forEach { (text, style) ->
                result.append(text.asComponent().withStyle(style))
            }
        }
    }

    fun transformText(orderedText: FormattedCharSequence?): FormattedCharSequence? {
        if (orderedText == null) return null
        if (!modifyVisualWordsEnabled()) return null

        return textCache.getOrPut(orderedText) {
            val rawCharacters = mutableListOf<StyledCharacter>()
            var canReplace = true

            orderedText.accept { index, style, codePoint ->
                if (codePoint == -1) {
                    canReplace = false
                    return@accept true
                }
                rawCharacters.add(StyledCharacter(codePoint, style, index == 0))
                true
            }

            val characters = if (canReplace) doReplacements(rawCharacters) else rawCharacters
            val compositeCharacters = characters.toStyleRuns().map { (text, style) ->
                FormattedCharSequence.forward(text, style)
            }
            FormattedCharSequence.composite(compositeCharacters)
        }
    }

    fun transformFormattedText(formattedText: FormattedText?): FormattedText? {
        if (formattedText == null) return null
        if (formattedText is Component) return transformComponent(formattedText)
        if (!modifyVisualWordsEnabled()) return null
        return visitAndReplace(formattedText)
    }

    fun transformComponent(component: Component?): Component? {
        if (component == null) return null
        if (!modifyVisualWordsEnabled()) return null
        return componentCache.getOrPut(component) { visitAndReplace(component) }
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

    private fun stylesAreOverlapping(testStyle: Style, testedStyle: Style) =
        (testStyle.color == testedStyle.color || testStyle.color == null) &&
            !(testStyle.isBold && !testedStyle.isBold) &&
            !(testStyle.isItalic && !testedStyle.isItalic) &&
            !(testStyle.isObfuscated && !testedStyle.isObfuscated) &&
            !(testStyle.isUnderlined && !testedStyle.isUnderlined) &&
            !(testStyle.isStrikethrough && !testedStyle.isStrikethrough)
}

private fun List<StyledCharacter>.toStyleRuns(): List<Pair<String, Style>> {
    val runs = mutableListOf<Pair<String, Style>>()
    var lastStyle = Style.EMPTY
    val str = buildString {
        for (character in this@toStyleRuns) {
            if (character.style != lastStyle) {
                if (isNotEmpty()) runs.add(toString() to lastStyle)
                lastStyle = character.style
                clear()
            }
            appendCodePoint(character.codePoint)
        }
    }
    if (str.isNotEmpty()) runs.add(str to lastStyle)
    return runs
}

data class StyledCharacter(
    val codePoint: Int,
    val style: Style,
    val first: Boolean = false,
) {

    fun withParentStyle(parentStyle: Style) =
        StyledCharacter(codePoint, style.applyTo(parentStyle), first)
}

data class VisualWordText(
    val from: List<StyledCharacter>,
    val to: List<StyledCharacter>,
    val enabled: Boolean,
    val caseSensitive: Boolean,
) {

    fun toVisualWord() = VisualWord(
        from.toLegacyString().replace("§", "&&"),
        to.toLegacyString().replace("§", "&&"),
        enabled,
        caseSensitive,
    )

    companion object {

        fun fromVisualWord(visualWord: VisualWord) = VisualWordText(
            visualWord.phrase.replace("&&", "§").toStyledCharacterList(),
            visualWord.replacement.replace("&&", "§").toStyledCharacterList(),
            visualWord.enabled,
            visualWord.isCaseSensitive(),
        )
    }
}

private fun List<StyledCharacter>.toLegacyString(): String = buildString {
    var lastStyle = Style.EMPTY
    for (character in this@toLegacyString) {
        if (lastStyle != character.style) {
            append(requiredStyleChangeString(lastStyle, character.style, true))
            lastStyle = character.style
        }
        appendCodePoint(character.codePoint)
    }
}

private fun String.toStyledCharacterList(
    style: Style = Style.EMPTY,
    hasFirst: Boolean = true,
): List<StyledCharacter> = buildList {
    StringDecomposer.iterateFormatted(this@toStyledCharacterList, style) { index, iterStyle, codePoint ->
        add(StyledCharacter(codePoint, iterStyle, index == 0 && hasFirst))
    }
}
