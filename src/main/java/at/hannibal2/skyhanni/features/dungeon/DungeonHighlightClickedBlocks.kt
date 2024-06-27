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
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonHighlightClickedBlocks {
    private val config get() = SkyHanniMod.feature.dungeon.clickedBlocks
    private val blocks = TimeLimitedCache<LorenzVec, ClickedBlock>(3.seconds)
    private var colourIndex = 0
    private val colours = LorenzColor.entries.filter {
        it !in listOf(
            LorenzColor.BLACK,
            LorenzColor.WHITE,
            LorenzColor.CHROMA,
            LorenzColor.GRAY,
            LorenzColor.DARK_GRAY,
        )
    }

    private fun getRandomColour(): LorenzColor {
        var id = colourIndex + 1
        if (id == colours.size) id = 0
        colourIndex = id
        return colours[colourIndex]
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (event.message == "§cYou hear the sound of something opening...") {
            event.blockedReason = "dungeon_highlight_clicked_block"
        }

        if (event.message == "§cThat chest is locked!") {
            blocks.lastOrNull { it.value.displayText.contains("Chest") }?.value?.colour = config.lockedChestColour.toChromaColor()
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (DungeonAPI.inBossRoom) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        val position = event.position
        if (blocks.containsKey(position)) return

        val type: ClickedBlockType = when (position.getBlockAt()) {
            Blocks.chest -> ClickedBlockType.CHEST
            Blocks.trapped_chest -> ClickedBlockType.TRAPPED_CHEST
            Blocks.lever -> ClickedBlockType.LEVER
            Blocks.skull -> ClickedBlockType.WITHER_ESSENCE
            else -> return
        }

        if (type == ClickedBlockType.WITHER_ESSENCE) {
            val text = BlockUtils.getTextureFromSkull(position.toBlockPos())
            if (text != "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQ" +
                "ubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3M" +
                "DdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0="
            ) {
                return
            }
        }

        val inWaterRoom = DungeonAPI.getRoomID() == "-60,-60"
        if (inWaterRoom && type == ClickedBlockType.LEVER) return

        val color = if (config.randomColourEnabled) getRandomColour().toColor() else type.colour()
        val displayText = ExtendedChatColor(color.rgb, false).toString() + "Clicked " + type.display
        blocks[position] = ClickedBlock(displayText, color)
    }

    enum class ClickedBlockType(val display: String, val colour: () -> Color) {
        LEVER("Lever", { config.leverColour.toChromaColor() }),
        CHEST("Chest", { config.chestColour.toChromaColor() }),
        TRAPPED_CHEST("Trapped Chest", { config.trappedChestColour.toChromaColor() }),
        WITHER_ESSENCE("Wither Essence", { config.witherEssenceColour.toChromaColor() }),
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        blocks.forEach { (position, block) ->
            event.drawColor(position, block.colour)
            if (config.showTextEnabled) {
                event.drawString(position.add(0.5, 0.5, 0.5), block.displayText, true)
            } else {
                return
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(52, "dungeon.highlightClickedBlocks", "dungeon.clickedBlocks.enabled")
    }

    class ClickedBlock(val displayText: String, var colour: Color)

    fun isEnabled() = !DungeonAPI.inBossRoom && inDungeonAPI.inDungeon() && config.enabled

}
