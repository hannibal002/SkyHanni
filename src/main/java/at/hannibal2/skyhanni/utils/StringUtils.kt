package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.findAll
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.defaultStyleConstructor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.toChatFormatting
import at.hannibal2.skyhanni.utils.compat.unformattedTextForChatCompat
import at.hannibal2.skyhanni.utils.compat.value
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ComponentRenderUtils
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.util.Base64
import java.util.Locale
import java.util.NavigableMap
import java.util.NavigableSet
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

@Suppress("TooManyFunctions", "MemberVisibilityCanBePrivate")
object StringUtils {
    private val whiteSpaceResetPattern = "^(?:\\s|§r)*|(?:\\s|§r)*$".toPattern()
    private val whiteSpacePattern = "^\\s*|\\s*$".toPattern()
    private val resetPattern = "(?i)§R".toPattern()
    private val sFormattingPattern = "(?i)§S".toPattern()
    private val asciiWithColorCodePattern = "[^\\x00-\\x7F§]".toPattern()
    private val minecraftColorCodesPattern = "(?i)(§[0-9a-fklmnor])+".toPattern()
    private val lettersAndNumbersPattern = "(§.)|[^a-zA-Z0-9 ]".toPattern()
    fun String.removeAllNonLettersAndNumbers(): String = lettersAndNumbersPattern.matcher(this).replaceAll("")
    fun String.cleanString(): String = removeAllNonLettersAndNumbers().trimWhiteSpaceAndResets().lowercase()

    fun String.trimWhiteSpaceAndResets(): String = whiteSpaceResetPattern.matcher(this).replaceAll("")
    fun String.trimWhiteSpace(): String = whiteSpacePattern.matcher(this).replaceAll("")
    fun String.removeResets(): String = resetPattern.matcher(this).replaceAll("")
    fun String.removeSFormattingCode(): String = sFormattingPattern.matcher(this).replaceAll("")
    fun String.removeNonAsciiNonColorCode(): String = asciiWithColorCodePattern.matcher(this).replaceAll("")

    fun String.firstLetterUppercase(): String {
        return this.lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private val formattingChars = "kmolnrKMOLNR".toSet()
    private val colorChars = "abcdefABCDEF0123456789zZ".toSet()

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

        // Flag for whether there is a text style (non-color and non-reset formatting code) currently being applied
        var isFormatted = false

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

            // Get the formatting code (note: this may not be a valid formatting code)
            val formattingCode = this.getOrNull(nextFormattingSequence + 1)

            // If the next formatting sequence's code indicates a non-color format and we should keep those
            if (keepFormatting && formattingCode in formattingChars) {
                // Update formatted flag based on whether this is a reset or a style format code
                isFormatted = formattingCode?.lowercaseChar() != 'r'

                // Set the readIndex to the formatting indicator, so that the next loop will start writing from that paragraph symbol
                readIndex = nextFormattingSequence
                // Find the next § symbol after the formatting sequence
                nextFormattingSequence = indexOf('§', startIndex = readIndex + 1)
            } else {
                // If this formatting sequence should be skipped (either a color code, or !keepFormatting or an incomplete formatting sequence without a code)

                // If being formatted and color code encountered, reset the current formatting code
                if (isFormatted && formattingCode in colorChars) {
                    cleanedString.append("§r")
                    isFormatted = false
                }

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

    /**
     * From https://stackoverflow.com/questions/10711494/get-values-in-treemap-whose-string-keys-start-with-a-pattern
     */
    fun <T> subMapOfStringsStartingWith(prefix: String, map: NavigableMap<String, T>): NavigableMap<String, T> {
        if ("" == prefix) return map
        val lastKey = nextLexicographicallyStringWithSameLength(prefix)
        return map.subMap(prefix, true, lastKey, false)
    }

    fun subMapOfStringsStartingWith(prefix: String, map: NavigableSet<String>): NavigableSet<String> {
        if ("" == prefix) return map
        val lastKey = nextLexicographicallyStringWithSameLength(prefix)
        return map.subSet(prefix, true, lastKey, false)
    }

    fun nextLexicographicallyStringWithSameLength(input: String): String {
        val lastCharPosition = input.length - 1
        val inputWithoutLastChar = input.substring(0, lastCharPosition)
        val lastChar = input[lastCharPosition]
        val incrementedLastChar = (lastChar.code + 1).toChar()
        return inputWithoutLastChar + incrementedLastChar
    }

    fun UUID.toUnDashedUUID(): String = toString().replace("-", "")

    @Throws(IllegalArgumentException::class)
    fun parseUUID(uuidString: String): UUID = if (uuidString.isValidUuid()) UUID.fromString(uuidString)
    else {
        require(uuidString.length == 32) { "UUID string must either have dashes, or be 32 characters long" }
        val builder = StringBuilder(uuidString)
        builder.insert(8, '-').insert(13, '-').insert(18, '-').insert(23, '-')
        UUID.fromString(builder.toString())
    }

    fun parseUUIDOrNull(uuidString: String): UUID? = runCatching { parseUUID(uuidString) }.getOrNull()

    private fun String.internalCleanPlayerName(): String {
        val split = trim().split(" ")
        return if (split.size > 1) {
            split[1].removeColor()
        } else {
            split[0].removeColor()
        }.removeSuffix("'s")
    }

    fun String.cleanPlayerName(displayName: Boolean = false): String {
        return if (displayName) {
            if (SkyHanniMod.feature.chat.playerMessage.playerRankHider) {
                // TODO custom color
                "§b" + internalCleanPlayerName()
            } else this

        } else {
            internalCleanPlayerName()
        }
    }

    fun String.isPlayerName() = UtilsPatterns.playerNamePattern.matches(this)

    fun String.substringBeforeLastOrNull(needle: String): String? {
        val index = this.lastIndexOf(needle)
        if (index < 0) return null
        return this.substring(0, index)
    }

    fun encodeBase64(input: String): String = Base64.getEncoder().encodeToString(input.toByteArray())

    fun decodeBase64(input: String) = Base64.getDecoder().decode(input).decodeToString()

    fun String.removeWordsAtEnd(i: Int) = split(" ").dropLast(i).joinToString(" ")
    fun Double.removeUnusedDecimal() = if (this % 1 == 0.0) this.toInt().toString() else this.toString()

    fun String.splitLines(width: Int): String = splitText(
        this,
        width,
    ).joinToString("\n") { it.removePrefix("§r") }

    private fun splitText(text: String, width: Int): List<String> {
        val lines = ComponentRenderUtils.wrapComponents(Component.literal(text), width, Minecraft.getInstance().font)
        val strings: MutableList<String> = ArrayList(lines.size)
        for (line in lines) {
            var newLine = ""
            var lastColor: TextColor? = null
            var lastFormatting = ""
            line.accept { index, style, codePoint ->
                val color = style.color
                if (color != lastColor) {
                    lastColor = color
                    lastFormatting = ""
                    if (color != null) {
                        newLine += color.toChatFormatting()
                    }
                }
                var newFormatting = ""
                if (style.isBold) newFormatting = "§l"
                else if (style.isItalic) newFormatting = "§o"
                else if (style.isUnderlined) newFormatting = "§n"
                else if (style.isStrikethrough) newFormatting = "§m"
                else if (style.isObfuscated) newFormatting = "§k"
                else newFormatting = ""

                if (newFormatting != lastFormatting) {
                    lastFormatting = newFormatting
                    newLine += newFormatting
                }
                newLine += codePoint.toChar()
                true
            }
            strings.add(newLine)
        }
        return strings
    }

    /**
     * Creates a comma-separated list using natural formatting (a, b, and c).
     * this = the list of strings to join into a string, containing 0 or more elements.
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

    fun String.pluralize(number: Int) = pluralize(number, this)

    fun pluralize(number: Int, singular: String, plural: String? = null, withNumber: Boolean = false): String {
        val pluralForm = plural ?: "${singular}s"
        var str = if (number == 1 || number == -1) singular else pluralForm
        if (withNumber) str = "${number.addSeparators()} $str"
        return str
    }

    fun progressBar(percentage: Double, steps: Int = 24): Any {
        // '§5§o§2§l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §f§l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §r §e348,144.3§6/§e936k'
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

    fun String.capAtMinecraftLength(limit: Int) = capAtLength(limit) {
        Minecraft.getInstance().font.width(it.toString())
    }

    private fun String.capAtLength(limit: Int, lengthJudger: (Char) -> Int): String {
        var i = 0
        return takeWhile {
            i += lengthJudger(it)
            i < limit
        }
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
    fun String.convertToUnformatted(): String = this.replace("§", "&")

    fun String.allLettersFirstUppercase() = split("_").joinToString(" ") { it.firstLetterUppercase() }

    fun String?.equalsIgnoreColor(string: String?) = this?.let { it.removeColor() == string?.removeColor() } ?: false

    fun String.isRoman(): Boolean = UtilsPatterns.isRomanPattern.matches(this)

    fun isEmpty(message: String): Boolean = message.removeColor().trimWhiteSpaceAndResets().isEmpty()

    fun generateRandomId() = UUID.randomUUID().toString()

    fun String.insert(pos: Int, chars: CharSequence): String = this.substring(0, pos) + chars + this.substring(pos)

    fun String.insert(pos: Int, char: Char): String = this.substring(0, pos) + char + this.substring(pos)

    fun replaceIfNeeded(
        original: Component,
        newText: String,
    ): Component? {
        return replaceIfNeeded(original, newText.asComponent())
    }

    private val colorMap = ChatFormatting.entries.associateBy { it.toString()[1] }
    fun enumChatFormattingByCode(char: Char): ChatFormatting? {
        return colorMap[char]
    }

    fun doLookTheSame(left: Component, right: Component): Boolean {
        class ChatIterator(var component: Component) {
            var queue = mutableListOf<Component>()
            var idx = 0
            var colorOverride = defaultStyleConstructor
            fun next(): Pair<Char, Style>? {
                while (true) {
                    while (idx >= component.unformattedTextForChatCompat().length) {
                        queue.addAll(0, component.siblings)
                        colorOverride = defaultStyleConstructor
                        component = queue.removeFirstOrNull() ?: return null
                    }
                    val char = component.unformattedTextForChatCompat()[idx++]
                    if (char == '§' && idx < component.unformattedTextForChatCompat().length) {
                        val formattingChar = component.unformattedTextForChatCompat()[idx++]
                        val formatting = enumChatFormattingByCode(formattingChar) ?: continue
                        when (formatting) {
                            ChatFormatting.OBFUSCATED -> {
                                colorOverride.withObfuscated(true)
                            }

                            ChatFormatting.BOLD -> {
                                colorOverride.withBold(true)
                            }

                            ChatFormatting.STRIKETHROUGH -> {
                                colorOverride.withStrikethrough(true)
                            }

                            ChatFormatting.UNDERLINE -> {
                                colorOverride.withUnderlined(true)
                            }

                            ChatFormatting.ITALIC -> {
                                colorOverride.withItalic(true)
                            }

                            else -> {
                                colorOverride = defaultStyleConstructor.withColor(formatting)
                            }
                        }
                    } else {
                        return Pair(char, colorOverride.applyTo(component.style))
                    }
                }
            }
        }

        val leftIt = ChatIterator(left)
        val rightIt = ChatIterator(right)
        while (true) {
            val leftChar = leftIt.next()
            val rightChar = rightIt.next()
            if (leftChar == null && rightChar == null) return true
            if (leftChar != rightChar) return false
        }
    }

    fun <T : Component> replaceIfNeeded(
        original: T,
        newText: T,
    ): T? {
        if (doLookTheSame(original, newText)) return null
        return newText
    }

    private fun addComponent(foundCommands: MutableList<Component>, message: Component) {
        val clickEvent = message.command
        if (clickEvent != null) {
            if (foundCommands.size == 1 && foundCommands[0].command == clickEvent) {
                return
            }
            foundCommands.add(message)
        }
    }

    /**
     * Applies a transformation on the message of a SystemMessageEvent if possible.
     */
    fun SystemMessageEvent.applyIfPossible(
        transformationReason: String? = null,
        transform: (String) -> String,
    ) {
        val original = chatComponent.formattedTextCompat()
        val new = transform(original)
        if (new == original) return

        val clickEvents = mutableListOf<ClickEvent>()
        val hoverEvents = mutableListOf<HoverEvent>()
        chatComponent.findAllEvents(clickEvents, hoverEvents)

        if (clickEvents.size > 1 || hoverEvents.size > 1) return

        val newComponent = new.asComponent().apply {
            if (clickEvents.size == 1) command = clickEvents.first().value()
            if (hoverEvents.size == 1) hover = hoverEvents.first().value()
        }

        replaceComponent(newComponent, transformationReason.orEmpty())
    }

    private fun Component.findAllEvents(
        clickEvents: MutableList<ClickEvent>,
        hoverEvents: MutableList<HoverEvent>,
    ) {
        siblings.forEach { it.findAllEvents(clickEvents, hoverEvents) }

        val clickEvent = style.clickEvent
        val hoverEvent = style.hoverEvent

        if (clickEvent?.action() != null && clickEvents.none { it.value() == clickEvent.value() }) {
            clickEvents.add(clickEvent)
        }

        if (hoverEvent?.action() != null && hoverEvents.none {
                it.value() == hoverEvent.value()
            }
        ) {
            hoverEvents.add(hoverEvent)
        }
    }

    fun String.replaceAll(oldValue: String, newValue: String, ignoreCase: Boolean = false): String {
        var text = this
        while (true) {
            val newText = text.replace(oldValue, newValue, ignoreCase = ignoreCase)
            if (newText == text) {
                return text
            }
            text = newText
        }
    }

    /**
     * Removes starting and ending reset formattings that don't sever a benefit at all.
     */
    fun String.stripHypixelMessage(): String {
        var message = this

        while (message.startsWith("§r")) {
            message = message.substring(2)
        }
        while (message.endsWith("§r")) {
            message = message.substring(0, message.length - 2)
        }
        return message
    }

    fun String.applyFormattingFrom(original: ComponentSpan): Component {
        return asComponent { style = original.sampleStyleAtStart() }
    }

    fun String.applyFormattingFrom(original: Component): Component {
        return asComponent { style = original.style }
    }

    fun Component.contains(string: String): Boolean = formattedTextCompat().contains(string)

    fun String.width(): Int {
        return Minecraft.getInstance().font.width(this)
    }

    private val vowels = "aeiouAEIOU".toSet()

    fun Char.isVowel(): Boolean = this in vowels

    fun String.lastColorCode(): String? = minecraftColorCodesPattern.findAll(this).lastOrNull()

    fun String.splitCamelCase() = this.replace("([a-z])([A-Z])".toRegex(), "$1 $2")

    fun String.isValidUuid(): Boolean {
        return try {
            UUID.fromString(this)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun optionalAn(string: String): String {
        if (string.isEmpty()) return ""
        return if (string[0] in "aeiou") "an" else "a"
    }

    fun String.hasWhitespace(): Boolean = any { it.isWhitespace() }

    fun String.splitLastWhitespace(): Pair<String, String> {
        val lastWhitespaceIndex = lastIndexOf(" ")
        return if (lastWhitespaceIndex == -1) {
            "" to this
        } else {
            substring(0, lastWhitespaceIndex) to substring(lastWhitespaceIndex + 1)
        }
    }

    fun String.addStrikethorugh(strikethorugh: Boolean = true): String {
        if (!strikethorugh) return this

        val firstColor = getFirstColorCode()
        val clean = removeColor()
        return "§$firstColor§m$clean"
    }

    fun getListOfStringsMatchingLastWord(words: Array<String>, args: Collection<String>): List<String> {
        val lastWord = words.lastOrNull() ?: return emptyList()
        val matches = args.filter { it.startsWith(lastWord, ignoreCase = true) }
        return matches
    }

    // Just fully yoinked this one from the font renderer thx dinner bone
    fun getFormatFromString(text: String): String {
        val length = text.length
        var string = ""
        var i = -1

        while ((text.indexOf(167.toChar(), i + 1).also { i = it }) != -1) {
            if (i < length - 1) {
                val c0 = text[i + 1]
                if (isFormatColor(c0)) {
                    string = "§$c0"
                } else if (isFormatSpecial(c0)) {
                    string = "$string§$c0"
                }
            }
        }

        return string
    }

    private fun isFormatColor(colorChar: Char): Boolean {
        return colorChar in '0'..'9' || colorChar in 'a'..'f' || colorChar in 'A'..'F'
    }

    private fun isFormatSpecial(formatChar: Char): Boolean {
        return formatChar in 'k'..'o' || formatChar in 'K'..'O' || formatChar in "rR"
    }

    fun String.removePrefix(prefixPattern: Pattern): String {
        val matcher = prefixPattern.matcher(this)
        // Only remove the prefix if it matches at the start of the string
        return if (matcher.find() && matcher.start() == 0) {
            this.substring(matcher.end())
        } else this
    }
}
