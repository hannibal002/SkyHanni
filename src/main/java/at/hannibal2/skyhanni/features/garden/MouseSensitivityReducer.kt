package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.MouseSensitivityReducerConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft

@SkyHanniModule
object MouseSensitivityReducer {

    // shared /shmouselock and /shsensreduce because they modify the same state
    private val MESSAGE_ID = ChatUtils.getUniqueMessageId()

    val config get() = GardenApi.config.mouseSensitivityReducer

    private var activeState: SensitivityState = SensitivityState.UNCHANGED
    private var manualState: SensitivityState? = null

    /**
     * REGEX-TEST: Teleported you to Plot - 1!
     * REGEX-TEST: Teleported you to Plot - 20!
     * REGEX-TEST: Teleported you to The Barn!
     * REGEX-TEST: Warping...
     */
    private val teleportPattern by RepoPattern.list(
        "garden.mouse-sensitivity-reducer.chat.teleport.list",
        "Teleported you to Plot - (?<plot>.+)!",
        "Teleported you to (?<plot>The Barn)!",
        // TODO: decide what to do with /warp garden, this will not unlock mouse for now
        "(?<plot>Warping\\.\\.\\.)", // this is safe because plot names cannot have dots
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (manualState == null || config.unlockOnTeleport == MouseSensitivityReducerConfig.UnlockOnTeleport.NEVER) return

        teleportPattern.matchMatchers(event.cleanMessage) {
            if (config.unlockOnTeleport.condition(group("plot"))) {
                val oldState = manualState

                manualState = null
                update()

                if (config.chatMessage)
                    ChatUtils.notifyOrDisable(
                        if (oldState == SensitivityState.REDUCED) "Mouse sensitivity has been restored because you teleported."
                        else "Mouse rotation has been unlocked because you teleported.",
                        config::unlockOnTeleport,
                        messageId = MESSAGE_ID,
                    )
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        manualState = null
        update()
    }

    @HandleEvent
    fun onTick() {
        update()
    }

    private fun update() {
        activeState = manualState ?: when {
            !isAutoEnabled() -> SensitivityState.UNCHANGED
            !config.lockMouse -> SensitivityState.REDUCED
            else -> SensitivityState.LOCKED
        }
    }

    private fun isAutoEnabled(): Boolean =
        GardenApi.inGarden() &&
            config.autoEnable &&
            config.autoEnableMode.any { it.condition() } &&
            !(config.onlyPlot && GardenApi.onUnfarmablePlot) &&
            !(config.onGround && !isOnGround())

    private fun isOnGround(): Boolean {
        if (PlayerUtils.onGround()) return true
        val tolerance = config.onGroundTolerance.takeUnless { it == 0f } ?: return false
        if (!PlayerUtils.hasNormalMovement()) return false

        return playerLocation().let { !BlockUtils.raycast(it, it.down(tolerance)).miss }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shsensreduce") {
            description = "Lowers the mouse sensitivity for easier small adjustments (for farming)"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                if (manualState != SensitivityState.REDUCED) {
                    manualState = SensitivityState.REDUCED
                    ChatUtils.chat(
                        "Mouse sensitivity is now lowered. Type /shsensreduce to restore your sensitivity.",
                        messageId = MESSAGE_ID,
                    )
                } else {
                    manualState = null
                    ChatUtils.chat("Mouse sensitivity is now restored.", messageId = MESSAGE_ID)
                }
            }
        }
        event.registerBrigadier("shmouselock") {
            description = "Lock/Unlock the mouse so it will no longer rotate the player (for farming)"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shlockmouse")
            simpleCallback {
                if (manualState != SensitivityState.LOCKED) {
                    manualState = SensitivityState.LOCKED
                    ChatUtils.chat(
                        "Mouse rotation is now locked. Type /shmouselock to unlock your mouse.",
                        messageId = MESSAGE_ID,
                    )
                } else {
                    manualState = null
                    ChatUtils.chat("Mouse rotation is now unlocked.", messageId = MESSAGE_ID)
                }
            }
        }
    }

    @HandleEvent
    fun onGuiRenderOverlay() {
        if (config.showGui) config.position.renderRenderable(
            when (activeState) {
                SensitivityState.UNCHANGED -> return
                SensitivityState.REDUCED -> Renderable.text("§eSensitivity Lowered")
                SensitivityState.LOCKED -> Renderable.text("§eMouse Locked")
            },
            posLabel = "Mouse Sensitivity Reducer",
        )
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mouse Sensitivity Reducer")

        if (activeState == SensitivityState.UNCHANGED) event.addIrrelevant {
            add("not enabled")
        }
        else event.addData {
            add("current state: $activeState")
            add("manual state: $manualState")
            add("sensitivity factor: " + remapSensitivity(1.0))
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val oldBase = "garden.sensitivityReducer"
        val base = "garden.mouseSensitivityReducer"
        event.move(80, "garden.sensitivityReducerConfig", oldBase)
        event.move(81, "$oldBase.showGUI", "$oldBase.showGui")
        event.transform(116, "$oldBase.mode") { element ->
            event.add(116, "$oldBase.enabled") {
                JsonPrimitive(element.asString != "OFF")
            }
            val newList = JsonArray()
            when (element.asString) {
                "OFF" -> newList.add("TOOL")
                else -> newList.add(element.asString)
            }
            newList
        }
        // old migrations from separate mouse lock feature
        event.move(135, "misc.lockMouseLookChatMessage", "garden.mouseLock.chatMessage")
        event.move(135, "misc.lockedMouseDisplay", "garden.mouseLock.display")
        // variable renames
        event.move(137, oldBase, base)
        event.move(137, "$base.enabled", "$base.autoEnable")
        event.move(137, "$base.mode", "$base.autoEnableMode")
        // convert old factor to new percent
        event.move(137, "$base.reducingFactor", "$base.reducingPercent") {
            JsonPrimitive((1f.fractionOf(it.asFloat) * 100f).toFloat().roundTo(2))
        }
        // move mouse lock settings to new config
        event.move(137, "garden.mouseLock.chatMessage", "$base.chatMessage")
        event.move(137, "garden.mouseLock.unlockOnTeleport", "$base.unlockOnTeleport")
    }

    @JvmStatic
    fun remapSensitivity(original: Double): Double = activeState.transform(original)

    private enum class SensitivityState(val transform: (Double) -> Double) {
        UNCHANGED({ it }),
        REDUCED({ it * config.reducingPercent.fractionOf(100.0) }),
        LOCKED({ 0.0 }),
    }

    enum class AutoEnableMode(private val displayName: String, val condition: () -> Boolean) {
        KEYBIND("Holding Keybind", { config.keybind.isKeyHeld() && Minecraft.getInstance().screen == null }),
        TOOL("Farming tool", { GardenApi.hasFarmingToolInHand() }),
        FISHING_ROD("Fishing Rod", { FishingApi.holdingRod }),
        MOUSEMAT("Squeaky Mousemat", { GardenApi.hasMousematInHand() }),
        VACUUM("Vacuum", { PestApi.hasVacuumInHand() }),
        SPRAYONATOR("Sprayonator", { PestApi.hasSprayonatorInHand() }),
        ;

        override fun toString() = displayName
    }
}
