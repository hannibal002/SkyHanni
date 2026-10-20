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
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.compat.container
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@SkyHanniModule
object DungeonSpiritLeapOverlay {
    private val config get() = SkyHanniMod.feature.dungeon.spiritLeapOverlay
    private val colorConfig get() = config.colorConfig

    /**
     * REGEX-TEST: Spirit Leap
     * REGEX-TEST: Teleport to Player
     */
    private val inventoryMenuPattern by DungeonApi.patternGroup.pattern(
        "spirit-leap.menu",
        "Spirit Leap|Teleport to Player",
    )

    private var scaleFactor: Double = 1.0
    private var overlayPosition: Position? = null
    private var containerWidth = 0
    private var containerHeight = 0
    private var playerList = emptyList<PlayerStackInfo>()
    private val inventory = InventoryDetector { inventoryMenuPattern }

    data class PlayerStackInfo(val playerInfo: DungeonApi.TeamMember?, val stack: SafeItemStack, val slotNumber: Int)

    @HandleEvent
    private fun onGuiContainerPreDraw(event: GuiContainerEvent.PreDraw) {
        if (!isEnabled()) return

        val gui = event.gui
        // TODO find a way to make InventoryDetector usable here.
        if (gui !is ContainerScreen || !inventory.isInside()) return
        containerWidth = gui.width
        containerHeight = gui.height
        scaleFactor = min(containerWidth, containerHeight).toDouble() / max(containerWidth, containerHeight).toDouble()

        updatePlayerList(gui.container as ChestMenu)

        renderCenteredOverlay(gui)
        event.cancel()
    }

    private fun renderCenteredOverlay(gui: ContainerScreen) {
        val layout = createLeapButtons().take(4).chunked(2)
        val renderable = createOverlayTable(layout)
        overlayPosition = Position(
            (gui.width - renderable.width) / 2,
            (gui.height - renderable.height) / 2,
        ).apply {
            renderRenderable(renderable, posLabel = "Spirit Leap Overlay", addToGuiManager = false)
        }
    }

    private fun createLeapButtons(): List<Renderable> = playerList.mapIndexedNotNull { index, stackInfo ->
        val member = stackInfo.playerInfo ?: return@mapIndexedNotNull null
        val itemRenderable = createPlayerItem(stackInfo.stack)
        val row = createRowLayout(itemRenderable, member.createPlayerInfo())
        val line = createFixedWidthLine(row)
        val keybindHint = createKeybindHint(index)
        val content = createButtonContent(keybindHint, line)
        Renderable.clickable(
            render = createButtonFrame(content, member.getBackgroundColor()),
            onLeftClick = { leapToPlayer(stackInfo) },
        )
    }

    private fun createButtonContent(keybindHint: Renderable, line: Renderable): VerticalContainerRenderable = Renderable.vertical(
        keybindHint,
        Renderable.placeholder(0, height = (-15 * scaleFactor).toInt()),
        Renderable.fixedSizeColumn(
            line,
            height = (containerHeight * 0.35).toInt(),
        ),
    )

    private fun updatePlayerList(chest: ChestMenu) {
        val list = collectPlayerStacks(chest)
        playerList = sortByClass(list)
    }

    private fun collectPlayerStacks(chest: ChestMenu): List<PlayerStackInfo> = buildList {
        for ((slot, stack) in chest.getUpperItems()) {
            val lore = stack.getLore()
            if (lore.isNotEmpty()) {
                val playerInfo = DungeonApi.getPlayerInfo(stack.hoverName.formattedTextCompatLeadingWhiteLessResets().cleanPlayerName())
                add(PlayerStackInfo(playerInfo, stack, slot.index))
            }
        }
    }

    private fun sortByClass(list: List<PlayerStackInfo>): List<PlayerStackInfo> =
        list.sortedBy { it.playerInfo?.dungeonClass?.ordinal }

    private fun DungeonApi.TeamMember.createPlayerInfo(): VerticalContainerRenderable {
        val classInfo = getClassInfo()
        return Renderable.playerInfo(username, classInfo)
    }

    @HandleEvent
    private fun onKeyPress(event: KeyDownEvent) {
        if (!isEnabled() || !config.spiritLeapKeybindConfig.enableKeybind) return
        if (!inventory.isInside()) return
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

    private fun getKeybindIndex(keyCode: Int): Int = spiritLeapKeybinds.indexOf(keyCode)

    private fun createOverlayTable(layout: List<List<Renderable>>): Renderable {
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

    private fun DungeonApi.TeamMember.getBackgroundColor(): Color {
        val backgroundColor = if (playerDead) deadTeammateColor else getClassColor(dungeonClass)
        return backgroundColor.toColor()
    }

    private fun createFixedWidthLine(content: HorizontalContainerRenderable): Renderable = Renderable.fixedSizeLine(
        content,
        width = (containerWidth * 0.40).toInt(),
        verticalAlign = VerticalAlignment.CENTER,
    )

    private fun createButtonFrame(
        input: VerticalContainerRenderable,
        backgroundColor: Color,
    ): Renderable = Renderable.drawInsideRoundedRectWithOutline(
        input = input,
        verticalAlign = VerticalAlignment.CENTER,
        color = backgroundColor,
        topOutlineColor = 0xFFFFF,
        bottomOutlineColor = 0xFFFFF,
        borderOutlineThickness = 2,
        radius = 7,
        smoothness = 10,
        padding = 5,
    )

    private fun createPlayerItem(stack: SafeItemStack): Renderable = Renderable.drawInsideRoundedRect(
        Renderable.item(stack) {
            scale = scaleFactor * 0.9 + 2.7
        },
        color = Color(255, 255, 255, 100),
        radius = 5,
    )

    private fun DungeonApi.TeamMember.getClassInfo(): String = buildString {
        dungeonClass?.let {
            append(it.displayName)
            if (config.showDungeonClassLevel) append(" $classLevel")
            if (playerDead) append(" (Dead)")
        }
    }

    private fun createRowLayout(
        itemRenderable: Renderable,
        playerInfo: VerticalContainerRenderable,
    ): HorizontalContainerRenderable = Renderable.horizontal(
        Renderable.placeholder((containerWidth * 0.01).toInt(), 0),
        itemRenderable,
        Renderable.placeholder((containerWidth * 0.01).toInt(), 0),
        playerInfo,
        verticalAlign = VerticalAlignment.CENTER,
    )

    private fun Renderable.Companion.playerInfo(text: String, classInfo: String): VerticalContainerRenderable = vertical(
        wrappedText(
            text,
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

    private fun createKeybindHint(index: Int): Renderable {
        val hasKeybind = index in 0..<spiritLeapKeybinds.count()
        val showHint = config.spiritLeapKeybindConfig.showKeybindHint && hasKeybind
        if (!showHint) return Renderable.placeholder(width = 10, height = (12 * scaleFactor).toInt())
        return createKeybindBox(createKeybindText(index))
    }

    private fun createKeybindBox(input: StringRenderable): Renderable = Renderable.drawInsideRoundedRectOutline(
        input,
        topOutlineColor = 0xFFFFF,
        bottomOutlineColor = 0xFFFFF,
        borderOutlineThickness = 2,
        padding = 4,
        horizontalAlign = HorizontalAlignment.RIGHT,
    )

    private fun createKeybindText(index: Int): StringRenderable = Renderable.text(
        KeyboardManager.getKeyName(spiritLeapKeybinds[index]),
        (scaleFactor * 0.9) + 0.7,
        verticalAlign = VerticalAlignment.CENTER,
    )

    private fun leapToPlayer(player: PlayerStackInfo) {
        val playerInfo = player.playerInfo ?: return
        if (playerInfo.playerDead) {
            ChatUtils.chat("§cCannot leap - §e${playerInfo.username} §cis dead.")
            return
        }
        InventoryUtils.clickSlot(player.slotNumber, button = MIDDLE_CLICK)
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
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(98, "dungeon.spiritLeapOverlay.archerClassColor", "dungeon.spiritLeapOverlay.colorConfig.archerClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.mageClassColor", "dungeon.spiritLeapOverlay.colorConfig.mageClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.berserkClassColor", "dungeon.spiritLeapOverlay.colorConfig.berserkClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.tankClassColor", "dungeon.spiritLeapOverlay.colorConfig.tankClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.healerClassColor", "dungeon.spiritLeapOverlay.colorConfig.healerClassColor")
        event.move(98, "dungeon.spiritLeapOverlay.defaultColor", "dungeon.spiritLeapOverlay.colorConfig.defaultColor")
        event.move(98, "dungeon.spiritLeapOverlay.deadTeammateColor", "dungeon.spiritLeapOverlay.colorConfig.deadTeammateColor")
    }
}
