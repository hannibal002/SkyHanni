package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.world.item.Items

// Taken and modified from Not Enough Updates https://github.com/NotEnoughUpdates/NotEnoughUpdates
@SkyHanniModule
object CustomTodosGui {

    private val todos get() = SkyHanniMod.customTodos.customTodos

    private val config get() = SkyHanniMod.feature.misc.customTodos

    @Suppress("ReturnCount")
    private fun matchString(todo: CustomTodo, text: String): MatchType {
        if (!todo.isValid()) return MatchType.NO_MATCH
        val cleanedText = if (todo.ignoreColorCodes) text.removeColor() else text

        when (todo.triggerMatcher) {
            CustomTodo.TriggerMatcher.REGEX -> {
                if (cleanedText.matches(todo.getRegex() ?: return MatchType.NO_MATCH)) return MatchType.MATCH
                if (cleanedText.matches(todo.getAntiTriggerRegex() ?: return MatchType.NO_MATCH)) return MatchType.ANTI_MATCH
            }

            CustomTodo.TriggerMatcher.STARTS_WITH -> {
                if (cleanedText.startsWith(todo.trigger)) return MatchType.MATCH
                if (todo.antiTrigger.isNotBlank() && cleanedText.startsWith(todo.antiTrigger)) return MatchType.ANTI_MATCH
            }

            CustomTodo.TriggerMatcher.CONTAINS -> {
                if (cleanedText.contains(todo.trigger)) return MatchType.MATCH
                if (todo.antiTrigger.isNotBlank() && cleanedText.contains(todo.antiTrigger)) return MatchType.ANTI_MATCH
            }

            CustomTodo.TriggerMatcher.EQUALS -> {
                if (cleanedText == todo.trigger) return MatchType.MATCH
                if (todo.antiTrigger.isNotBlank() && cleanedText == todo.antiTrigger) return MatchType.ANTI_MATCH
            }
        }
        return MatchType.NO_MATCH
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListUpdate(event: TabListUpdateEvent) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.TAB_LIST) return@forEach
            event.tabList.forEach { line ->
                if (matchString(todo, line.formattedTextCompat()) == MatchType.MATCH) todo.setDoneNow()
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.SIDEBAR) return@forEach
            event.new.forEach { line ->
                if (matchString(todo, line) == MatchType.MATCH) todo.setDoneNow()
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.ACTION_BAR) return@forEach
            val matchType = matchString(todo, event.actionBar)
            if (matchType == MatchType.MATCH) todo.setDoneNow()
            if (matchType == MatchType.ANTI_MATCH) todo.antiTriggered()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.CHAT) return@forEach
            val matchType = matchString(todo, event.message)
            if (matchType == MatchType.MATCH) todo.setDoneNow()
            if (matchType == MatchType.ANTI_MATCH) todo.antiTriggered()
        }
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (todos.isEmpty()) return
        val display = mutableListOf<Renderable>()
        for ((index, todo) in todos.withIndex()) {
            val renderable: Renderable
            try {
                renderable = todo.getRenderable() ?: continue
            } catch (e: Exception) {
                continue
            }
            if (config.separateGuis) {
                todo.position.renderRenderable(renderable, posLabel = "${todo.label} $index")
            } else {
                display.add(renderable)
            }
        }
        if (!config.separateGuis) {
            config.position.renderRenderables(display, posLabel = "Custom Todo Display")
        }
    }

    fun parseItem(icon: String): SafeItemStack {
        if (icon.isEmpty()) return SafeItemStack(Items.PAINTING)
        return NeuInternalName.fromItemName(icon).getItemStack()
    }

    private enum class MatchType {
        MATCH,
        NO_MATCH,
        ANTI_MATCH,
    }
}
