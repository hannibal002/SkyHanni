package at.hannibal2.skyhanni.utils

import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.IChatComponent
import java.util.Stack
import java.util.regex.Matcher
import java.util.regex.Pattern

object ComponentMatcherUtils {

    fun IChatComponent.intoSpan(): ComponentSpan {
        val text = this.unformattedText
        return ComponentSpan(
            this,
            text,
            0,
            text.length
        )
    }

    fun Pattern.styledMatcher(span: ComponentSpan): ComponentMatcher {
        val matcher = matcher(span.getText())
        return ComponentMatcher(matcher, span)
    }

    inline fun <T> Pattern.matchStyledMatcher(chat: IChatComponent, consumer: ComponentMatcher.() -> T) =
        matchStyledMatcher(chat.intoSpan(), consumer)

    inline fun <T> Pattern.matchStyledMatcher(span: ComponentSpan, consumer: ComponentMatcher.() -> T) =
        styledMatcher(span).let { if (it.matches()) consumer(it) else null }

    inline fun <T> Pattern.findStyledMatcher(chat: IChatComponent, consumer: ComponentMatcher.() -> T) =
        findStyledMatcher(chat.intoSpan(), consumer)

    inline fun <T> Pattern.findStyledMatcher(span: ComponentSpan, consumer: ComponentMatcher.() -> T) =
        styledMatcher(span).let { if (it.find()) consumer(it) else null }
}

class ComponentMatcher internal constructor(
    val matcher: Matcher,
    val span: ComponentSpan,
) {
    fun matches(): Boolean {
        return matcher.matches()
    }

    fun find(): Boolean {
        return matcher.find()
    }

    fun group(index: Int): ComponentSpan? {
        val start = matcher.start(index)
        if (start < 0) return null
        return this.span.slice(start, matcher.end(index))
    }

    fun group(name: String): ComponentSpan? {
        val start = matcher.start(name)
        if (start < 0) return null
        return this.span.slice(start, matcher.end(name))
    }
}

class ComponentSpan internal constructor(
    val textComponent: IChatComponent,
    private val cachedText: String,
    val start: Int, val end: Int
) {
    init {
        require(start <= end)
        require(0 <= start)
        require(end <= cachedText.length)
    }

    val length get() = end - start
    fun slice(start: Int, end: Int): ComponentSpan {
        require(0 <= start)
        require(start <= end)
        require(end <= length)
        return ComponentSpan(textComponent, cachedText, this.start + start, this.start + end)
    }

    fun getText() = cachedText.substring(start, end)

    fun sampleStyleAtStart(): ChatStyle? = sampleAtStart().chatStyle

    fun sampleComponents(): List<IChatComponent> {
        return sampleSlicedComponents().map { it.first }
    }

    fun sampleSlicedComponents(): List<Triple<IChatComponent, Int, Int>> {
        var index = start
        val workStack = Stack<IChatComponent>()
        workStack.push(textComponent)
        var lastComponent = textComponent
        val listBuilder = mutableListOf<Triple<IChatComponent, Int, Int>>()
        while (workStack.isNotEmpty()) {
            val currentComponent = workStack.pop()
            if (index + length <= 0) {
                break
            }
            for (sibling in currentComponent.siblings.reversed()) {
                workStack.push(sibling)
            }
            val rawText = currentComponent.unformattedTextForChat
            index -= rawText.length
            if (index < 0) {
                listBuilder.add(
                    Triple(
                        currentComponent,
                        (rawText.length + index).coerceAtLeast(0),
                        (rawText.length + index + length).coerceAtMost(rawText.length)
                    )
                )
            }
            lastComponent = currentComponent
        }
        if (listBuilder.isEmpty())
            listBuilder.add(Triple(lastComponent, 0, 0))
        return listBuilder
    }

    fun sampleAtStart(): IChatComponent {
        return sampleComponents().first()
    }

    /**
     * Create an [IChatComponent] that looks identical to this slice, including hover events and such.
     * This new chat component will be structurally different (flat) and not therefore not have the same property
     * inheritances as the old [textComponent]. Be therefore careful when modifying styles. This new component will also
     * only use [ChatComponentText], converting any other [IChatComponent] in the process.
     */
    fun intoComponent(): IChatComponent {
        val parent = ChatComponentText("")
        parent.chatStyle = ChatStyle()
        sampleSlicedComponents().forEach {
            val copy = ChatComponentText(it.first.unformattedTextForChat.substring(it.second, it.third))
            copy.chatStyle = it.first.chatStyle.createDeepCopy()
            parent.appendSibling(copy)
        }
        return parent
    }

    fun sampleStyles(): List<ChatStyle> {
        return sampleComponents().map { it.chatStyle }
    }

}
