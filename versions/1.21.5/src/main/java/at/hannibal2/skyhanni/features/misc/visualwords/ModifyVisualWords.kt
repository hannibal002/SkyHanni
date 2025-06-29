package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils.requiredStyleChangeString
import net.minecraft.client.MinecraftClient
import net.minecraft.text.OrderedText
import net.minecraft.text.StringVisitable
import net.minecraft.text.Style
import net.minecraft.text.TextVisitFactory
import java.util.Optional
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ModifyVisualWords {
    private val config get() = SkyHanniMod.feature.gui.modifyWords

    val textCache = TimeLimitedCache<OrderedText, OrderedText>(5.minutes)
    val stringVisitableCache = TimeLimitedCache<StringVisitable, StringVisitable>(5.minutes)

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
        MinecraftClient.getInstance().inGameHud.chatHud.refresh()
    }

    var changeWords = true

    fun transformText(orderedText: OrderedText?): OrderedText? {
        if (orderedText == null) return null

        if (!config.enabled) return orderedText
        if (!changeWords) return orderedText

        return textCache.getOrPut(orderedText) {

            var characters = mutableListOf<StyledCharacter>()
            var hasCachedCharacters = false
            var lastDiff = 1
            var replace = true

            OrderedText { visitor ->
                if (!hasCachedCharacters) {

                    var lastIndex = 0
                    orderedText.accept { index, style, codePoint ->
                        if (codePoint == -1) {
                            replace = false
                            return@accept true
                        }
                        lastDiff = index - lastIndex
                        lastIndex = index
                        characters.add(StyledCharacter(codePoint, style, index == 0))
                        true
                    }

                    if (replace) characters = doReplacements(characters)

                    hasCachedCharacters = true
                }

                var index = 0
                for (character in characters) {
                    if (!visitor.accept(index, character.style, character.codePoint)) {
                        return@OrderedText false
                    }
                    index += lastDiff
                }
                true
            }
        }
    }

    fun transformStringVisitable(stringVisitable: StringVisitable?) : StringVisitable? {
        if (stringVisitable == null) return null

        if (!config.enabled) return stringVisitable
        if (!changeWords) return stringVisitable

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

            val outputParts = mutableListOf<StringVisitable>()

            var lastStyle = Style.EMPTY
            val stringBuilder = StringBuilder()

            for (character in characters) {
                if (lastStyle != character.style) {

                    outputParts.add(StringVisitable.styled(stringBuilder.toString(), lastStyle))
                    lastStyle = character.style
                    stringBuilder.clear()
                }

                stringBuilder.appendCodePoint(character.codePoint)
            }

            if (stringBuilder.isNotEmpty()) {
                outputParts.add(StringVisitable.styled(stringBuilder.toString(), lastStyle))
            }

            StringVisitable.concat(outputParts)
        }
    }

    private fun doReplacements(characters: MutableList<StyledCharacter>): MutableList<StyledCharacter> {

        if (userModifiedWords.isEmpty() && SkyHanniMod.visualWordsData.modifiedWords.isNotEmpty()) {
            userModifiedWords.addAll(SkyHanniMod.visualWordsData.modifiedWords.map { VisualWordText.fromVisualWord(it) })
            update()
        }

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
                    first.style.withParent(workingCharacters[index].style) == workingCharacters[index].style
                ) {
                    var subIndex = 1
                    while (subIndex < word.from.size) {

                        val char = word.from[subIndex]
                        val styledCharacter = workingCharacters[index + subIndex]

                        if (
                            char.codePoint != styledCharacter.codePoint ||
                            char.style.withParent(styledCharacter.style) != styledCharacter.style
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
}

data class StyledCharacter(
    val codePoint: Int,
    val style: Style,
    val first: Boolean = false
) {

    fun withParentStyle(parentStyle: Style) = StyledCharacter(codePoint, style.withParent(parentStyle), first)
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

fun List<StyledCharacter>.toLegacyString(): String {
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

fun String.toStyledCharacterList(style: Style = Style.EMPTY, hasFirst: Boolean = true): List<StyledCharacter> {
    val newList = mutableListOf<StyledCharacter>()

    TextVisitFactory.visitFormatted(this, style) {  index: Int, style: Style, codePoint: Int ->
        newList.add(StyledCharacter(codePoint, style, index == 0 && hasFirst))
        true
    }

    return newList
}
