package at.hannibal2.hanni.features.misc.pathfind

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.misc.PathfindConfig
import at.hannibal2.hanni.data.IslandGraphs
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.RenderDisplayHelper
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import at.hannibal2.hanni.utils.chat.TextHelper.send
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.primitives.text
import net.minecraft.util.ChatComponentText
import kotlin.time.Duration.Companion.seconds

@HanniModule
object NavigationFeedback {

    private val config get() = HanniMod.feature.misc.pathfinding
    private val pathFindMessageId = ChatUtils.getUniqueMessageId()
    private var guiRenderable: Renderable? = null
    private var lastChatMessageSent = SimpleTimeMark.farPast()
    private var navActive: Boolean = false
    private var navLastActive: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.feedbackMode) {
            guiRenderable = null
        }
    }

    private fun isActive() = navActive || navLastActive.passedSince() < 3.seconds

    fun setNavInactive() { navActive = false }

    fun sendPathFindMessage(message: String) = sendPathFindMessage(message.asComponent())
    fun sendPathFindMessage(component: ChatComponentText): Boolean {
        navActive = true
        navLastActive = SimpleTimeMark.now()
        return when (config.feedbackMode.get()) {
            PathfindConfig.FeedbackMode.NONE -> false
            PathfindConfig.FeedbackMode.CHAT -> sendChatFeedback(component)
            PathfindConfig.FeedbackMode.GUI -> sendGuiFeedback(component)
            else -> false
        }
    }

    private fun sendChatFeedback(component: ChatComponentText): Boolean {
        if (lastChatMessageSent.passedSince() < config.chatUpdateInterval.duration) return false
        component.send(pathFindMessageId)
        lastChatMessageSent = SimpleTimeMark.now()
        return true
    }

    private fun sendGuiFeedback(component: ChatComponentText): Boolean {
        val guiFormattedText = component.formattedText.replace("§e[Hanni] ", "§e")
        guiRenderable = Renderable.clickable(
            Renderable.text(guiFormattedText),
            onLeftClick = IslandGraphs::cancelClick,
            tips = listOf("§eClick to stop navigating!")
        )
        return true
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isActive() && config.feedbackMode.get() == PathfindConfig.FeedbackMode.GUI },
            onRender = {
                guiRenderable?.let {
                    config.position.renderRenderable(it, "Pathfind Feedback")
                }
            },
        )
    }
}
