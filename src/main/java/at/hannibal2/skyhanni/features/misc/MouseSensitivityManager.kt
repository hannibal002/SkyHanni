package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object MouseSensitivityManager {

    /**
     * REGEX-TEST: §aTeleported you to §r§aPlot
     */
    private val gardenTeleportPattern by RepoPattern.pattern(
        "chat.garden.teleport",
        "§aTeleported you to .*",
    )

    private val config get() = SkyHanniMod.feature.misc
    private val isActive get() = mouseSensitivityState == MouseSensitivityState.LOCKED

    var mouseSensitivityState = MouseSensitivityState.DEFAULT

    @HandleEvent
    fun onWorldChange() {
        unlockMouse()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!gardenTeleportPattern.matches(event.message)) return
        unlockMouse()
    }

    fun unlockMouse() {
        if (!isActive) return

        mouseSensitivityState = MouseSensitivityState.DEFAULT
        if (config.lockMouseLookChatMessage) {
            ChatUtils.chat("§bMouse rotation is now unlocked.")
        }
    }

    private fun lockMouse() {
        if (isActive) return

        mouseSensitivityState = MouseSensitivityState.LOCKED
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
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Mouse Sensitivity")

        if (mouseSensitivityState == MouseSensitivityState.DEFAULT) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("current state: $mouseSensitivityState")
        }
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

    enum class MouseSensitivityState(
        private val transform: ((Float) -> Float)
    ) {
        DEFAULT({ it }),
        LOCKED({_ -> -1f/3f}),
        REDUCED({ it }),
        ;

        fun apply(original: Float): Float = transform(original)
    }
}
