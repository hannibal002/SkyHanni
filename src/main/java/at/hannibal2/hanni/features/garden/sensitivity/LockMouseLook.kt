package at.hannibal2.hanni.features.garden.sensitivity

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object LockMouseLook {
    /**
     * REGEX-TEST: §aTeleported you to §r§aPlot
     */
    private val gardenTeleportPattern by RepoPattern.Companion.pattern(
        "chat.garden.teleport",
        "§aTeleported you to .*",
    )

    private val config get() = HanniMod.feature.misc
    private val isActive get() = MouseSensitivityManager.SensitivityState.LOCKED.isActive()

    @HandleEvent
    fun onWorldChange() {
        unlockMouse()
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!gardenTeleportPattern.matches(event.message)) return
        unlockMouse()
    }

    fun unlockMouse() {
        if (!isActive) return

        MouseSensitivityManager.state = MouseSensitivityManager.SensitivityState.UNCHANGED
        if (config.lockMouseLookChatMessage) {
            ChatUtils.chat("§bMouse rotation is now unlocked.")
        }
    }

    private fun lockMouse() {
        if (isActive) return

        MouseSensitivityManager.state = MouseSensitivityManager.SensitivityState.LOCKED
        if (config.lockMouseLookChatMessage) {
            ChatUtils.chat("§bMouse rotation is now locked.")
        }
    }

    private fun toggleLock() {
        if (isActive) {
            unlockMouse()
        } else {
            lockMouse()
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isActive) return
        config.lockedMouseDisplay.renderString("§eMouse Locked", posLabel = "Mouse Locked")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shmouselock") {
            description = "Lock/Unlock the mouse so it will no longer rotate the player (for farming)"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shlockmouse")
            callback { toggleLock() }
        }
    }
}
