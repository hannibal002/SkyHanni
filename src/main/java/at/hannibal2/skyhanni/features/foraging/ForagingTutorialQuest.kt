package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ForagingTutorialQuest {
    private val config get() = SkyHanniMod.feature.foraging.tutorialQuest

    private var lastParkWarpAttempt = SimpleTimeMark.farPast()
    private var lastSuggestion = SimpleTimeMark.farPast()

    @HandleEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        val message = event.message
        if (message.lowercase() == "/warp park") {
            lastParkWarpAttempt = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    fun onChat(event: SystemMessageEvent) {
        if (event.message == "§cYou don't have the requirements to use this warp!") {
            if (lastParkWarpAttempt.passedSince() < 1.seconds) {
                firstStep()
            }
        }
        if (IslandType.HUB.isCurrent()) {
            if (event.message == "§cYou must complete the §r§6Foraging Tutorial Quest §r§cto use this!") {
                firstStep()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onPlayerSpawn(event: MobEvent.Spawn.DisplayNpc) {
        if (event.mob.name == "§cRequires §6Foraging Tutorial Quest") {
            firstStep()
        }
    }

    private fun firstStep() {
        if (!config.enabled) {
            suggest()
            return
        }
        EntityMovementData.onNextTeleport(IslandType.HUB) {
            ChatUtils.chat("Go to Lumber Jack and start the Foraging Quest!")
            IslandGraphs.pathFind(
                LorenzVec(-221.2, 73.0, -14.9),
                "Lumber Jack",
                condition = { config.enabled },
            )
        }
    }

    private fun suggest() {
        if (!config.suggestToEnable) return
        if (lastSuggestion.passedSince() < 10.minutes) return

        lastSuggestion = SimpleTimeMark.now()
        ChatUtils.clickableChat(
            "Do you want to have help solving the Foraging Tutorial Quest? Click here!",
            onClick = {
                config.enabled = true
                firstStep()
            },
        )
        ChatUtils.clickableChat(
            "Never see this suggestion again? Click heere!",
            onClick = {
                ChatUtils.chat("Disabled Foraging Tutorial Quest Suggestions.")
                config.suggestToEnable = false
            },
        )
    }

    fun isEnalbed() = config.enabled
}
