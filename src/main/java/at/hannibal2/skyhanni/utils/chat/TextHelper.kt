package at.hannibal2.skyhanni.utils.chat

import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addDeletableMessageToChat
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.toChatFormatting
import at.hannibal2.skyhanni.utils.compat.withColor
import com.mojang.authlib.GameProfile
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ComponentRenderUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.contents.objects.AtlasSprite
import net.minecraft.network.chat.contents.objects.PlayerSprite
import net.minecraft.resources.Identifier
import net.minecraft.world.item.component.ResolvableProfile
import java.awt.Color
import java.util.Optional

@Suppress("TooManyFunctions")
object TextHelper {

    val NEWLINE = "\n".asComponent()
    val HYPHEN = "-".asComponent()
    val SPACE = " ".asComponent()
    val EMPTY = "".asComponent()
    val chromaStyle by lazy { TextColor(0xFFFFFE, "chroma") }

    fun text(text: String, init: MutableComponent.() -> Unit = {}) = text.asComponent(init)
    fun String.asComponent(init: MutableComponent.() -> Unit = {}): MutableComponent =
        Component.literal(this).also(init)

    fun multiline(vararg lines: Any?) = join(*lines, separator = NEWLINE)
    fun join(vararg components: Any?, separator: Component? = null): Component {
        val result = "".asComponent()
        components.forEachIndexed { index, component ->
            when (component) {
                is Component -> result.append(component)
                is String -> result.append(component)
                is List<*> -> result.append(join(*component.toTypedArray(), separator = separator))
                null -> return@forEachIndexed
                else -> error("Unsupported type: ${component::class.simpleName}")
            }

            if (index < components.size - 1 && separator != null) {
                result.append(separator)
            }
        }
        return result
    }

    fun Component.style(init: Style.() -> Unit): Component {
        this.style.init()
        return this
    }

    fun Component.prefix(prefix: String): Component = join(prefix, this)
    fun Component.suffix(suffix: String): Component = join(this, suffix)
    fun Component.wrap(prefix: String, suffix: String) = this.prefix(prefix).suffix(suffix)

    fun String.applyFormattingFrom(original: Component): Component =
        asComponent { style = original.style }

    fun Component.contains(other: String): Boolean = string.contains(other)

    fun Component.startsWith(other: String): Boolean = string.startsWith(other)

    fun Component.width(): Int = Minecraft.getInstance().font.width(this.string)

    fun String.width(): Int = Minecraft.getInstance().font.width(this)

    fun Component.fitToChat(): Component {
        val width = this.width()
        val maxWidth = MinecraftCompat.hud.chat.width
        if (width < maxWidth) {
            val repeat = maxWidth / width
            val component = "".asComponent()
            repeat(repeat) { component.append(this) }
            return component
        }
        return this
    }

    fun Component.center(width: Int = MinecraftCompat.hud.chat.width): Component {
        val textWidth = this.width()
        val spaceWidth = SPACE.width().coerceAtLeast(1)
        val padding = (width - textWidth).coerceAtLeast(0) / 2
        return join(" ".repeat(padding / spaceWidth), this)
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

    fun String.splitLines(width: Int): String = splitText(
        this,
        width,
    ).joinToString("\n") { it.removePrefix("§r") }

    private fun splitText(text: String, width: Int): List<String> {
        val lines = ComponentRenderUtils.wrapComponents(text.asComponent(), width, Minecraft.getInstance().font)
        val strings: MutableList<String> = ArrayList(lines.size)
        for (line in lines) {
            var newLine = ""
            var lastColor: TextColor? = null
            var lastFormatting = ""
            line.accept { _, style, codePoint ->
                val color = style.color
                if (color != lastColor) {
                    lastColor = color
                    lastFormatting = ""
                    if (color != null) {
                        newLine += color.toChatFormatting()
                    }
                }
                var newFormatting = ""
                newFormatting = if (style.isBold) "§l"
                else if (style.isItalic) "§o"
                else if (style.isUnderlined) "§n"
                else if (style.isStrikethrough) "§m"
                else if (style.isObfuscated) "§k"
                else ""

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

    fun Component.send(id: Int = 0, bypassSelfMessages: Boolean = false) =
        addDeletableMessageToChat(this, id, bypassSelfMessages)

    fun List<Component>.send(id: Int = 0, bypassSelfMessages: Boolean = false) {
        val parent = "".asComponent()
        forEach {
            parent.siblings.add(it)
            parent.siblings.add("\n".asComponent())
        }

        parent.send(id, bypassSelfMessages)
    }

    fun Component.onClick(expiresAt: SimpleTimeMark = SimpleTimeMark.farFuture(), oneTime: Boolean = true, onClick: () -> Any) {
        val token = ChatClickActionManager.createAction(onClick, expiresAt, oneTime)
        this.command = "/shaction $token"
    }

    fun Component.onHover(tip: String) {
        this.hover = tip.asComponent()
    }

    fun Component.onHover(tips: List<String>) {
        this.hover = tips.joinToString("\n").asComponent()
    }

    // TODO remove this deprecated alias in November 2026
    @Deprecated(
        "Moved to PaginatedList",
        ReplaceWith(
            "PaginatedList.createDivider(dividerColor)",
            "at.hannibal2.skyhanni.utils.chat.PaginatedList",
        ),
    )
    fun createDivider(dividerColor: ChatFormatting = ChatFormatting.BLUE) =
        PaginatedList.createDivider(dividerColor)

    // TODO remove this deprecated alias in November 2026
    @Deprecated(
        "Moved to PaginatedList",
        ReplaceWith(
            "PaginatedList.displayPaginatedList(title, list, chatLineId, emptyMessage, currentPage, maxPerPage, dividerColor, formatter)",
            "at.hannibal2.skyhanni.utils.chat.PaginatedList",
        ),
    )
    fun <T> displayPaginatedList(
        title: String,
        list: List<T>,
        chatLineId: Int,
        emptyMessage: String,
        currentPage: Int = 1,
        maxPerPage: Int = 15,
        dividerColor: ChatFormatting = ChatFormatting.BLUE,
        formatter: (T) -> Component,
    ): Unit = PaginatedList.displayPaginatedList(
        title, list, chatLineId, emptyMessage, currentPage, maxPerPage, dividerColor, formatter,
    )

    fun createGradientText(start: LorenzColor, end: LorenzColor, string: String): Component {
        return createGradientText(start.toColor(), end.toColor(), string)
    }

    fun createGradientText(start: Color, end: Color, string: String): Component {
        val length = string.length
        val text = componentBuilder {
            for ((index, char) in string.withIndex()) {
                val color = ColorUtils.blendRGB(start, end, index, length).rgb
                append(char.toString()) {
                    withColor(color)
                }
            }
        }
        return text
    }

    fun matcher(component: Component, match: String): Component? {
        var index = 0
        var newComponent: Component = Component.empty()
        var currentString = ""
        var done = false

        component.forEachNonEmpty { style, string ->
            fun String.newText() = asComponent().withStyle(style)
            if (done) return@forEachNonEmpty
            for (c in string) {
                if (index >= match.length) {
                    if (currentString.isNotEmpty()) {
                        newComponent.append(currentString.newText())
                    }
                    currentString = ""
                    done = true
                    return@forEachNonEmpty
                }
                if (c == match[index]) {
                    currentString += c
                    index++
                } else {
                    currentString = ""
                    newComponent = Component.empty()
                    index = 0
                }
            }
            if (currentString.isNotEmpty()) {
                newComponent.append(currentString.newText())
            }
            currentString = ""
        }
        return newComponent.takeIf { it.string.isNotEmpty() }
    }

    fun split(component: Component, delimiter: String): List<Component>? {
        val newComponents = mutableListOf<MutableComponent>()
        var currentComponent = Component.empty()

        component.forEachNonEmpty { style, string ->
            fun String.toStyledComponent() = this.asComponent().withStyle(style)
            val split = string.split(delimiter)
            if (split.isEmpty() || split.size == 1) {
                currentComponent.append(string.toStyledComponent())
            } else {
                currentComponent.append(split.first().toStyledComponent())
                if (currentComponent.string.isNotEmpty()) newComponents.add(currentComponent)
                currentComponent = Component.empty()
                for ((index, str) in split.withIndex()) {
                    if (index == 0) continue
                    currentComponent.append(str.toStyledComponent())
                    if (currentComponent.string.isNotEmpty()) newComponents.add(currentComponent)
                    currentComponent = Component.empty()
                }
            }
        }

        if (currentComponent.string.isNotEmpty()) newComponents.add(currentComponent)
        return newComponents.takeIf { it.isNotEmpty() }
    }

    fun createAtlasSprite(sprite: String, atlas: String = "gui", namespace: String = "skyhanni"): Component {
        val atlasId = Identifier.withDefaultNamespace(atlas)
        val texture = Identifier.fromNamespaceAndPath(namespace, sprite)
        return Component.`object`(AtlasSprite(atlasId, texture)).withColor(ChatFormatting.WHITE)
    }

    private fun Component.forEachNonEmpty(visitor: (Style, String) -> Unit) {
        visitNonEmpty { style, string ->
            visitor(style, string)
            Optional.empty()
        }
    }

    private fun <T : Any> Component.visitNonEmpty(visitor: (Style, String) -> Optional<T>): Optional<T> = this.visit(
        { style, string ->
            if (string.isEmpty()) Optional.empty()
            else visitor(style, string)
        },
        Style.EMPTY,
    )

    fun List<Component>.merge(): MutableComponent {
        val component = "".asComponent()
        for ((index, item) in withIndex()) {
            component.append(item)
            if (index < size - 1) component.append(" ")
        }
        return component
    }

    fun GameProfile.asComponent(): Component {
        val resolvedProfile = ResolvableProfile.createResolved(this)
        val sprite = PlayerSprite(resolvedProfile, false)
        return Component.`object`(sprite)
    }
}
