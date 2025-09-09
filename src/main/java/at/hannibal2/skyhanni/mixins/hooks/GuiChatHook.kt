package at.hannibal2.skyhanni.mixins.hooks import at.hannibal2.skyhanni.utils.compat.unformattedTextForChatCompat

import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.Style
//#if MC < 1.21
//$$ import net.minecraft.text.Text
//#endif

object GuiChatHook {

    @JvmStatic
    var currentComponent: Text? = null

    lateinit var replacement: Text

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
            //#if MC < 1.21
            //$$ style.chatHoverEvent = hoverEvent
            //#else
            style.withHoverEvent(hoverEvent)
            //#endif
        }

        replacement = newComponent
    }

    fun getReplacementAsIChatComponent(): Text {
        if (!this::replacement.isInitialized) {
            // Return an extremely basic chat component as to not error downstream
            return "Original component was not set".asComponent()
        }
        return replacement
    }
}
