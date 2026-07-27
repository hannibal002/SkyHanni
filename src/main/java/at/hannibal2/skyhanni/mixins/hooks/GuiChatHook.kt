package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.chroma.ChromaFontManager
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

object GuiChatHook {

    @JvmStatic
    var replacementComponent: Component? = null

    fun replaceEntireComponent(title: String, chatStyle: Style) {
        // Initialise new component
        val newComponent = title.asComponent()
        newComponent.style = chatStyle

        replacementComponent = newComponent
    }

    fun replaceHoverEventComponent(component: Component) {
        replacementComponent = component
    }

    @JvmStatic
    fun getReplacement(): Component {
        return replacementComponent ?: "No replacement component was set".asComponent()
    }

    @JvmStatic
    fun wrapChatRender(
        original: Operation<Void>,
        chatGraphicsAccess: ChatComponent.ChatGraphicsAccess,
        screenHeight: Int,
        ticks: Int,
        displayMode: ChatComponent.DisplayMode,
    ) {
        ChromaFontManager.renderingChat = true
        ModifyVisualWords.changeWords = false
        original.call(chatGraphicsAccess, screenHeight, ticks, displayMode)
        ChromaFontManager.renderingChat = false
        ModifyVisualWords.changeWords = true
    }
}
