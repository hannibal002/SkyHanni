package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.PathfindConfig
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.util.ChatComponentText
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object NavigationFeedback {

    private val config get() = SkyHanniMod.feature.misc.pathfinding
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
        val guiFormattedText = component.formattedText.replace("§e[SkyHanni] ", "§e")
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
