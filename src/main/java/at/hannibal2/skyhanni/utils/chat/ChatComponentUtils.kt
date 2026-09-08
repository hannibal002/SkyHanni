package at.hannibal2.skyhanni.utils.chat

import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.defaultStyleConstructor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.unformattedTextForChatCompat
import at.hannibal2.skyhanni.utils.compat.value
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

object ChatComponentUtils {

    private val colorMap = ChatFormatting.entries.associateBy { it.toString()[1] }

    private fun enumChatFormattingByCode(char: Char): ChatFormatting? = colorMap[char]

    fun replaceIfNeeded(original: Component, newText: String): Component? =
        replaceIfNeeded(original, newText.asComponent())

    fun <T : Component> replaceIfNeeded(
        original: T,
        newText: T,
    ): T? {
        if (doLookTheSame(original, newText)) return null
        return newText
    }

    private fun doLookTheSame(left: Component, right: Component): Boolean {
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

    /**
     * Applies a transformation on the message of a SystemMessageEvent if possible.
     */
    fun SystemMessageEvent.Modify.applyIfPossible(
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
}
