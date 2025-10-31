package at.hannibal2.hanni.features.combat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.WidgetUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.SoundUtils.playSound
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.TimeUtils.format
import kotlin.reflect.KProperty0
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

@HanniModule
object BroodmotherFeatures {

    enum class StageEntry(private val str: String, val duration: Duration) {
        SLAIN("§eSlain", 10.minutes),
        DORMANT("§eDormant", 9.minutes),
        SOON("§6Soon", 6.minutes),
        AWAKENING("§6Awakening", 3.minutes),
        IMMINENT("§4Imminent", 1.minutes),
        ALIVE("§4Alive!", 0.minutes);

        override fun toString() = str
    }

    private val config get() = HanniMod.feature.combat.broodmother
    private val spawnAlertConfig get() = config.spawnAlert

    private var lastStage: StageEntry? = null
    private var currentStage: StageEntry? = null
    private var broodmotherSpawnTime = SimpleTimeMark.farPast()
    private var display = ""

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.BROODMOTHER)) return
        val newStage = event.widget.matchMatcherFirstLine { group("stage") }.orEmpty()
        if (newStage.isNotEmpty() && newStage != lastStage.toString()) {
            lastStage = currentStage
            currentStage = StageEntry.valueOf(newStage.replace("!", "").uppercase())
            onStageUpdate()
        }
    }

    private fun onStageUpdate() {
        ChatUtils.debug("New Broodmother stage: $currentStage")

        if (onServerJoin()) return

        // ignore Hypixel bug where the stage may temporarily revert to Imminent after the Broodmother's death
        if (currentStage == StageEntry.IMMINENT && lastStage == StageEntry.ALIVE) return

        if (currentStage == StageEntry.ALIVE) {
            onBroodmotherSpawn()
            return
        }

        val lastStage = lastStage ?: return
        val timeUntilSpawn = currentStage?.duration ?: return
        broodmotherSpawnTime = SimpleTimeMark.now() + timeUntilSpawn

        if (currentStage == StageEntry.IMMINENT && config.imminentWarning) {
            playImminentWarning()
            return
        }

        if (currentStage !in config.stages) return
        if (currentStage == StageEntry.SLAIN) {
            onBroodmotherSlain()
        } else {
            val pluralize = StringUtils.pluralize(timeUntilSpawn.toInt(DurationUnit.MINUTES), "minute")
            ChatUtils.chat(
                "Broodmother: $lastStage §e-> $currentStage§e. §b${timeUntilSpawn.inWholeMinutes} $pluralize §euntil it spawns!",
            )
        }
    }

    private fun onServerJoin(): Boolean {
        if (lastStage != null || !config.stageOnJoin) return false
        // don't send if user has config enabled for either of the alive messages
        // this is so that two messages aren't immediately sent upon joining a server
        if (!(currentStage == StageEntry.ALIVE && isAliveMessageEnabled())) {
            val duration = currentStage?.duration
            var message = "The Broodmother's current stage in this server is ${currentStage.toString().replace("!", "")}§e."
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
        if (!isAliveMessageEnabled()) return
        val feature: KProperty0<*>
        if (config.alertOnSpawn) {
            feature = config::alertOnSpawn
            val alertSound = SoundUtils.createSound(spawnAlertConfig.alertSound, spawnAlertConfig.pitch)
            SoundUtils.repeatSound(100, spawnAlertConfig.repeatSound, alertSound)
            TitleManager.sendTitle(spawnAlertConfig.text.replace("&", "§"))
        } else {
            feature = config::stages
        }
        ChatUtils.clickToActionOrDisable(
            "The Broodmother has spawned!",
            feature,
            actionName = "warp to the Top of the Nest",
            action = { HypixelCommands.warp("nest") },
        )
    }

    private fun playImminentWarning() {
        SoundUtils.repeatSound(100, 2, SoundUtils.createSound("note.pling", 0.5f))
        ChatUtils.chat("The Broodmother is §4Imminent§e! It will spawn in §b60 seconds§e!")
    }

    private fun onBroodmotherSlain() {
        broodmotherSpawnTime = SimpleTimeMark.now() + 10.minutes
        if (!(config.hideSlainWhenNearby && SpidersDenApi.isAtTopOfNest())) {
            ChatUtils.chat("The Broodmother was killed!")
        }
    }

    @HandleEvent
    fun onWorldChange() {
        broodmotherSpawnTime = SimpleTimeMark.farPast()
        lastStage = null
        currentStage = null
        display = ""
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isCountdownEnabled()) return
        if (display.isEmpty()) return
        if (broodmotherSpawnTime.isInPast() && !broodmotherSpawnTime.isFarPast()) {
            display = "§4Broodmother spawning now!"
        }

        config.countdownPosition.renderString(display, posLabel = "Broodmother Countdown")
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isCountdownEnabled()) return

        if (broodmotherSpawnTime.isFarPast()) {
            if (currentStage == StageEntry.ALIVE) {
                display = "§4Broodmother spawned!"
            }
        } else {
            val countdown = broodmotherSpawnTime.timeUntil().format()
            display = "§4Broodmother spawning in §b$countdown"
        }
    }

    @JvmStatic
    fun playTestSound() {
        with(spawnAlertConfig) {
            SoundUtils.createSound(alertSound, pitch).playSound()
        }
    }

    private fun inSpidersDen() = IslandType.SPIDER_DEN.isCurrent()
    private fun isCountdownEnabled() = inSpidersDen() && config.countdown
    private fun isAliveMessageEnabled() = config.alertOnSpawn || config.stages.contains(StageEntry.ALIVE)
}
