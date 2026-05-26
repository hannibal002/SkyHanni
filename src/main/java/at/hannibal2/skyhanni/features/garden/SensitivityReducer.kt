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
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
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

    private val commandMessageId = ChatUtils.getUniqueMessageId()
    private val SQUEAKY_MOUSEMAT = "SQUEAKY_MOUSEMAT".toInternalName()

    private var state: State = State.UNCHANGED
    private var manualState: State? = null
        set(value) {
            field = value
            onTick()
        }

    /**
     * REGEX-TEST: Teleported you to The Barn!
     * REGEX-TEST: Teleported you to Plot - 1!
     * REGEX-TEST: Teleported you to Plot - 20!
     * REGEX-TEST: Warping...
     */
    private val teleportPattern by RepoPattern.pattern(
        "garden.sensitivityreducer.chat.teleport-no-color",
        "Teleported you to .*!|Warping\\.\\.\\.",
    )

    @JvmStatic
    fun remapSensitivity(original: Double): Double = state.transform(original)

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.disableOnTeleport) return
        val state = manualState ?: return

        if (teleportPattern.matches(event.chatComponent)) {
            manualState = null
            ChatUtils.notifyOrDisable(
                if (state == State.REDUCED) "Mouse sensitivity has been restored because you teleported."
                    else "Mouse rotation has been unlocked because you teleported.",
                config::disableOnTeleport,
                messageId = commandMessageId,
            )
        }
    }

    @HandleEvent
    fun onWorldChange() {
        manualState = null
    }

    @HandleEvent
    fun onTick() {
        // TODO: drop the `State` prefix when context-sensitive resolution is enabled
        state = when {
            manualState != null -> manualState
            !shouldAutoReduce() -> State.UNCHANGED
            !config.lockMouse -> State.REDUCED
            else -> State.LOCKED
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

    private fun isOnGround() {
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
                if (manualState != State.REDUCED) {
                    manualState = State.REDUCED
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
                if (manualState != State.LOCKED) {
                    manualState = State.LOCKED
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
        if (state == State.UNCHANGED) return

        config.position.renderRenderable(
            Renderable.text("§e" + if (state == State.REDUCED)) "Sensitivity Lowered" else "Mouse Locked"),
            posLabel = "Sensitivity Reducer",
        )
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Sensitivity Reducer")

        if (state == State.UNCHANGED) event.addIrrelevant {
            add("not enabled")
        } else event.addData {
            add("current state: $state")
            add("manual state: $manualState")
            add("reducing factor: " + if (SensitivityReducer.LOCKED) 0.0 else config.reducingPercent.fractionOf(100.0))
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
        event.move(134, "$base.reducingFactor", "$base.reducingPercent")
        event.transform(134, "$base.reducingPercent") {
            JsonPrimitive((1f.fractionOf(it.asFloat) * 100f).toFloat().roundTo(2))
        }
    }

    private enum class State(val transform: (Double) -> Double) {
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
