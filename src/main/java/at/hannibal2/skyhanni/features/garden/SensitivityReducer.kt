package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.SensitivityReducerConfig
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
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
object SensitivityReducer {

    private val config get() = GardenApi.config.sensitivityReducer

    private val commandMessageId = ChatUtils.getUniqueCustomMessageId()
    private val SQUEAKY_MOUSEMAT = "SQUEAKY_MOUSEMAT".toInternalName()

    private var state: SensitivityState = SensitivityState.UNCHANGED
    private var manualState: SensitivityState? = null
        set(value) {
            field = value
            onTick()
        }

    /**
     * REGEX-TEST: Teleported you to Plot - 1!
     * REGEX-TEST: Teleported you to Plot - 20!
     * REGEX-TEST: Teleported you to The Barn!
     * REGEX-TEST: Warping...
     */
    private val teleportPattern by RepoPattern.list(
        "garden.sensitivityreducer.chat.teleport.list",
        "Teleported you to Plot - (?<plot>.+)!",
        "Teleported you to (?<plot>The Barn)!",
        "(?<plot>Warping\\.\\.\\.)",
    )

    @JvmStatic
    fun remapSensitivity(original: Double): Double = state.transform(original)

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (config.unlockOnTeleport == SensitivityReducerConfig.UnlockOnTeleport.NEVER) return
        val state = manualState ?: return

        teleportPattern.matchMatchers(event.cleanMessage) {
            if (config.unlockOnTeleport.condition(group("plot"))) DelayedRun.runNextTick {
                manualState = null
                ChatUtils.chat(
                    if (state == SensitivityState.REDUCED) "Mouse sensitivity has been restored because you teleported."
                    else "Mouse rotation has been unlocked because you teleported.",
                    messageId = commandMessageId,
                )
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        manualState = null
    }

    @HandleEvent
    fun onTick() {
        val manualState = manualState
        state = when {
            manualState != null -> manualState
            !shouldAutoReduce() -> SensitivityState.UNCHANGED
            !config.lockMouse -> SensitivityState.REDUCED
            else -> SensitivityState.LOCKED
        }
    }

    private fun shouldAutoReduce(): Boolean {
        if (!config.enabled) return false

        if (!GardenApi.inGarden()) return false

        if (config.mode.none { it.isActive() }) return false

        if (config.onlyPlot && GardenApi.onUnfarmablePlot) return false

        if (config.onGround && !isOnGround()) return false

        return true
    }

    private fun isOnGround(): Boolean {
        if (PlayerUtils.onGround()) return true
        val tolerance = config.onGroundTolerance
        if (tolerance <= 0f || PlayerUtils.isFlying()) return false
        return PlayerUtils.getLocation().let { BlockUtils.raycast(it, it.down(tolerance))?.miss == false }
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
                        messageId = commandMessageId,
                    )
                } else {
                    manualState = null
                    ChatUtils.chat("Mouse sensitivity is now restored.", messageId = commandMessageId)
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
                    ChatUtils.chat("Mouse rotation is now locked. Type /shlockmouse to unlock your mouse.", messageId = commandMessageId)
                } else {
                    manualState = null
                    ChatUtils.chat("Mouse rotation is now unlocked.", messageId = commandMessageId)
                }
            }
        }
    }

    @HandleEvent
    fun onGuiRenderOverlay() {
        if (!config.showGui) return
        if (state == SensitivityState.UNCHANGED) return

        config.position.renderRenderable(
            Renderable.text("§e" + if (state == SensitivityState.REDUCED) "Sensitivity Lowered" else "Mouse Locked"),
            posLabel = "Sensitivity Reducer",
        )
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Sensitivity Reducer")

        if (state == SensitivityState.UNCHANGED) event.addIrrelevant {
            add("not enabled")
        } else event.addData {
            add("current state: $state")
            add("manual state: $manualState")
            add("reducing factor: " + if (state == SensitivityState.LOCKED) 0.0 else config.reducingPercent.fractionOf(100.0))
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val base = "garden.sensitivityReducer"
        event.move(80, "garden.sensitivityReducerConfig", base)
        event.move(81, "$base.showGUI", "$base.showGui")
        event.transform(116, "$base.mode") { element ->
            event.add(116, "$base.enabled") {
                JsonPrimitive(element.asString != "OFF")
            }
            val newList = JsonArray()
            when (element.asString) {
                "OFF" -> newList.add("TOOL")
                else -> newList.add(element.asString)
            }
            newList
        }
        event.move(136, "garden.mouseLock.unlockOnTeleport", "$base.unlockOnTeleport")
        event.move(136, "$base.reducingFactor", "$base.reducingPercent")
        event.transform(136, "$base.reducingPercent") {
            JsonPrimitive((1f.fractionOf(it.asFloat) * 100f).toFloat().roundTo(2))
        }
    }

    private enum class SensitivityState(val transform: (Double) -> Double) {
        UNCHANGED({ it }),
        REDUCED({ it * config.reducingPercent.fractionOf(100.0) }),
        LOCKED({ 0.0 }),
        ;
    }

    enum class Mode(private val displayName: String, val isActive: () -> Boolean) {
        KEYBIND("Holding Keybind", { config.keybind.isKeyHeld() && Minecraft.getInstance().screen == null }),
        TOOL("Farming tool", { GardenApi.toolInHand != null }),
        FISHING_ROD("Fishing Rod", { FishingApi.holdingRod }),
        MOUSEMAT("Squeaky Mousemat", { GardenApi.itemInHand?.getInternalName() == SQUEAKY_MOUSEMAT }),
        VACUUM("Vacuum", { PestApi.hasVacuumInHand() }),
        SPRAYONATOR("Sprayonator", { PestApi.hasSprayonatorInHand() }),
        ;

        override fun toString() = displayName
    }
}
