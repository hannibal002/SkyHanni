package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.holdingRod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft

@SkyHanniModule
object LockMouseLook {

    /**
     * REGEX-TEST: §aTeleported you to §r§aPlot
     */
    private val gardenTeleportPattern by RepoPattern.pattern(
        "chat.garden.teleport",
        "§aTeleported you to .*",
    )
    const val mousematMessage = "§aSnapped to squeaky mousemat!"

    private val config get() = SkyHanniMod.feature.garden.lockMouseConfig
    private val storage get() = SkyHanniMod.feature.storage
    var lockedMouse = false
    private var commandUsed = false
    private const val lockedPosition = -1F / 3F

    private val mc get() = Minecraft.getMinecraft()

    @HandleEvent
    fun onWorldChange() {
        commandUsed = false
        if (lockedMouse) toggleLock()
        val gameSettings = Minecraft.getMinecraft().gameSettings
        if (gameSettings.mouseSensitivity == lockedPosition) {
            gameSettings.mouseSensitivity = storage.savedMouselockedSensitivity
            ChatUtils.chat("§bMouse rotation is now unlocked because you left it locked.")
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        if (gardenTeleportPattern.matches(event.message)) {
            commandUsed = false
            if (lockedMouse) toggleLock()
        }

        if (event.message == mousematMessage && config.lockAfterMousemat) {
            commandUsed = true
            if (!lockedMouse) toggleLock()
        }
    }

    @HandleEvent
    fun onCommandRegister(event: CommandRegistrationEvent) {
        event.register("shmouselock") {
            description = "Lock/Unlock the mouse so it will no longer rotate the player (for farming)"
            aliases = listOf("shlockmouse")
            callback {
                commandUsed = true
                toggleLock()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (commandUsed && lockedMouse) return
        if (config.onlyGarden && !GardenApi.inGarden()) {
            if (lockedMouse) toggleLock()
            return
        }
        if (config.onlyPlot && GardenApi.onBarnPlot) {
            if (lockedMouse) toggleLock()
            return
        }
        if (config.onlyGround && !mc.thePlayer.onGround) {
            if (lockedMouse) toggleLock()
            return
        }

        when {
            GardenApi.isHoldingTool() && config.lockWithTool && !holdingRod -> {
                if (!lockedMouse) toggleLock()
                commandUsed = false
            }
            holdingRod && config.lockWithRod && !GardenApi.isHoldingTool() -> {
                if (!lockedMouse) toggleLock()
                commandUsed = false
            }
            else -> {
                if (lockedMouse) toggleLock()
                commandUsed = false
            }
        }
    }

    fun toggleLock() {
        lockedMouse = !lockedMouse

        val gameSettings = Minecraft.getMinecraft().gameSettings

        if (lockedMouse) {
            var mouseSensitivity = gameSettings.mouseSensitivity
            if (SensitivityReducer.isEnabled()) mouseSensitivity = SensitivityReducer.doTheMath(mouseSensitivity, true)

            storage.savedMouselockedSensitivity = mouseSensitivity
            gameSettings.mouseSensitivity = lockedPosition
            if (config.lockMouseLookChatMessage) {
                if (!commandUsed) return
                ChatUtils.chat("§bMouse rotation is now locked. Type /shmouselock to unlock your rotation")
            }
        } else {
            if (!SensitivityReducer.isEnabled()) gameSettings.mouseSensitivity = storage.savedMouselockedSensitivity
            else gameSettings.mouseSensitivity = SensitivityReducer.doTheMath(storage.savedMouselockedSensitivity)
            if (config.lockMouseLookChatMessage) {
                if (!commandUsed) return
                ChatUtils.chat("§bMouse rotation is now unlocked.")
            }
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!lockedMouse) return
        config.lockedMouseDisplay.renderString("§eMouse Locked", posLabel = "Mouse Locked")
    }

    fun autoDisable() {
        if (lockedMouse) {
            toggleLock()
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Mouse Lock")

        if (!lockedMouse) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("Stored Sensitivity: ${storage.savedMouselockedSensitivity}")
        }
    }
}
