package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonHighlightClickedBlocks {

    private val config get() = SkyHanniMod.feature.dungeon.highlightClickedBlocks

    private val blocks = TimeLimitedCache<LorenzVec, ClickedBlock>(3.seconds)
    private var colorIndex = 0
    private val colors = listOf(LorenzColor.YELLOW, LorenzColor.AQUA, LorenzColor.GREEN, LorenzColor.LIGHT_PURPLE)
    private const val WATER_ROOM_ID = "-60,-60"
    private const val WITHER_ESSENCE_TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3MDdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0="

    private fun getNextColor(): LorenzColor {
        var id = colorIndex + 1
        if (id == colors.size) id = 0
        colorIndex = id
        return colors[colorIndex]
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (event.message == "§cYou hear the sound of something opening...") {
            event.blockedReason = "dungeon_highlight_clicked_block"
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        val position = event.position
        if (blocks.containsKey(position)) return

        val type = when (position.getBlockAt()) {
            Blocks.chest, Blocks.trapped_chest -> ClickedBlockType.CHEST
            Blocks.lever -> ClickedBlockType.LEVER
            Blocks.skull -> ClickedBlockType.WITHER_ESSENCE
            else -> return
        }

        if (type == ClickedBlockType.WITHER_ESSENCE) {
            if (BlockUtils.getTextureFromSkull(position) != WITHER_ESSENCE_TEXTURE) return
        }

        val inWaterRoom = DungeonAPI.getRoomID() == WATER_ROOM_ID
        if (inWaterRoom && type == ClickedBlockType.LEVER) return

        val color = getNextColor()
        val displayText = color.getChatColor() + "Clicked " + type.display
        blocks[position] = ClickedBlock(displayText, color)
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        blocks.forEach { (position, block) ->
            event.drawColor(position, block.color)
            event.drawString(position.add(0.5, 0.5, 0.5), block.displayText, true)
        }
    }

    private fun isEnabled() = !DungeonAPI.inBossRoom && DungeonAPI.inDungeon() && config

    class ClickedBlock(
        val displayText: String,
        val color: LorenzColor,
    )

    enum class ClickedBlockType(val display: String) {
        LEVER("Lever"),
        CHEST("Chest"),
        WITHER_ESSENCE("Wither Essence"),
    }
}
