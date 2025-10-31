package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.ClickedBlockType
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.dungeon.DungeonBlockClickEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.rgb
import at.hannibal2.hanni.utils.ExtendedChatColor
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.collection.TimeLimitedCache
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import io.github.notenoughupdates.moulconfig.ChromaColour
import kotlin.time.Duration.Companion.seconds

@HanniModule
object DungeonHighlightClickedBlocks {

    private val config get() = HanniMod.feature.dungeon.clickedBlocks

    private val patternGroup = RepoPattern.group("dungeons.highlightclickedblock")
    private val leverPattern by patternGroup.pattern(
        "lever",
        "§cYou hear the sound of something opening\\.\\.\\.",
    )
    private val lockedPattern by patternGroup.pattern(
        "locked",
        "§cThat chest is locked!",
    )

    private val blocks = TimeLimitedCache<LorenzVec, ClickedBlock>(3.seconds)
    private var colorIndex = 0
    private val undesirableColors = listOf(
        LorenzColor.BLACK,
        LorenzColor.WHITE,
        LorenzColor.CHROMA,
        LorenzColor.GRAY,
        LorenzColor.DARK_GRAY,
    )
    private val randomColors = LorenzColor.entries.filter { it !in undesirableColors }

    private fun getRandomColor(): LorenzColor {
        var id = colorIndex + 1
        if (id == randomColors.size) id = 0
        colorIndex = id
        return randomColors[colorIndex]
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        if (leverPattern.matches(event.message)) {
            event.blockedReason = "dungeon_highlight_clicked_block"
        }

        if (lockedPattern.matches(event.message)) {
            blocks.values.lastOrNull { it.displayText.contains("Chest") }?.color = config.lockedChestColor
        }
    }

    @HandleEvent
    fun onDungeonClickedBlock(event: DungeonBlockClickEvent) {
        if (!isEnabled()) return
        if (DungeonApi.inWaterRoom && event.blockType == ClickedBlockType.LEVER) return

        val type = event.blockType

        val color = if (config.randomColor) getRandomColor().toChromaColor() else getBlockProperties(type).color
        val displayText = ExtendedChatColor(color.rgb, false).toString() + "Clicked " + getBlockProperties(type).name
        blocks[event.position] = ClickedBlock(displayText, color)

    }

    private fun getBlockProperties(type: ClickedBlockType): BlockProperties {
        return when (type) {
            ClickedBlockType.LEVER -> BlockProperties("Lever", config.leverColor)
            ClickedBlockType.CHEST -> BlockProperties("Chest", config.chestColor)
            ClickedBlockType.TRAPPED_CHEST -> BlockProperties("Trapped Chest", config.trappedChestColor)
            ClickedBlockType.WITHER_ESSENCE -> BlockProperties("Wither Essence", config.witherEssenceColor)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        blocks.forEach { (position, block) ->
            event.drawColor(position, block.color)
            if (config.showText) {
                event.drawString(position.blockCenter(), block.displayText, true)
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(56, "dungeon.highlightClickedBlocks", "dungeon.clickedBlocks.enabled")
    }

    private data class ClickedBlock(val displayText: String, var color: ChromaColour)
    private data class BlockProperties(val name: String, val color: ChromaColour)

    private fun isEnabled() = !DungeonApi.inBossRoom && DungeonApi.inDungeon() && config.enabled

}
