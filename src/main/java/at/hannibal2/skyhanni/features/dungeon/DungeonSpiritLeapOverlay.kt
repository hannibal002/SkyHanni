package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.dungeon.spiritleap.SpiritLeapColorConfig
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@SkyHanniModule
object DungeonSpiritLeapOverlay {
    private val config get() = SkyHanniMod.feature.dungeon.spiritLeapOverlay
    private val colorConfig get() = config.colorConfig

    private var scaleFactor: Double = 1.0
    private var overlayPosition: Position? = null
    private var containerWidth = 0
    private var containerHeight = 0
    private var playerList = emptyList<PlayerStackInfo>()
    private val validInventoryNames = setOf("Spirit Leap", "Teleport to Player")

    data class PlayerStackInfo(val playerInfo: DungeonApi.TeamMember?, val stack: ItemStack, val slotNumber: Int)

    @HandleEvent
    fun onGuiContainerPreDraw(event: GuiContainerEvent.PreDraw) {
        if (!isEnabled()) return

        val gui = event.gui
        // TODO find a way to make InventoryDetector usable here.
        if (gui !is GuiChest || InventoryUtils.openInventoryName().removeColor() !in validInventoryNames) return
        containerWidth = gui.width
        containerHeight = gui.height
        scaleFactor = min(containerWidth, containerHeight).toDouble() / max(containerWidth, containerHeight).toDouble()

        val chest = gui.inventorySlots as ContainerChest
        playerList = buildList {
            for ((slot, stack) in chest.getUpperItems()) {
                val lore = stack.getLore()
                if (lore.isNotEmpty()) {
                    val playerInfo = DungeonApi.getPlayerInfo(stack.displayName.cleanPlayerName())
                    add(PlayerStackInfo(playerInfo, stack, slot.slotNumber))
                }
            }
        }.sortedBy { it.playerInfo?.dungeonClass?.ordinal }

        val leapRenderItems = playerList.mapIndexedNotNull { index, player ->
            createLeapItem(player, index)
        }

        val spiritLeapOverlay = createSpiritLeapOverlay(leapRenderItems)

        overlayPosition = Position(
            (gui.width - spiritLeapOverlay.width) / 2,
            (gui.height - spiritLeapOverlay.height) / 2,
        ).apply {
            renderRenderable(spiritLeapOverlay, posLabel = "Spirit Leap Overlay", addToGuiManager = false)
        }
        event.cancel()
    }

    @HandleEvent
    fun onKeyPress(event: KeyDownEvent) {
        if (!isEnabled() || !config.spiritLeapKeybindConfig.enableKeybind) return
        val index = getKeybindIndex(event.keyCode)
        if (index !in 0..<playerList.count()) return
        leapToPlayer(playerList[index])
    }

    private val spiritLeapKeybinds
        get() = intArrayOf(
            config.spiritLeapKeybindConfig.keybindOption1,
            config.spiritLeapKeybindConfig.keybindOption2,
            config.spiritLeapKeybindConfig.keybindOption3,
            config.spiritLeapKeybindConfig.keybindOption4,
        )

    private fun getKeybindIndex(keyCode: Int): Int {
        return spiritLeapKeybinds.indexOf(keyCode)
    }

    private fun createSpiritLeapOverlay(leapRenderItems: List<Renderable>): Renderable {
        val layout = leapRenderItems.take(4).chunked(2)
        return if (layout.isNotEmpty()) Renderable.table(
            layout,
            xSpacing = 18,
            ySpacing = 18,
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        ) else Renderable.wrappedText(
            setWidth = (containerWidth * 0.8).toInt(),
            text = "No targets available for leap.",
            scale = scaleFactor * 3,
        )
    }

    private fun createLeapItem(playerStackInfo: PlayerStackInfo, index: Int): Renderable? {
        val player = playerStackInfo.playerInfo ?: return null
        val classInfo = buildString {
            player.dungeonClass?.let {
                append(it.scoreboardName)
                if (config.showDungeonClassLevel) append(" ${player.classLevel}")
                if (player.playerDead) append(" (Dead)")
            }
        }

        val backgroundColor = if (player.playerDead) deadTeammateColor else getClassColor(player.dungeonClass)
        val itemRenderable = Renderable.drawInsideRoundedRect(
            Renderable.item(playerStackInfo.stack, scale = scaleFactor * 0.9 + 2.7),
            color = Color(255, 255, 255, 100),
            radius = 5,
        )

        val playerInfoRenderable = with(Renderable) {
            vertical(
                wrappedText(
                    player.username,
                    setWidth = (containerWidth * 0.25).toInt(),
                    scale = scaleFactor + 1.5,
                ),
                placeholder(0, (containerHeight * 0.03).toInt()),
                wrappedText(
                    classInfo,
                    setWidth = (containerWidth * 0.25).toInt(),
                    scale = (scaleFactor * 0.9) + 1.1,
                ),
                horizontalAlign = HorizontalAlignment.CENTER,
                verticalAlign = VerticalAlignment.CENTER,
            )
        }

        val buttonLayout = Renderable.horizontal(
            Renderable.placeholder((containerWidth * 0.01).toInt(), 0),
            itemRenderable,
            Renderable.placeholder((containerWidth * 0.01).toInt(), 0),
            playerInfoRenderable,
            verticalAlign = VerticalAlignment.CENTER,
        )

        return Renderable.clickable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.vertical(
                    if (config.spiritLeapKeybindConfig.showKeybindHint && index in 0..<spiritLeapKeybinds.count()) {
                        Renderable.drawInsideRoundedRectOutline(
                            Renderable.text(
                                KeyboardManager.getKeyName(spiritLeapKeybinds[index]),
                                (scaleFactor * 0.9) + 0.7,
                                verticalAlign = VerticalAlignment.CENTER,
                            ),
                            topOutlineColor = 0xFFFFF,
                            bottomOutlineColor = 0xFFFFF,
                            borderOutlineThickness = 2,
                            padding = 4,
                            horizontalAlign = HorizontalAlignment.RIGHT,
                        )
                    } else {
                        Renderable.placeholder(width = 10, height = (12 * scaleFactor).toInt())
                    },
                    Renderable.placeholder(0, height = (-15 * scaleFactor).toInt()),
                    Renderable.fixedSizeColumn(
                        Renderable.fixedSizeLine(
                            buttonLayout,
                            width = (containerWidth * 0.40).toInt(),
                            verticalAlign = VerticalAlignment.CENTER,
                        ),
                        height = (containerHeight * 0.35).toInt(),
                    ),
                ),
                verticalAlign = VerticalAlignment.CENTER,
                color = backgroundColor.toColor(),
                topOutlineColor = 0xFFFFF,
                bottomOutlineColor = 0xFFFFF,
                borderOutlineThickness = 2,
                radius = 7,
                smoothness = 10,
                padding = 5,
            ),
            onLeftClick = { leapToPlayer(playerStackInfo) },
        )
    }

    private fun leapToPlayer(player: PlayerStackInfo) {
        val playerInfo = player.playerInfo ?: return
        if (playerInfo.playerDead) {
            ChatUtils.chat("§cCannot leap — §e${playerInfo.username} §cis dead.")
            return
        }
        InventoryUtils.clickSlot(player.slotNumber, mouseButton = 2, mode = 3)
    }

    private val deadTeammateColor = colorConfig.deadTeammateColor

    private fun getClassColor(dungeonClass: DungeonApi.DungeonClass?): ChromaColour = when (dungeonClass) {
        DungeonApi.DungeonClass.ARCHER -> colorConfig.archerClassColor
        DungeonApi.DungeonClass.MAGE -> colorConfig.mageClassColor
        DungeonApi.DungeonClass.BERSERK -> colorConfig.berserkClassColor
        DungeonApi.DungeonClass.TANK -> colorConfig.tankClassColor
        DungeonApi.DungeonClass.HEALER -> colorConfig.healerClassColor
        else -> SpiritLeapColorConfig.defaultColor
    }

    private fun isEnabled() = config.enabled && DungeonApi.inDungeon() && DungeonApi.started && !DungeonApi.completed

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(98, "dungeon.spiritLeapOverlay.archerClassColor", "dungeon.spiritLeapOverlay.colorConfig.archerClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.mageClassColor", "dungeon.spiritLeapOverlay.colorConfig.mageClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.berserkClassColor", "dungeon.spiritLeapOverlay.colorConfig.berserkClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.tankClassColor", "dungeon.spiritLeapOverlay.colorConfig.tankClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.healerClassColor", "dungeon.spiritLeapOverlay.colorConfig.healerClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.defaultColor", "dungeon.spiritLeapOverlay.colorConfig.defaultColor")
        event.move(98, "dungeon.spiritLeapOverlay.deadTeammateColor", "dungeon.spiritLeapOverlay.colorConfig.deadTeammateColor")
    }
}
