package at.hannibal2.skyhanni.features.garden.sensitivity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.holdingRod
import at.hannibal2.skyhanni.features.garden.GardenApi
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
    private const val mousematMessage = "§aSnapped to squeaky mousemat!"

    private val config get() = SkyHanniMod.feature.garden.lockMouseConfig
    private val storage get() = SkyHanniMod.feature.storage
    private var commandUsed = false

    private val isActive get() = MouseSensitivityManager.SensitivityState.LOCKED.isActive()
    private val mc get() = Minecraft.getMinecraft()

    @HandleEvent
    fun onWorldChange() {
        commandUsed = false
        unlockMouse()
    }

    @HandleEvent
    fun onCommandRegister(event: CommandRegistrationEvent) {
        event.register("shmouselock") {
            description = "Lock/Unlock the mouse so it will no longer rotate the player (for farming)"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shlockmouse")
            callback {
                commandUsed = true
                toggleLock()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        if (gardenTeleportPattern.matches(event.message)) {
            commandUsed = false
            unlockMouse()
        }

        if (event.message == mousematMessage && config.lockAfterMousemat) {
            commandUsed = true
            lockMouse()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (commandUsed && isActive) return
        if (config.onlyGarden && !GardenApi.inGarden()) {
            if (isActive) toggleLock()
            return
        }
        if (config.onlyPlot && GardenApi.onBarnPlot) {
            if (isActive) toggleLock()
            return
        }
        if (config.onlyGround && !mc.thePlayer.onGround) {
            if (isActive) toggleLock()
            return
        }

        when {
            GardenApi.isHoldingTool() && config.lockWithTool && !holdingRod -> {
                if (!isActive) toggleLock()
                commandUsed = false
            }
            holdingRod && config.lockWithRod && !GardenApi.isHoldingTool() -> {
                if (!isActive) toggleLock()
                commandUsed = false
            }
            else -> {
                if (isActive) toggleLock()
                commandUsed = false
            }
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isActive) return
        config.lockedMouseDisplay.renderString("§eMouse Locked", posLabel = "Mouse Locked")
    }

    private fun toggleLock() {
        if (isActive) {
            unlockMouse()
        } else {
            lockMouse()
        }
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
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Mouse Lock")

        if (!isActive) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("Stored Sensitivity: ${storage.savedMouselockedSensitivity}")
        }
    }
}
