package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.mixins.transformers.AccessorChatComponentText
import at.hannibal2.skyhanni.utils.GuiRenderUtils.darkenColor
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiUtilRenderComponents
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import java.util.Base64
import java.util.UUID
import java.util.function.Predicate
import java.util.regex.Matcher
import java.util.regex.Pattern

object StringUtils {
    private val whiteSpaceResetPattern = "^(?:\\s|§r)*|(?:\\s|§r)*$".toPattern()
    private val whiteSpacePattern = "^\\s*|\\s*$".toPattern()
    private val resetPattern = "(?i)§R".toPattern()
    private val sFormattingPattern = "(?i)§S".toPattern()
    private val stringColourPattern = "§[0123456789abcdef].*".toPattern()

    fun String.trimWhiteSpaceAndResets(): String = whiteSpaceResetPattern.matcher(this).replaceAll("")
    fun String.trimWhiteSpace(): String = whiteSpacePattern.matcher(this).replaceAll("")
    fun String.removeResets(): String = resetPattern.matcher(this).replaceAll("")
    fun String.removeSFormattingCode(): String = sFormattingPattern.matcher(this).replaceAll("")

    fun String.firstLetterUppercase(): String {
        if (isEmpty()) return this

        val lowercase = lowercase()
        val first = lowercase[0].uppercase()
        return first + lowercase.substring(1)
    }

    private val formattingChars = "kmolnrKMOLNR".toSet()

    /**
     * Removes color and optionally formatting codes from the given string, leaving plain text.
     *
     * @param keepFormatting Boolean indicating whether to retain non-color formatting codes (default: false).
     * @return A string with color codes removed (and optionally formatting codes if specified).
     */
    fun CharSequence.removeColor(keepFormatting: Boolean = false): String {
        // Glossary:
        // Formatting indicator: The '§' character indicating the beginning of a formatting sequence
        // Formatting code: The character following a formatting indicator which specifies what color or text style this sequence corresponds to
        // Formatting sequence: The combination of a formatting indicator and code that changes the color or format of a string

        // Find the first formatting indicator
        var nextFormattingSequence = indexOf('§')

        // If this string does not contain any formatting indicators, just return this string directly
        if (nextFormattingSequence < 0) return this.toString()

        // Let's create a new string, and pre-allocate enough space to store this entire string
        val cleanedString = StringBuilder(this.length)

        // Read index stores the position in `this` which we have written up until now
        // a/k/a where we need to start reading from
        var readIndex = 0

        // As long as there still is a formatting indicator left in our string
        while (nextFormattingSequence >= 0) {

            // Write everything from the read index up to the next formatting indicator into our clean string
            cleanedString.append(this, readIndex, nextFormattingSequence)

            // If the next formatting sequence's code indicates a non-color format and we should keep those
            if (keepFormatting && nextFormattingSequence + 1 < length && this[nextFormattingSequence + 1] in formattingChars) {
                // Set the readIndex to the formatting indicator, so that the next loop will start writing from that paragraph symbol
                readIndex = nextFormattingSequence
                // Find the next § symbol after the formatting sequence
                nextFormattingSequence = indexOf('§', startIndex = readIndex + 1)
            } else {
                // If this formatting sequence should be skipped (either a color code, or !keepFormatting or an incomplete formatting sequence without a code)
                // Set the readIndex to after this formatting sequence, so that the next loop will skip over it before writing the string
                readIndex = nextFormattingSequence + 2
                // Find the next § symbol after the formatting sequence
                nextFormattingSequence = indexOf('§', startIndex = readIndex)

                // If the next read would be out of bound, reset the readIndex to the very end of the string, resulting in a "" string to be appended
                readIndex = readIndex.coerceAtMost(this.length)
            }
        }
        // Finally, after the last formatting sequence was processed, copy over the last sequence of the string
        cleanedString.append(this, readIndex, this.length)

        // And turn the string builder into a string
        return cleanedString.toString()
    }

    fun UUID.toDashlessUUID(): String = toString().replace("-", "")

    inline fun <T> Pattern.matchMatcher(text: String, consumer: Matcher.() -> T) =
        matcher(text).let { if (it.matches()) consumer(it) else null }

    inline fun <T> Pattern.findMatcher(text: String, consumer: Matcher.() -> T) =
        matcher(text).let { if (it.find()) consumer(it) else null }

    inline fun <T> List<String>.matchFirst(pattern: Pattern, consumer: Matcher.() -> T): T? {
        for (line in this) {
            pattern.matcher(line).let { if (it.matches()) return consumer(it) }
        }
        return null
    }

    inline fun <T> List<String>.matchAll(pattern: Pattern, consumer: Matcher.() -> T): T? {
        for (line in this) {
            pattern.matcher(line).let { if (it.find()) consumer(it) }
        }
        return null
    }

    private fun String.internalCleanPlayerName(): String {
        val split = trim().split(" ")
        return if (split.size > 1) {
            split[1].removeColor()
        } else {
            split[0].removeColor()
        }
    }

    fun String.cleanPlayerName(displayName: Boolean = false): String {
        return if (displayName) {
            if (SkyHanniMod.feature.chat.playerMessage.playerRankHider) {
                "§b" + internalCleanPlayerName()
            } else this
        } else {
            internalCleanPlayerName()
        }
    }

    inline fun <T> List<Pattern>.matchMatchers(text: String, consumer: Matcher.() -> T): T? {
        for (pattern in iterator()) {
            pattern.matchMatcher<T>(text) {
                return consumer()
            }
        }
        return null
    }

    fun getColor(string: String, default: Int, darker: Boolean = true): Int {
        val matcher = stringColourPattern.matcher(string)
        if (matcher.matches()) {
            val colorInt = Minecraft.getMinecraft().fontRendererObj.getColorCode(string[1])
            return if (darker) {
                colorInt.darkenColor()
            } else {
                "ff${Integer.toHexString(colorInt)}".toLong(radix = 16).toInt()
            }
        }
        return default
    }

    fun encodeBase64(input: String) = Base64.getEncoder().encodeToString(input.toByteArray())

    fun decodeBase64(input: String) = Base64.getDecoder().decode(input).decodeToString()

    fun addFormat(text: String, format: String): String {
        if (text.length < 2) return text

        val rawText = text.substring(2)
        return if (rawText == text.removeColor()) {
            val originalColor = text.substring(0, 2)
            "$originalColor$format$rawText"
        } else {
            "$format$text"
        }
    }

    fun String.removeWordsAtEnd(i: Int) = split(" ").dropLast(i).joinToString(" ")

    fun String.splitLines(width: Int): String {
        val fr = Minecraft.getMinecraft().fontRendererObj
        return GuiUtilRenderComponents.splitText(
            ChatComponentText(this), width, fr, false, false
        ).joinToString("\n") {
            val text = it.formattedText
            val formatCode = Regex("(?:§[a-f0-9l-or]|\\s)*")
            formatCode.matchAt(text, 0)?.let { matcher ->
                val codes = matcher.value.replace("\\s".toRegex(), "")
                codes + text.removeRange(matcher.range)
            } ?: text
        }
    }

    /**
     * Creates a comma-separated list using natural formatting (a, b, and c).
     * @param list - the list of strings to join into a string, containing 0 or more elements.
     * @param delimiterColor - the color code of the delimiter, inserted before each delimiter (commas and "and").
     * @return a string representing the list joined with the Oxford comma and the word "and".
     */
    fun List<String>.createCommaSeparatedList(delimiterColor: String = ""): String {
        if (this.isEmpty()) return ""
        if (this.size == 1) return this[0]
        if (this.size == 2) return "${this[0]}$delimiterColor and ${this[1]}"
        val lastIndex = this.size - 1
        val allButLast = this.subList(0, lastIndex).joinToString("$delimiterColor, ")
        return "$allButLast$delimiterColor, and ${this[lastIndex]}"
    }

    fun pluralize(number: Int, singular: String, plural: String? = null, withNumber: Boolean = false): String {
        val pluralForm = plural ?: "${singular}s"
        var str = if (number == 1) singular else pluralForm
        if (withNumber) str = "${number.addSeparators()} $str"
        return str
    }

    fun progressBar(percentage: Double, steps: Int = 24): Any {
        //'§5§o§2§l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §f§l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §r §e348,144.3§6/§e936k'
        val prefix = "§5§o§2"
        val step = "§l§m "
        val missing = "§f"
        val end = "§r"

        val builder = StringBuilder()
        var inMissingArea = false
        builder.append(prefix)
        for (i in 0..steps) {
            val toDouble = i.toDouble()
            val stepPercentage = toDouble / steps
            if (stepPercentage >= percentage && !inMissingArea) {
                builder.append(missing)
                inMissingArea = true
            }
            builder.append(step)
        }
        builder.append(end)
        return builder.toString()
    }

    fun String.capAtMinecraftLength(limit: Int) =
        capAtLength(limit) { Minecraft.getMinecraft().fontRendererObj.getCharWidth(it) }

    private fun String.capAtLength(limit: Int, lengthJudger: (Char) -> Int): String {
        var i = 0
        return takeWhile {
            i += lengthJudger(it)
            i < limit
        }
    }

    // recursively goes through the chat component until an action is completed
    fun modifyFirstChatComponent(chatComponent: IChatComponent, action: Predicate<IChatComponent>): Boolean {
        if (action.test(chatComponent)) {
            return true
        }
        for (sibling in chatComponent.siblings) {
            if (modifyFirstChatComponent(sibling, action)) {
                return true
            }
        }
        return false
    }

    // replaces a word without breaking any chat components
    fun replaceFirstChatText(chatComponent: IChatComponent, toReplace: String, replacement: String): IChatComponent {
        modifyFirstChatComponent(chatComponent) { component ->
            if (component is ChatComponentText) {
                component as AccessorChatComponentText
                val componentText = component.text_skyhanni()
                if (componentText.contains(toReplace)) {
                    component.setText_skyhanni(componentText.replace(toReplace, replacement))
                    return@modifyFirstChatComponent true
                }
                return@modifyFirstChatComponent false
            }
            return@modifyFirstChatComponent false
        }
        return chatComponent
    }

    fun String.getPlayerNameFromChatMessage(): String? = matchPlayerChatMessage(this)?.group("username")

    fun String.getPlayerNameAndRankFromChatMessage(): String? = matchPlayerChatMessage(this)?.group("rankedName")

    private fun matchPlayerChatMessage(string: String): Matcher? {
        var username = ""
        var matcher = UtilsPatterns.playerChatPattern.matcher(string)
        if (matcher.matches()) {
            username = matcher.group("important").removeResets()
        }
        if (username == "") return null

        if (username.contains("[NPC]")) {
            return null
        }

        if (username.contains(">")) {
            username = username.substring(username.indexOf('>') + 1).trim()
        }

        username = username.removePrefix("§dFrom ")
        username = username.removePrefix("§dTo ")

        matcher = UtilsPatterns.chatUsernamePattern.matcher(username)
        return if (matcher.matches()) matcher else null
    }

    fun String.convertToFormatted(): String = this.replace("&&", "§")

    fun Pattern.matches(string: String?) = string?.let { matcher(it).matches() } ?: false
    fun Pattern.anyMatches(list: List<String>?) = list?.any { this.matches(it) } ?: false

    fun Pattern.find(string: String?) = string?.let { matcher(it).find() } ?: false

    fun String.allLettersFirstUppercase() = split("_").joinToString(" ") { it.firstLetterUppercase() }

    fun String?.equalsIgnoreColor(string: String?) = this?.let { it.removeColor() == string?.removeColor() } ?: false

    fun String.isRoman(): Boolean = UtilsPatterns.isRomanPattern.matches(this)

    fun isEmpty(message: String): Boolean = message.removeColor().trimWhiteSpaceAndResets().isEmpty()

    fun generateRandomId() = UUID.randomUUID().toString()
}
