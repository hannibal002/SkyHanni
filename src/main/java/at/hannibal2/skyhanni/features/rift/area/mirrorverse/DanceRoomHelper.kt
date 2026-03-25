package at.hannibal2.skyhanni.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.data.jsonobjects.repo.DanceRoomInstructionsJson
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.TitleReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.phys.AABB

@SkyHanniModule
object DanceRoomHelper {

    private val config get() = RiftApi.config.area.mirrorverse.danceRoomHelper
    private val danceRoom = AABB(-260.0, 32.0, -110.0, -267.0, 40.0, -102.0)

    private var display = emptyList<Renderable>()
    private var index = 0
    private var inRoom = false
    private var instructions = emptyList<String>()
    private var countdownTicks = 0

    private val countdownStr: String
        get() = if (countdownTicks <= 0) "" else {
            val totalMilliseconds = countdownTicks * 50
            "%s%01d:%03d".format(
                config.danceRoomFormatting.color.countdown.formatColor(),
                totalMilliseconds / 1000,
                totalMilliseconds % 1000,
            )
        }

    private val emptyInstructionsDisplay by lazy {
        buildList {
            // TODO: add generic repo outdated error logic here
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
        when (lineIndex) {
            index -> "${now.formatColor()} $formattedLine $countdownStr"
            index + 1 -> "${next.formatColor()} $formattedLine"
            in (index + 2..index + config.lineToShow) -> "${later.formatColor()} $formattedLine"
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

    @HandleEvent
    fun onGuiRenderOverlay() {
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
        index = 0
        countdownTicks = 0
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick(event: SkyHanniTickEvent) {
        // We want this to run even if not enabled, so that the Hide Other Players feature
        // properly updates without the helper being enabled
        if (event.isMod(10)) {
            inRoom = RiftApi.inMirrorVerse && danceRoom.isPlayerInside()
        }

        if (inRoom) {
            update()
        } else {
            index = 0
            countdownTicks = 0
        }
    }

    private fun PlaySoundEvent.isSuccess() =
        soundName == "block.note_block.bass" && pitch.equalsOneOf(0.6984127f, 0.52380955f) && volume == 1f

    private fun PlaySoundEvent.isFailure() =
        (soundName == "entity.player.burp" && volume == 0.8f) ||
            (soundName == "entity.player.levelup" && pitch == 1.8412699f && volume == 1f)

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.enabled || !inRoom) return

        when {
            event.isSuccess() -> {
                ChatUtils.debug("DanceRoomHelper: Got success sound (t=${MinecraftData.totalServerTicks})")
                index++
                countdownTicks = 20
                update()
            }
            event.isFailure() && (index > 0 || countdownTicks > 0) -> {
                ChatUtils.debug("DanceRoomHelper: Got failure sound (t=${MinecraftData.totalServerTicks})")
                index = 0
                countdownTicks = 0
                update()
            }
        }
    }

    @HandleEvent
    fun onTitleReceived(event: TitleReceivedEvent) {
        if (!config.enabled || !inRoom) return
        if (config.hideOriginalTitle) event.cancel()
    }

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<RemotePlayer>) {
        if (!inRoom) return
        if (config.hidePlayers && event.entity.isRealPlayer()) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        instructions = event.getConstant<DanceRoomInstructionsJson>("DanceRoomInstructions").instructions
    }

    @HandleEvent
    fun onServerTick() {
        if (!inRoom) return

        if (countdownTicks > 0) countdownTicks--
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.mirrorVerseConfig", "rift.area.mirrorverse")
    }
}
