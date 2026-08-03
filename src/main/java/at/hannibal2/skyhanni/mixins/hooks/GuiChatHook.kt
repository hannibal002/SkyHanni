package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.chroma.ChromaFontManager
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.test.command.ErrorManager
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

    // Required for Java interop with Operation<Void>
    @Suppress("ForbiddenVoid")
    @JvmStatic
    fun wrapChatRender(
        original: Operation<Void>,
        chatGraphicsAccess: ChatComponent.ChatGraphicsAccess,
        screenHeight: Int,
        ticks: Int,
        //? if >= 26.1 {
        displayMode: ChatComponent.DisplayMode,
        //?} else {
        /*displayMode: Boolean,*/
        //?}
    ) {
        ChromaFontManager.renderingChat = true
        ModifyVisualWords.changeWords = false
        try {
            original.call(chatGraphicsAccess, screenHeight, ticks, displayMode)
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(e, "Error in chat rendering")
        } finally {
            ChromaFontManager.renderingChat = false
            ModifyVisualWords.changeWords = true
        }
    }
}
