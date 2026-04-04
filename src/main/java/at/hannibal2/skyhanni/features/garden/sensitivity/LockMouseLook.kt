package at.hannibal2.skyhanni.features.garden.sensitivity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object LockMouseLook {
    /**
     * REGEX-TEST: Teleported you to Plot
     */
    private val gardenTeleportPattern by RepoPattern.pattern(
        "chat.garden.teleport.colorless",
        "Teleported you to .*",
    )

    private val config get() = SkyHanniMod.feature.misc
    private val isActive get() = MouseSensitivityManager.SensitivityState.LOCKED.isActive()
    private val lockedRenderable by lazy { Renderable.text("§eMouse Locked") }

    @HandleEvent
    fun onWorldChange() = unlockMouse()

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!gardenTeleportPattern.matches(event.chatComponent)) return
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

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isActive) return
        config.lockedMouseDisplay.renderRenderable(lockedRenderable, posLabel = "Mouse Locked")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shmouselock") {
            description = "Lock/Unlock the mouse so it will no longer rotate the player (for farming)"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shlockmouse")
            simpleCallback {
                if (isActive) unlockMouse() else lockMouse()
            }
        }
    }
}
