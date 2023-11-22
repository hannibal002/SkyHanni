package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.mixins.transformers.AccessorChatComponentText
import at.hannibal2.skyhanni.utils.GuiRenderUtils.darkenColor
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiUtilRenderComponents
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import org.intellij.lang.annotations.Language
import java.util.Base64
import java.util.NavigableMap
import java.util.UUID
import java.util.function.Predicate
import java.util.regex.Matcher
import java.util.regex.Pattern

object StringUtils {
    // TODO USE SH-REPO
    private val playerChatPattern = "(?<important>.*?)(?:§[f7r])*: .*".toPattern()
    private val chatUsernamePattern =
        "^(?:§\\w\\[§\\w\\d+§\\w] )?(?:(?:§\\w)+\\S )?(?<rankedName>(?:§\\w\\[\\w.+] )?(?:§\\w)?(?<username>\\w+))(?: (?:§\\w)?\\[.+?])?".toPattern()
    private val whiteSpaceResetPattern = "^(?:\\s|§r)*|(?:\\s|§r)*$".toPattern()
    private val whiteSpacePattern = "^\\s*|\\s*$".toPattern()
    private val resetPattern = "(?i)§R".toPattern()

    fun String.trimWhiteSpaceAndResets(): String = whiteSpaceResetPattern.matcher(this).replaceAll("")
    fun String.trimWhiteSpace(): String = whiteSpacePattern.matcher(this).replaceAll("")
    fun String.removeResets(): String = resetPattern.matcher(this).replaceAll("")

    fun String.firstLetterUppercase(): String {
        if (isEmpty()) return this

        val lowercase = lowercase()
        val first = lowercase[0].uppercase()
        return first + lowercase.substring(1)
    }

    fun String.removeColor(): String {
        val builder = StringBuilder(this.length)

        var counter = 0
        while (counter < this.length) {
            if (this[counter] == '§') {
                counter += 2
                continue
            }
            builder.append(this[counter])
            counter++
        }

        return builder.toString()
    }

    /**
     * From https://stackoverflow.com/questions/10711494/get-values-in-treemap-whose-string-keys-start-with-a-pattern
     */
    fun <T> subMapWithKeysThatAreSuffixes(prefix: String, map: NavigableMap<String?, T>): Map<String?, T>? {
        if ("" == prefix) return map
        val lastKey = nextLexicographicallyStringWithSameLength(prefix)
        return map.subMap(prefix, true, lastKey, false)
    }

    private fun nextLexicographicallyStringWithSameLength(input: String): String {
        val lastCharPosition = input.length - 1
        val inputWithoutLastChar = input.substring(0, lastCharPosition)
        val lastChar = input[lastCharPosition]
        val incrementedLastChar = (lastChar.code + 1).toChar()
        return inputWithoutLastChar + incrementedLastChar
    }

    fun UUID.toDashlessUUID(): String {
        return toString().replace("-", "")
    }

    @Deprecated("Do not create a regex pattern each time.", ReplaceWith("toPattern()"))
    fun String.matchRegex(@Language("RegExp") regex: String): Boolean = regex.toRegex().matches(this)

    private fun String.removeAtBeginning(text: String): String =
        if (this.startsWith(text)) substring(text.length) else this

    // TODO find better name for this method
    inline fun <T> Pattern.matchMatcher(text: String, consumer: Matcher.() -> T) =
        matcher(text).let { if (it.matches()) consumer(it) else null }

    fun String.cleanPlayerName(): String {
        val split = trim().split(" ")
        return if (split.size > 1) {
            split[1].removeColor()
        } else {
            split[0].removeColor()
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
        val stringPattern = "§[0123456789abcdef].*".toPattern()

        val matcher = stringPattern.matcher(string)
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

    fun optionalPlural(number: Int, singular: String, plural: String) =
        "${number.addSeparators()} " + if (number == 1) singular else plural

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
                if (component.text_skyhanni().contains(toReplace)) {
                    component.setText_skyhanni(component.text_skyhanni().replace(toReplace, replacement))
                    return@modifyFirstChatComponent true
                }
                return@modifyFirstChatComponent false
            }
            return@modifyFirstChatComponent false
        }
        return chatComponent
    }

    fun String.getPlayerNameFromChatMessage(): String? {
        val matcher = matchPlayerChatMessage(this) ?: return null
        return matcher.group("username")
    }

    fun String.getPlayerNameAndRankFromChatMessage(): String? {
        val matcher = matchPlayerChatMessage(this) ?: return null
        return matcher.group("rankedName")
    }

    private fun matchPlayerChatMessage(string: String): Matcher? {
        var username = ""
        var matcher = playerChatPattern.matcher(string)
        if (matcher.matches()) {
            username = matcher.group("important").removeResets()
        }
        if (username == "") return null

        if (username.contains(">")) {
            username = username.substring(username.indexOf('>') + 1).trim()
        }

        username = username.removePrefix("§dFrom ")
        username = username.removePrefix("§dTo ")

        matcher = chatUsernamePattern.matcher(username)
        return if (matcher.matches()) matcher else null
    }

    fun String.convertToFormatted(): String {
        return this.replace("&&", "§")
    }

    fun Pattern.matches(string: String) = matcher(string).matches()
}
