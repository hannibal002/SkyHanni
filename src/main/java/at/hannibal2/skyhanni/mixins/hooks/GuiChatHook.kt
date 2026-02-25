package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

object GuiChatHook {

    @JvmStatic
    var currentComponent: Component? = null

    var replacementComponent: Component? = null

    fun replaceEntireComponent(title: String, chatStyle: Style) {
        // Initialise new component
        val newComponent = title.asComponent()
        newComponent.setStyle(chatStyle)

        replacementComponent = newComponent
    }

    fun replaceHoverEventComponent(component: Component) {
        replacementComponent = component
    }

    fun getReplacement(): Component {
        return replacementComponent ?: "No replacement component was set".asComponent()
    }
}
