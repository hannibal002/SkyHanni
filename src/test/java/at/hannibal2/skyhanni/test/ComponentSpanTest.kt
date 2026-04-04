package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.findStyledMatcher
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class ComponentSpanTest {
    @Test
    fun testComponent() {
        val component = componentBuilder {
            append("12345")
            append("12345") {
                withColor(ChatFormatting.RED)
            }
            append("12345")
        }
        val span = component.intoSpan()
        assert(span.sampleStyleAtStart().isEmpty)
        assertEquals(ChatFormatting.RED.color, span.slice(5, 8).sampleStyleAtStart().color?.value)
        assertEquals(ChatFormatting.RED.color, span.slice(8, 12).sampleStyleAtStart().color?.value)
        assert(span.slice(10, 12).sampleStyleAtStart().isEmpty)
        // TODO fix (make sure to not accidentally break mod features if you change the behavior)
        //assertEquals("§r5§r§c12345§r1§r", span.slice(4, 11).intoComponent().formattedTextCompat())
    }

    @Test
    fun testRemovePrefix() {
        val component = componentBuilder {
            append("12345")
            append("12345")
            append("12345§r")
        }.intoSpan()
        val prefixRemoved = component.removePrefix("123")
        assertEquals("451234512345§r", prefixRemoved.getText())
        assertEquals("123451234512345", component.stripHypixelMessage().getText())
        assertEquals("123451234512345", component.stripHypixelMessage().slice().getText())
        assertEquals("", component.slice(0, 0).slice(0, 0).getText())
    }

    @Test
    fun testRegex() {
        val component = componentBuilder {
            append("12345")
            append("abcdef") {
                withColor(ChatFormatting.RED)
            }
            append("12345")
        }
        Pattern.compile("[0-9]*(?<middle>[a-z]+)[0-9]*").matchStyledMatcher(component) {
            assertEquals(ChatFormatting.RED.color, groupOrThrow("middle").sampleStyleAtStart().color?.value)
        }
        val middlePartExtracted =
            Pattern.compile("[0-9]*(?<middle>[0-9][a-z]+[0-9])[0-9]*").matchStyledMatcher(component) {
                assertEquals(3, groupOrThrow("middle").sampleComponents().size)
                assertEquals(
                    ChatFormatting.RED.color,
                    groupOrThrow("middle").sampleStyles().find {
                        it.color != null
                    }?.color?.value
                )
                groupOrThrow("middle")
            }!!
        Pattern.compile("(?<whole>c)").findStyledMatcher(middlePartExtracted) {
            assertEquals(ChatFormatting.RED.color, groupOrThrow("whole").sampleStyleAtStart().color?.value)
        }
    }

}
