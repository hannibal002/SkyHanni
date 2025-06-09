package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
//#if MC < 1.21
import net.minecraft.util.IChatComponent
//#endif

object GuiChatHook {

    @JvmStatic
    var currentComponent: IChatComponent? = null

    lateinit var replacement: ChatComponentText

    fun replaceEntireComponent(title: String, chatStyle: ChatStyle) {
        if (!this::replacement.isInitialized) return

        // Initialise new component
        val newComponent = title.asComponent()
        newComponent.setChatStyle(chatStyle)

        replacement = newComponent
    }

    fun replaceOnlyHoverEvent(hoverEvent: HoverEvent) {
        if (!this::replacement.isInitialized) return

        // Initialise new component
        val newComponent = replacement.unformattedTextForChat.asComponent {
            chatStyle = replacement.chatStyle
            //#if MC < 1.21
            chatStyle.chatHoverEvent = hoverEvent
            //#else
            //$$ style.withHoverEvent(hoverEvent)
            //#endif
        }

        replacement = newComponent
    }

    fun getReplacementAsIChatComponent(): IChatComponent {
        if (!this::replacement.isInitialized) {
            // Return an extremely basic chat component as to not error downstream
            return "Original component was not set".asComponent()
        }
        return replacement
    }
}
