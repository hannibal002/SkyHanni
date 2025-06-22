package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.mining.nucleus.PowderChestTimerConfig
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.block.BlockChest
import net.minecraft.block.state.IBlockState
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PowderChestTimer {

    private val config get() = SkyHanniMod.feature.mining.powderChestTimer

    private var display: String? = null
    private val chests = TimeLimitedCache<LorenzVec, SimpleTimeMark>(61.seconds)
    private val maxDuration = 60.seconds
    private const val MAX_CHEST_DISTANCE = 15
    private const val NEAR_PLAYER_DISTANCE = 25
    private var lastSound = SimpleTimeMark.farPast()

    private val arePlayersNearby by RecalculatingValue(5.seconds) {
        EntityUtils.getPlayerEntities().any { it.distanceToPlayer() < NEAR_PLAYER_DISTANCE }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onPlaySound(event: PlaySoundEvent) {
        if (event.soundName == "random.levelup" && event.pitch == 1f && event.volume == 1.0f) {
            lastSound = SimpleTimeMark.now()
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onRenderOverlay() {
        if (isEnabled()) {
            config.position.renderString(display, posLabel = "Powder Chest Timer")
        }
    }

    @HandleEvent
    fun onWorldChange() = chests.clear()

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onServerBlockChange(event: ServerBlockChangeEvent) {
        val location = event.location
        if (location.distanceToPlayer() > MAX_CHEST_DISTANCE) return
        val isNewChest = event.newState.isChest()
        val isOldChest = event.oldState.isChest()

        if (isNewChest && !isOldChest) {
            if (arePlayersNearby && lastSound.passedSince() > 200.milliseconds) return
            chests[location] = maxDuration.fromNow()
        } else if (isOldChest && !isNewChest) {
            chests.remove(location)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        val location = event.position
        if (!location.getBlockStateAt().isChest()) return

        if (HotmData.GREAT_EXPLORER.activeLevel < 20) return

        if (location.isOpened()) return
        if (event.clickType == ClickType.RIGHT_CLICK) {
            chests.remove(location)
            return
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onTick() {
        if (isEnabled()) {
            display = drawDisplay()
        }
    }

    private fun drawDisplay(): String? {
        val chests = chests.takeIf { it.isNotEmpty() }?.toMap() ?: return null

        val count = chests.size
        val name = StringUtils.pluralize(count, "chest")
        val first = chests.values.minByOrNull { it.timeUntil() } ?: return null

        val timeUntil = first.timeUntil()
        val color = timeUntil.getColorBasedOnTime().toChatColor()

        return "$color${timeUntil.format(TimeUnit.SECOND)} §8(§e$count §b$name§8)"
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        val chests = chests.takeIf { it.isNotEmpty() }?.toMap() ?: return

        event.renderChests(chests)

        val chestToConnect = sortChests(chests)
        if (chestToConnect.isEmpty()) return

        event.drawFirstLine(chestToConnect)
        event.drawRemainingLines(chestToConnect)
    }

    private fun SkyHanniRenderWorldEvent.drawFirstLine(list: List<Map.Entry<LorenzVec, SimpleTimeMark>>) {
        val (firstPos, firstTime) = list.first()

        drawLineToEye(
            firstPos.blockCenter(),
            firstTime.timeUntil().getColorBasedOnTime(),
            lineWidth = 3,
            depth = true,
        )
    }

    private fun SkyHanniRenderWorldEvent.drawRemainingLines(list: List<Map.Entry<LorenzVec, SimpleTimeMark>>) {
        for ((first, second) in list.zipWithNext()) {
            val (current, currentTime) = first
            val (next, _) = second

            val color = currentTime.timeUntil().getColorBasedOnTime()
            draw3DLine(current.blockCenter(), next.blockCenter(), color, 3, true)
        }
    }

    private fun sortChests(chests: Map<LorenzVec, SimpleTimeMark>): List<Map.Entry<LorenzVec, SimpleTimeMark>> {
        val sortedChests = when (config.lineMode) {
            PowderChestTimerConfig.LineMode.OLDEST -> chests.entries.sortedBy { it.value.timeUntil() }
            PowderChestTimerConfig.LineMode.NEAREST -> chests.entries.sortedBy { it.key.distanceToPlayer() }
            else -> return emptyList()
        }

        return sortedChests.take(config.drawLineToChestAmount)
    }

    private fun SkyHanniRenderWorldEvent.renderChests(chests: Map<LorenzVec, SimpleTimeMark>) {
        val playerY = LocationUtils.playerLocation().y
        for ((loc, time) in chests) {
            val timeLeft = time.timeUntil()

            if (config.highlightChests) {
                val color = if (config.useStaticColor) config.staticColor.toColor()
                else timeLeft.getColorBasedOnTime()
                drawWaypointFilled(loc, color)
            }

            if (config.drawTimerOnChest) {
                val yOffset = if (loc.y <= playerY) 1.25 else -0.25
                val textPos = loc.add(x = 0.5, y = yOffset, z = 0.5)
                drawString(textPos, timeLeft.format(TimeUnit.SECOND))
            }
        }
    }

    private fun Color.toChatColor(): String {
        return when {
            red in 0..127 && green in 127..255 -> "§a"
            red in 127..212 && green in 42..127 -> "§6"
            red in 212..230 && green in 25..42 -> "§c"
            red in 230..255 && green in 0..25 -> "§4"
            else -> "§f"
        }
    }

    private fun Duration.getColorBasedOnTime(): Color {
        val ratio = (this / maxDuration).coerceIn(0.0, 1.0)

        val red = (255 * (1 - ratio)).toInt()
        val green = (255 * ratio).toInt()

        return Color(red, green, 0)
    }

    private fun LorenzVec.isOpened() = !chests.containsKey(this)

    private fun IBlockState.isChest() = block is BlockChest

    private fun isEnabled() = config.enabled
}
