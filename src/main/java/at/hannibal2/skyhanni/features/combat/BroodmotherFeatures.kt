package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

// TODO clean this file up - there is a lot of state management that could utilize Resettable
//  and other mechanisms to avoid the repetitive code.
@SkyHanniModule
object BroodmotherFeatures {
    enum class StageEntry(private val str: String, val duration: Duration) {
        SLAIN("§eSlain", 2.5.minutes),
        DORMANT("§eDormant", 2.minutes),
        SOON("§6Soon", 1.5.minutes),
        AWAKENING("§6Awakening", 1.minutes),
        IMMINENT("§4Imminent", 0.5.minutes),
        ALIVE("§4Alive!", 0.minutes);

        override fun toString() = str
    }

    private val config get() = SkyHanniMod.feature.combat.broodmother
    private val spawnAlertConfig get() = config.spawnAlert

    private val alertSound get() =
        SoundUtils.createSound(spawnAlertConfig.alertSound, spawnAlertConfig.pitch, isWarning = true)

    @JvmStatic
    fun playTestSound() = alertSound.playSound()

    private var lastStage: StageEntry? = null
    private var currentStage: StageEntry? = null
    private var broodmotherSpawnTime = SimpleTimeMark.farPast()
    private var display: Renderable? = null

    @HandleEvent
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(BROODMOTHER)) return
        val newStage = event.widget.matchMatcherFirstLine { group("stage") }.orEmpty()
        if (newStage.isNotEmpty() && newStage != lastStage.toString().removeColor()) {
            lastStage = currentStage
            currentStage = StageEntry.valueOf(newStage.replace("!", "").uppercase())
            onStageUpdate()
            if (lastStage == null) lastStage = currentStage
        }
    }

    private fun onStageUpdate() {
        ChatUtils.debug("New Broodmother stage: $currentStage")

        if (onServerJoin()) return

        // ignore Hypixel bug where the stage may temporarily revert to Imminent after the Broodmother's death
        if (currentStage == IMMINENT && lastStage == ALIVE) return

        if (currentStage == ALIVE) {
            onBroodmotherSpawn()
            return
        }

        val timeUntilSpawn = currentStage?.duration ?: return
        broodmotherSpawnTime = SimpleTimeMark.now() + timeUntilSpawn

        if (currentStage == IMMINENT) {
            playImminentWarning()
        } else if (currentStage == SLAIN) {
            onBroodmotherSlain()
        }
    }

    private fun onServerJoin(): Boolean {
        if (lastStage != null || !config.stageOnJoin) return false
        // don't send if user has the spawn alert enabled
        // this is so that two messages aren't immediately sent upon joining a server
        if (!(currentStage == ALIVE && isSpawnAlertEnabled())) {
            var message = "The Broodmother's current stage in this server is ${currentStage.toString().replace("!", "")}§e."

            val duration = currentStage?.duration
            if (duration != 0.minutes) {
                message += " It will spawn §bwithin $duration§e."
            }

            ChatUtils.chat(message)
            return true
        }
        return false
    }

    private fun onBroodmotherSpawn() {
        broodmotherSpawnTime = SimpleTimeMark.farPast()

        if (!isSpawnAlertEnabled()) return

        SoundUtils.repeatSound(100, spawnAlertConfig.repeatSound, alertSound)
        TitleManager.sendTitle(spawnAlertConfig.text.replace("&", "§"))

        ChatUtils.clickToActionOrDisable(
            "The Broodmother has spawned!",
            config::alertOnSpawn,
            actionName = "warp to the Top of the Nest",
            action = { HypixelCommands.warp("nest") },
        )
    }

    private fun playImminentWarning() {
        if (!config.imminentWarning) return

        SoundUtils.repeatSound(100, 2, SoundUtils.createSound("block.note_block.pling", 0.5f, isWarning = true))
        ChatUtils.chat(
            componentBuilder {
                append("The Broodmother is ")
                appendWithColor("Imminent", ChatFormatting.DARK_RED)
                append("! It will spawn in ")
                appendWithColor("30 seconds", ChatFormatting.AQUA)
                append("!")
            },
        )
    }

    private fun onBroodmotherSlain() {
        broodmotherSpawnTime = SimpleTimeMark.now() + 2.5.minutes

        if (!config.slainMessage) return
        if (config.hideSlainWhenNearby && SpidersDenApi.isAtTopOfNest()) return

        ChatUtils.chat("The Broodmother was killed!")
    }

    @HandleEvent
    private fun onWorldChange() {
        broodmotherSpawnTime = SimpleTimeMark.farPast()
        lastStage = null
        currentStage = null
        display = null
    }

    @HandleEvent
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isCountdownEnabled()) return
        val display = if (broodmotherSpawnTime.isInPast() && !broodmotherSpawnTime.isFarPast()) {
            Renderable.text("§4Broodmother spawning now!")
        } else display ?: return
        config.countdownPosition.renderRenderable(display, posLabel = "Broodmother Countdown")
    }

    @HandleEvent
    private fun onSecondPassed(event: SecondPassedEvent) {
        if (!isCountdownEnabled()) return

        if (broodmotherSpawnTime.isFarPast()) {
            if (currentStage == ALIVE) {
                display = Renderable.text("§4Broodmother spawned!")
            }
        } else {
            val countdown = broodmotherSpawnTime.timeUntil().format()
            display = Renderable.text("§4Broodmother spawning in §b$countdown")
        }
    }

    private fun inSpidersDen() = IslandType.SPIDER_DEN.isInIsland()
    private fun isCountdownEnabled() = inSpidersDen() && config.countdown
    private fun isSpawnAlertEnabled() = config.alertOnSpawn
}
