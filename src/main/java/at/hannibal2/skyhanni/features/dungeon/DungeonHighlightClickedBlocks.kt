package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonHighlightClickedBlocks {

    private val config get() = SkyHanniMod.feature.dungeon.clickedBlocks
    private val blocks = TimeLimitedCache<LorenzVec, ClickedBlock>(3.seconds)

    private val patternGroup = RepoPattern.group("dungeons.highlightclickedblock")
    private val leverPattern by patternGroup.pattern(
        "lever",
        "§cYou hear the sound of something opening\\.\\.\\.",
    )
    private val lockedPattern by patternGroup.pattern(
        "locked",
        "§cThat chest is locked!",
    )
    private const val WATER_ROOM_ID = "-60,-60"
    private const val WITHER_ESSENCE_TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3MDdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0="

    private var colorIndex = 0
    private val allowedColors by lazy {
        val ignoredColors = listOf(
            LorenzColor.BLACK,
            LorenzColor.WHITE,
            LorenzColor.CHROMA,
            LorenzColor.GRAY,
            LorenzColor.DARK_GRAY,
        )
        LorenzColor.entries.filter { it !in ignoredColors }
    }

    private fun getRandomColor(): Color {
        colorIndex = (colorIndex + 1) % allowedColors.size
        return allowedColors[colorIndex].toColor()
    }

    private val inWaterRoom by RecalculatingValue(1.seconds) { DungeonAPI.getRoomID() == WATER_ROOM_ID }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (leverPattern.matches(event.message)) {
            event.blockedReason = "dungeon_highlight_clicked_block"
        }

        if (lockedPattern.matches(event.message)) {
            blocks.lastOrNull { it.value.isChest }?.value?.color = config.lockedChestColor.toChromaColor()
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val position = event.position

        val type = when (position.getBlockAt()) {
            Blocks.chest -> ClickedBlockType.CHEST
            Blocks.trapped_chest -> ClickedBlockType.TRAPPED_CHEST
            Blocks.lever -> {
                if (inWaterRoom) return
                ClickedBlockType.LEVER
            }

            Blocks.skull -> {
                if (BlockUtils.getTextureFromSkull(position) != WITHER_ESSENCE_TEXTURE) return
                ClickedBlockType.WITHER_ESSENCE
            }
            else -> return
        }

        val color = if (config.randomColor) getRandomColor() else type.color()
        val displayText = ExtendedChatColor(color.rgb, false).toString() + "Clicked " + type.display
        blocks[position] = ClickedBlock(displayText, color, type.isChest)
    }

    private enum class ClickedBlockType(val display: String, val color: () -> Color) {
        LEVER("Lever", { config.leverColor.toChromaColor() }),
        CHEST("Chest", { config.chestColor.toChromaColor() }),
        TRAPPED_CHEST("Trapped Chest", { config.trappedChestColor.toChromaColor() }),
        WITHER_ESSENCE("Wither Essence", { config.witherEssenceColor.toChromaColor() }),
        ;

        val isChest get() = this == CHEST || this == TRAPPED_CHEST
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        blocks.forEach { (position, block) ->
            event.drawColor(position, block.color)
            if (config.showText) {
                event.drawString(position.add(0.5, 0.5, 0.5), block.displayText, true)
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(52, "dungeon.highlightClickedBlocks", "dungeon.clickedBlocks.enabled")
    }

    private class ClickedBlock(val displayText: String, var color: Color, val isChest: Boolean)

    private fun isEnabled() = !DungeonAPI.inBossRoom && DungeonAPI.inDungeon() && config.enabled

}
