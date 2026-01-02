package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.unformattedTextForChatCompat
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

object GuiChatHook {

    @JvmStatic
    var currentComponent: Component? = null

    lateinit var replacement: Component

    fun replaceEntireComponent(title: String, chatStyle: Style) {
        if (!this::replacement.isInitialized) return

        // Initialise new component
        val newComponent = title.asComponent()
        newComponent.setStyle(chatStyle)

        replacement = newComponent
    }

    fun replaceOnlyHoverEvent(hoverEvent: HoverEvent) {
        if (!this::replacement.isInitialized) return

        // Initialise new component
        val newComponent = replacement.unformattedTextForChatCompat().asComponent {
            style = replacement.style
            style.withHoverEvent(hoverEvent)
        }

        replacement = newComponent
    }

    fun getReplacementAsIChatComponent(): Component {
        if (!this::replacement.isInitialized) {
            // Return an extremely basic chat component as to not error downstream
            return "Original component was not set".asComponent()
        }
        return replacement
    }
}
