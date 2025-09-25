package at.hannibal2.skyhanni.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.DanceRoomInstructionsJson
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.TitleReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.AxisAlignedBB
import kotlin.time.Duration

@SkyHanniModule
object DanceRoomHelper {

    private var display = emptyList<Renderable>()
    private val config get() = RiftApi.config.area.mirrorverse.danceRoomHelper
    private var index = 0
    private var foundNext = false
    private val danceRoom = AxisAlignedBB(-260.0, 32.0, -110.0, -267.0, 40.0, -102.0)
    private var inRoom = false
    private var instructions = emptyList<String>()
    private var countdown: String? = null

    private val emptyInstructionsDisplay by lazy {
        buildList {
            addString("§cError fetching Dance Room Instructions!")
            Renderable.optionalLink(
                "§cTry §e/shreloadlocalrepo §cor §e/shupdaterepo §c(Click to update now)",
                onLeftClick = { SkyHanniRepoManager.updateRepo("click on chat after dance doom error") },
            ).let { add(it) }
        }
    }

    fun update() {
        display = if (instructions.isEmpty()) emptyInstructionsDisplay
        else instructions.mapIndexed { lineIndex, line ->
            val formattedLine = line.split(" ").joinToString(" ") {
                it.firstLetterUppercase().addColor().formatColor()
            }
            getInstructionsLine(lineIndex, formattedLine)?.let { Renderable.text(it) }
        }.filterNotNull()
    }

    private fun getInstructionsLine(lineIndex: Int, formattedLine: String) = with(config.danceRoomFormatting) {
        when {
            index == lineIndex -> {
                val countdown = countdown?.let { "${color.countdown.formatColor()}$it" }.orEmpty()
                "${now.formatColor()} $formattedLine $countdown"
            }

            index + 1 == lineIndex -> "${next.formatColor()} $formattedLine"
            lineIndex in (index + 2..index + config.lineToShow) -> "${later.formatColor()} $formattedLine"
            else -> null
        }
    }

    private fun String.formatColor() = replace("&", "§")

    private fun String.addColor() = with(config.danceRoomFormatting.color) {
        when (this@addColor) {
            "Move" -> move
            "Stand" -> stand
            "Sneak" -> sneak
            "Jump" -> jump
            "Punch" -> punch
            else -> fallback
        } + this@addColor
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled || !inRoom) return
        config.position.renderRenderables(
            display,
            config.extraSpace,
            posLabel = "Dance Room Helper",
        )
    }

    @HandleEvent
    fun onWorldChange() {
        inRoom = false
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick(event: SkyHanniTickEvent) {
        // We want this to run even if not enabled, so that the Hide Other Players feature
        // properly updates without the helper being enabled
        if (event.isMod(10)) {
            inRoom = RiftApi.inMirrorVerse && danceRoom.isPlayerInside()
        }
        if (inRoom) update()
    }

    private fun PlaySoundEvent.isFailure() = (soundName == "random.burp" && volume == 0.8f) ||
        (soundName == "random.levelup" && pitch == 1.8412699f && volume == 1f)

    private fun PlaySoundEvent.isSuccess() = soundName == "note.bassattack" &&
        pitch == 0.6984127f && volume == 1f

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.enabled || !inRoom) return
        if (event.isFailure()) {
            index = 0
            foundNext = false
            countdown = null
            update()
        } else if (event.isSuccess() && !foundNext) {
            foundNext = true
            start(2000)
            update()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTitleReceived(event: TitleReceivedEvent) {
        if (!config.enabled) return
        if (config.hideOriginalTitle && inRoom) event.cancel()
    }

    private suspend fun startCountdown(seconds: Int, milliseconds: Int) {
        if (seconds <= 0 && milliseconds <= 0) {
            countdown = null
            return
        }

        val countdownString = "%01d:%03d".format(seconds, milliseconds)
        countdown = countdownString

        delay(1)
        var updatedSeconds = seconds
        var updatedMilliseconds = milliseconds - 1
        if (updatedMilliseconds < 0) {
            updatedSeconds -= 1
            updatedMilliseconds += 1000
        }
        startCountdown(updatedSeconds, updatedMilliseconds)
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityOtherPlayerMP>) {
        if (config.hidePlayers && inRoom && event.entity.isRealPlayer()) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        instructions = event.getConstant<DanceRoomInstructionsJson>("DanceRoomInstructions").instructions
    }

    // TODO maybe change to run delayed or tick based timer
    fun start(interval: Long) = SkyHanniMod.launchCoroutine("rift dance room helper", timeout = Duration.INFINITE) {
        while (isActive && foundNext) {
            index++
            startCountdown(0, 500)
            delay(interval)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.mirrorVerseConfig", "rift.area.mirrorverse")
    }
}
