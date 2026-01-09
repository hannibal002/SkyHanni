package at.hannibal2.skyhanni.test import at.hannibal2.skyhanni.utils.compat.formattedTextCompat

import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.findStyledMatcher
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.ChatFormatting
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class ComponentSpanTest {
    private fun text(string: String, init: Component.() -> Unit = {}) = Component(string).also(init)

    @Test
    fun testComponent() {
        val component = text("12345") {
            append(text("12345") {
                style = Style().withColor(ChatFormatting.RED)
            })
            append(text("12345"))
        }
        val span = component.intoSpan()
        require(span.sampleStyleAtStart()?.isEmpty == true)
        require(span.slice(5, 8).sampleStyleAtStart()?.color == ChatFormatting.RED)
        require(span.slice(8, 12).sampleStyleAtStart()?.color == ChatFormatting.RED)
        require(span.slice(10, 12).sampleStyleAtStart()?.isEmpty == true)
        require(span.slice(4, 11).intoComponent().formattedTextCompat() == "§r5§r§c12345§r1§r")
    }

    @Test
    fun testRemovePrefix() {
        val component = text("12345") {
            append(text("12345"))
            append(text("12345§r"))
        }.intoSpan()
        val prefixRemoved = component.removePrefix("123")
        require(prefixRemoved.getText() == "451234512345§r")
        require(component.stripHypixelMessage().getText() == "123451234512345")
        require(component.stripHypixelMessage().slice().getText() == "123451234512345")
        require(component.slice(0, 0).slice(0, 0).getText() == "")
    }

    @Test
    fun testRegex() {
        val component = text("12345") {
            append(text("abcdef") {
                style = Style().withColor(ChatFormatting.RED)
            })
            append(text("12345"))
        }
        Pattern.compile("[0-9]*(?<middle>[a-z]+)[0-9]*").matchStyledMatcher(component) {
            require(groupOrThrow("middle")?.sampleStyleAtStart()?.color == ChatFormatting.RED)
        }
        val middlePartExtracted =
            Pattern.compile("[0-9]*(?<middle>[0-9][a-z]+[0-9])[0-9]*").matchStyledMatcher(component) {
                require(groupOrThrow("middle")?.sampleComponents()?.size == 3)
                require(
                    groupOrThrow("middle")?.sampleStyles()?.find { it.color != null }?.color == ChatFormatting.RED
                )
                groupOrThrow("middle")
            }!!
        Pattern.compile("(?<whole>c)").findStyledMatcher(middlePartExtracted) {
            require(groupOrThrow("whole")?.sampleStyleAtStart()?.color == ChatFormatting.RED)
        }
    }

}
