package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.utils.compat.SkyHanniChromeScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue

class ChatHistoryScreen(
    val history: List<ChatManager.MessageFilteringResult>,
) : SkyHanniChromeScreen() {

    override val screenTitle = "Chat History"
    val scrollValue = ScrollValue()

    override fun buildContent(): Renderable = ChatHistoryGui.buildContent(this)
}
