package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.item.ItemStack
import java.util.regex.Pattern

// TODO in the future add highlight support for the inventory
abstract class TaskHud<T : PlayerTask<O>, O>(
    val label: String,
    val tasks: Set<T>,
) {

    abstract val configToggle: Property<Boolean>
    abstract val position: Position

    /** Needs to be a "get()=" since it needs to account for the profile change*/
    abstract val storage: MutableSet<T>

    abstract val inventoryPattern: Pattern

    abstract fun resetTime(): SimpleTimeMark
    abstract fun createDisplay(data: Set<T>): Renderable

    abstract fun preItemFilter(slot: Int, stack: ItemStack): O?
    open fun chatFilter(msg: String): String? = msg

    protected open fun isEnabled(): Boolean = configToggle.get()

    private var display: Renderable = Renderable.placeholder(0, 0)
    private var resetIndex = 0
    private var displayDirty = true

    /** Should be called once at ProfileJoin or at HypixelJoin*/
    fun resetSchedule() {
        val i = resetIndex++
        DelayedRun.runDelayed(resetTime().timeUntil()) {
            if (i == resetIndex) {
                reset()
                resetSchedule()
            }
        }
    }

    fun reset() {
        displayDirty = true
        storage.clear()
        storage.addAll(tasks)
    }

    fun T.storageManipulation(switch: Boolean?) = when (switch) {
        true -> {
            storage.remove(this)
            displayDirty = true
        }

        false -> Unit
        else -> {
            storage.add(this)
            displayDirty = true
        }
    }

    // All HandleEvents do nothing, but at least to declare that those are events that get called
    @HandleEvent(onlyOnSkyblock = true)
    fun onInventory(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!inventoryPattern.matches(event.inventoryName)) return
        event.inventoryItems.mapNotNull { preItemFilter(it.key, it.value) }.forEach { input ->
            tasks.forEach {
                it.storageManipulation(it.isTaskDoneViaItem(input))
            }
        }
    }

    @HandleEvent()
    fun onSystemMessage(event: SystemMessageEvent) {
        if (!isEnabled()) return
        val pre = chatFilter(event.message) ?: return
        tasks.forEach {
            it.storageManipulation(it.checkChat(pre))
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRender(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (displayDirty) {
            display = createDisplay(storage)
            displayDirty = false
        }
        position.renderRenderable(display, posLabel = label)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        configToggle.onToggle {
            if (configToggle.get()) {
                displayDirty = true
            } else {
                display = Renderable.placeholder(0, 0)
            }
        }
    }

    init {
        instances.add(this)
    }

    @SkyHanniModule
    companion object {

        /** We need to call all the events from a module directly since we don't register Events for super types
         * Note: To fix the registration of super types we would only need
         * to change from .getDeclaredMethods to .methods in the init of [at.hannibal2.skyhanni.api.event.SkyHanniEvents] */
        private val instances = mutableListOf<TaskHud<*, *>>()

        @HandleEvent
        fun onConfigLoad(event: ConfigLoadEvent) = instances.forEach { it.onConfigLoad(event) }

        @HandleEvent(onlyOnSkyblock = true)
        fun onGuiRender(event: GuiRenderEvent) =
            instances.forEach { it.onGuiRender(event) }

        @HandleEvent()
        fun onSystemMessage(event: SystemMessageEvent) = instances.forEach { it.onSystemMessage(event) }

        @HandleEvent(onlyOnSkyblock = true)
        fun onInventory(event: InventoryFullyOpenedEvent) = instances.forEach { it.onInventory(event) }

    }
}

interface PlayerTask<T> {

    /** @return true=done, false=ignore, null=not done */
    fun checkChat(msg: String): Boolean?

    /** @return true=done, false=ignore, null=not done */
    fun isTaskDoneViaItem(input: T): Boolean?
}
