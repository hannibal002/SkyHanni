package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import net.minecraft.init.Blocks
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonHighlightClickedBlocks {

    private val blocks = mutableListOf<ClickedBlock>()
    private var colorIndex = 0
    private val colors = listOf(LorenzColor.YELLOW, LorenzColor.AQUA, LorenzColor.GREEN, LorenzColor.LIGHT_PURPLE)

    private fun getNextColor(): LorenzColor {
        var id = colorIndex + 1
        if (id == colors.size) id = 0
        colorIndex = id
        return colors[colorIndex]
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!SkyHanniMod.feature.dungeon.highlightClickedBlocks) return
        if (!LorenzUtils.inDungeons) return

        if (event.message == "§cYou hear the sound of something opening...") {
            event.blockedReason = "dungeon_highlight_clicked_block"
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!SkyHanniMod.feature.dungeon.highlightClickedBlocks) return
        if (!LorenzUtils.inDungeons) return
        if (DungeonAPI.inBossRoom) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        val position = event.position
        if (blocks.any { it.position == position }) return

        val type: ClickedBlockType = when (position.getBlockAt()) {
            Blocks.chest, Blocks.trapped_chest -> ClickedBlockType.CHEST
            Blocks.lever -> ClickedBlockType.LEVER
            Blocks.skull -> ClickedBlockType.WITHER_ESSENCE
            else -> return
        }

        // Water room
        if (DungeonAPI.getFloorId() == "-60,-60") {
            return
        }

        if (type == ClickedBlockType.WITHER_ESSENCE) {
            val text = BlockUtils.getTextureFromSkull(position.toBlocPos())
            if (text != "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQ" +
                "ubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3M" +
                "DdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0="
            ) {
                return
            }
        }

        // TODO hide in water room
//        if (nearWaterRoom() && type == ClickedBlockType.LEVER) return

        val color = getNextColor()
        val displayText = color.getChatColor() + "Clicked " + type.display
        blocks.add(ClickedBlock(position, displayText, color, System.currentTimeMillis()))
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!SkyHanniMod.feature.dungeon.highlightClickedBlocks) return
        if (!LorenzUtils.inDungeons) return

        blocks.removeAll { System.currentTimeMillis() > it.time + 3000 }
        blocks.forEach {
            event.drawColor(it.position, it.color)
            drawString(it.position.add(0.5, 0.5, 0.5), it.displayText, true)
        }
    }

    class ClickedBlock(val position: LorenzVec, val displayText: String, val color: LorenzColor, val time: Long)

    enum class ClickedBlockType(val display: String) {
        LEVER("Lever"),
        CHEST("Chest"),
        WITHER_ESSENCE("Wither Essence"),
    }
}