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
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// Taken and modified from Not Enough Updates https://github.com/NotEnoughUpdates/NotEnoughUpdates
@SkyHanniModule
object CustomTodosGui {

    private val todos get() = SkyHanniMod.customTodos.customTodos

    private val config get() = SkyHanniMod.feature.misc.customTodos

    private fun matchString(todo: CustomTodo, text: String): Boolean {
        val cleanedText = if (todo.ignoreColorCodes) text.removeColor() else text
        return when (todo.triggerMatcher) {
            CustomTodo.TriggerMatcher.REGEX -> cleanedText.matches(todo.trigger.toRegex())
            CustomTodo.TriggerMatcher.STARTS_WITH -> cleanedText.startsWith(todo.trigger)
            CustomTodo.TriggerMatcher.CONTAINS -> cleanedText.contains(todo.trigger)
            CustomTodo.TriggerMatcher.EQUALS -> cleanedText == todo.trigger
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListUpdate(event: TabListUpdateEvent) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.TAB_LIST) return@forEach
            event.tabList.forEach { line ->
                if (matchString(todo, line)) todo.setDoneNow()
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.SIDEBAR) return@forEach
            event.new.forEach { line ->
                if (matchString(todo, line)) todo.setDoneNow()
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.ACTION_BAR) return@forEach
            if (matchString(todo, event.actionBar)) todo.setDoneNow()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        todos.forEach { todo ->
            if (todo.triggerTarget != CustomTodo.TriggerTarget.CHAT) return@forEach
            if (matchString(todo, event.message)) todo.setDoneNow()
        }
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (todos.isEmpty()) return
        val display = mutableListOf<Renderable>()
        for ((index, todo) in todos.withIndex()) {
            val renderable = todo.getRenderable() ?: continue
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

    fun parseItem(icon: String): ItemStack {
        if (icon.isEmpty()) return ItemStack(Items.PAINTING)
        return NeuInternalName.fromItemName(icon).getItemStack()
    }

}
