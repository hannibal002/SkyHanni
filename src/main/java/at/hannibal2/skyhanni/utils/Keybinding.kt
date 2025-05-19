package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.KeyEvent
import at.hannibal2.skyhanni.events.minecraft.KeyHeldEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.KeyReleaseEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Keybinding(
    // it would be an easy change to support modifieres etc. with this,
    // but it's not added as it's not frequently used,
    // an example where it would be useful is the garden plot borders
    val keyCodeProvider: () -> Int, // this may range from -100 to keyboard.KEYBOARD_SIZE
    val functionToExecute: () -> Unit,
    val cooldown: Duration = 2.seconds,
    val condition: (() -> Boolean)? = null,
    val instantCondition: (() -> Boolean)? = { Minecraft.getMinecraft().currentScreen == null && !NeuItems.neuHasFocus() },
    val onlyOnIsland: IslandType = IslandType.ANY,
    vararg val onlyOnIslands: IslandType = arrayOf(),
    val onEvent: KClass<out KeyEvent> = KeyPressEvent::class,
    val name: String? = null, // this is used for debugging and logging
) {
    private var keyCode: Int = keyCodeProvider()
        get() {
            val newValue = keyCodeProvider()
            if (newValue != field) {
                field = newValue
                updateActiveState()
            }
            return field
        }

    private var lastTimeActiveChecked: SimpleTimeMark = SimpleTimeMark.farPast()
    var active: Boolean = false
        get() {
            if (lastTimeActiveChecked.passedSince() > 10.seconds) {
                updateActiveState()
            }
            return field
        }
        private set

    private var lastTimeExecuted: SimpleTimeMark = SimpleTimeMark.farPast()

    init {
        addKeyBinding(this)
    }

    override fun toString(): String = buildString {
        append("Keybinding(")
        if (name != null) append("'$name', ")
        append(
            "keyCode=$keyCode, active=$active, lastTimeActiveChecked=$lastTimeActiveChecked, lastTimeExecuted=$lastTimeExecuted)"
        )
    }

    fun checkCondition() = condition?.invoke() ?: true
    fun checkInstantCondition() = instantCondition?.invoke() ?: true

    fun isActive() = active

    private fun checkIsActive(): Boolean {
        if (onlyOnIsland != IslandType.ANY && !onlyOnIsland.isCurrent()) return false
        if (onlyOnIslands.isNotEmpty() && !onlyOnIslands.any { it.isCurrent() }) return false
        return checkCondition()
    }

    fun updateActiveState() {
        active = checkIsActive()
        lastTimeActiveChecked = SimpleTimeMark.now()
    }

    fun execute() {
        lastTimeExecuted = SimpleTimeMark.now()
        functionToExecute()
    }

    private fun isOnCooldown(): Boolean = lastTimeExecuted.passedSince() < cooldown

    private fun onCorrectKeyEvent() {
        if (active && checkInstantCondition() && !isOnCooldown()) {
            execute()
        }
    }

    @SkyHanniModule
    companion object {
        fun List<Keybinding>.updateActiveStates() {
            forEach { it.updateActiveState() }
        }

        private val keybindings
            get() = keybindingsOnPress + keybindingsOnHeld + keybindingsOnRelease
        private val keybindingsOnPress = mutableSetOf<Keybinding>()
        private val keybindingsOnHeld = mutableSetOf<Keybinding>()
        private val keybindingsOnRelease = mutableSetOf<Keybinding>()

        private fun addKeyBinding(keybinding: Keybinding) {
            when (keybinding.onEvent) {
                KeyPressEvent::class -> keybindingsOnPress.add(keybinding)
                KeyHeldEvent::class -> keybindingsOnHeld.add(keybinding)
                KeyReleaseEvent::class -> keybindingsOnRelease.add(keybinding)
                else -> throw IllegalArgumentException("Invalid keybinding type: ${keybinding.onEvent}")
            }
            keybinding.updateActiveState()
        }

        @HandleEvent(eventTypes = [ConfigLoadEvent::class, WorldChangeEvent::class, IslandChangeEvent::class, GraphAreaChangeEvent::class])
        fun updateActiveStates() {
            keybindings.forEach { it.updateActiveState() }
        }

        @HandleEvent
        fun onKeyPress(event: KeyPressEvent) {
            keybindingsOnPress.forEach { keybinding ->
                if (keybinding.keyCode == event.keyCode) {
                    keybinding.onCorrectKeyEvent()
                }
            }
        }

        @HandleEvent
        fun onKeyHeld(event: KeyEvent) {
            keybindingsOnHeld.forEach { keybinding ->
                if (keybinding.keyCode == event.keyCode) {
                    keybinding.onCorrectKeyEvent()
                }
            }
        }

        @HandleEvent
        fun onKeyRelease(event: KeyReleaseEvent) {
            keybindingsOnRelease.forEach { keybinding ->
                if (keybinding.keyCode == event.keyCode) {
                    keybinding.onCorrectKeyEvent()
                }
            }
        }

        @HandleEvent
        fun onDebug(event: DebugDataCollectEvent) {
            val activeKeybindings = keybindings.filter { it.active }
            val inactiveKeybindings = keybindings.filter {
                !activeKeybindings.contains(it)
            }

            event.title("Keybindings")
            event.addIrrelevant {
                add("Total of ${keybindings.size} keybindings")
                if (activeKeybindings.isNotEmpty()) {
                    add("${activeKeybindings.size} active keybindings:")
                    activeKeybindings.forEach {
                        add(it.toString())
                    }
                }
                if (inactiveKeybindings.isNotEmpty()) {
                    add("${inactiveKeybindings.size} inactive keybindings:")
                    inactiveKeybindings.forEach {
                        add(it.toString())
                    }
                }
            }
        }

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.register("shreloadkeybindings") {
                description = "Reloads the active state of all keybindings"
                category = CommandCategory.USERS_BUG_FIX
                callback {
                    val oldActiveKeyBindings = keybindings.filter { keybinding -> keybinding.active }
                    updateActiveStates()
                    val newActiveKeyBindings = keybindings.filter { keybinding -> keybinding.active }
                    val removedKeyBindings = oldActiveKeyBindings.filter { keybinding ->
                        keybinding !in newActiveKeyBindings
                    }
                    val addedKeyBindings = newActiveKeyBindings.filter { keybinding ->
                        keybinding !in oldActiveKeyBindings
                    }
                    ChatUtils.debug("Removed $removedKeyBindings")
                    ChatUtils.debug("Added $addedKeyBindings")
                    ChatUtils.chat("Reloaded keybindings")
                }
            }
        }
    }
}
