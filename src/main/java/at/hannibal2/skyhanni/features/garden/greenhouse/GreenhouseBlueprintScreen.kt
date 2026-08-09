package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.GardenStorage.GreenHouseStorage
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import kotlin.math.floor

internal class GreenhouseBlueprintScreen(private val plotId: Int) : SkyHanniBaseScreen() {

    private val actions = mutableListOf<ActionArea>()
    private var scrollOffset = 0
    private var maximumScroll = 0
    private var pendingDelete: String? = null
    private var currentMouseX = 0
    private var currentMouseY = 0

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        currentMouseX = mouseX
        currentMouseY = mouseY
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        actions.clear()

        val panelWidth = PANEL_WIDTH.coerceAtMost(width - 20)
        val panelHeight = PANEL_HEIGHT.coerceAtMost(height - 20)
        val panelX = (width - panelWidth) / 2
        val panelY = (height - panelHeight) / 2
        GuiRenderUtils.drawFloatingRectDark(panelX, panelY, panelWidth, panelHeight)

        GuiRenderUtils.drawString("§6§lGreenhouse Layouts", panelX + PADDING, panelY + PADDING)

        drawButton(panelX + panelWidth - 188, panelY + 9, 80, 18, "§bImport Link") {
            pendingDelete = null
            GreenhouseMutationBlueprint.importFromClipboard()
        }
        drawButton(panelX + panelWidth - 98, panelY + 9, 84, 18, "§aCapture New") {
            pendingDelete = null
            GreenhouseMutationBlueprint.captureGreenhouse()
        }

        val listTop = panelY + HEADER_HEIGHT
        val listBottom = panelY + panelHeight - FOOTER_HEIGHT
        val layouts = GreenhouseMutationBlueprint.layouts().toSortedMap()
        maximumScroll = (layouts.size * (CARD_HEIGHT + CARD_SPACING) - (listBottom - listTop)).coerceAtLeast(0)
        scrollOffset = scrollOffset.coerceIn(0, maximumScroll)

        GuiRenderUtils.enableScissor(panelX + PADDING, listTop, panelX + panelWidth - PADDING, listBottom)
        if (layouts.isEmpty()) {
            GuiRenderUtils.drawString(
                "§7No layouts saved yet. Click §aCapture New §7while standing in the Greenhouse.",
                panelX + PADDING + 8,
                listTop + 18,
            )
        } else {
            layouts.entries.forEachIndexed { index, (name, blueprint) ->
                val cardY = listTop + index * (CARD_HEIGHT + CARD_SPACING) - scrollOffset
                if (cardY >= listTop && cardY + CARD_HEIGHT <= listBottom) {
                    drawLayoutCard(panelX + PADDING, cardY, panelWidth - PADDING * 2, name, blueprint)
                }
            }
        }
        GuiRenderUtils.disableScissor()

        GuiRenderUtils.drawString(
            "§8Named capture: §7/shgreenhouseblueprint capture <name>",
            panelX + PADDING,
            panelY + panelHeight - 22,
        )
        drawButton(panelX + panelWidth - 62, panelY + panelHeight - 28, 48, 18, "§7Close") { onClose() }
    }

    private fun drawLayoutCard(
        x: Int,
        y: Int,
        cardWidth: Int,
        name: String,
        blueprint: GreenHouseStorage.MutationBlueprintStorage,
    ) {
        val active = GreenhouseMutationBlueprint.activeLayoutName(plotId) == name
        GuiRenderUtils.drawRect(x, y, x + cardWidth, y + CARD_HEIGHT, if (active) ACTIVE_CARD_COLOR else CARD_COLOR)
        val borderColor = if (active) ACTIVE_BORDER_COLOR else BORDER_COLOR
        GuiRenderUtils.drawRect(x, y, x + cardWidth, y + 1, borderColor)
        GuiRenderUtils.drawRect(x, y + CARD_HEIGHT - 1, x + cardWidth, y + CARD_HEIGHT, borderColor)
        GuiRenderUtils.drawRect(x, y, x + 1, y + CARD_HEIGHT, borderColor)
        GuiRenderUtils.drawRect(x + cardWidth - 1, y, x + cardWidth, y + CARD_HEIGHT, borderColor)

        drawGridPreview(x + 7, y + 7, blueprint)
        GuiRenderUtils.drawString("${if (active) "§a● " else "§7○ "}§f$name", x + 58, y + 8)
        val target = GreenhouseMutationBlueprint.targetMutation(blueprint)
        GuiRenderUtils.drawString(
            target?.let { "§7Spawned output: §e${it.displayName} §8(ignored)" } ?: "§7Spawned output: §cNot detected",
            x + 58,
            y + 21,
        )
        val roles = GreenhouseMutationBlueprint.roleCounts(blueprint)
        val setupCount = roles.getOrDefault(GreenhouseLayoutAnalysis.Role.SPAWN_INPUT, 0)
        val buffCount = roles.getOrDefault(GreenhouseLayoutAnalysis.Role.YIELD_BUFF, 0)
        val uniqueCount = roles.getOrDefault(GreenhouseLayoutAnalysis.Role.UNIQUE_CROP, 0)
        GuiRenderUtils.drawString("§a$setupCount setup §7• §b$buffCount buffs §7• §6$uniqueCount unique", x + 58, y + 33)

        val buttonY = y + CARD_HEIGHT - 20
        drawButton(x + 58, buttonY, 42, 14, if (active) "§aLoaded" else "§eLoad") {
            pendingDelete = null
            GreenhouseMutationBlueprint.loadLayout(plotId, name)
            ChatUtils.chat("§aLoaded Greenhouse layout §e$name §ain Plot §e$plotId§a.")
        }
        drawButton(x + 105, buttonY, 62, 14, "§bOverwrite") {
            pendingDelete = null
            GreenhouseMutationBlueprint.captureGreenhouse(name)
        }
        val confirmingDelete = pendingDelete == name
        drawButton(
            x + 172,
            buttonY,
            if (confirmingDelete) 65 else 45,
            14,
            if (confirmingDelete) "§cConfirm?" else "§cDelete",
        ) {
            if (confirmingDelete) {
                GreenhouseMutationBlueprint.deleteLayout(name)
                pendingDelete = null
            } else {
                pendingDelete = name
            }
        }
    }

    private fun drawGridPreview(
        x: Int,
        y: Int,
        blueprint: GreenHouseStorage.MutationBlueprintStorage,
    ) {
        GuiRenderUtils.drawRect(x - 1, y - 1, x + PREVIEW_SIZE + 1, y + PREVIEW_SIZE + 1, BORDER_COLOR)
        GuiRenderUtils.drawRect(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, PREVIEW_BACKGROUND)
        for (cell in 1 until GRID_SIZE) {
            val line = if (cell == GRID_SIZE / 2) CHUNK_DIVIDER_COLOR else GRID_LINE_COLOR
            GuiRenderUtils.drawRect(x + cell * CELL_SIZE, y, x + cell * CELL_SIZE + 1, y + PREVIEW_SIZE, line)
            GuiRenderUtils.drawRect(x, y + cell * CELL_SIZE, x + PREVIEW_SIZE, y + cell * CELL_SIZE + 1, line)
        }
        if (blueprint.importedCells.isNotEmpty()) {
            val target = GreenhouseMutationBlueprint.targetMutation(blueprint)
            blueprint.importedCells.forEach { placement ->
                val size = GreenhouseMutation.fromSkyShardsId(placement.cropId)?.size ?: 1
                val role = GreenhouseLayoutAnalysis.roleFor(
                    GreenhouseLayoutAnalysis.Entry(placement.cropId, size * size, placement.target),
                    target,
                )
                repeat(size) { rowOffset ->
                    repeat(size) { columnOffset ->
                        drawPreviewCell(
                            x,
                            y,
                            placement.column + columnOffset,
                            placement.row + rowOffset,
                            placement.cropId,
                            role,
                        )
                    }
                }
            }
        } else {
            val target = GreenhouseMutationBlueprint.targetMutation(blueprint)
            blueprint.mutations.forEach { placement ->
                val cellX = floor(placement.offset.x).toInt() + GRID_SIZE / 2
                val cellZ = floor(placement.offset.z).toInt() + GRID_SIZE / 2
                val size = GreenhouseMutation.fromInternalId(placement.mutationId)?.size ?: placement.size
                val role = GreenhouseLayoutAnalysis.roleFor(
                    GreenhouseLayoutAnalysis.Entry(placement.mutationId.lowercase(), size * size),
                    target,
                )
                drawPreviewCell(x, y, cellX, cellZ, placement.mutationId, role)
            }
        }
    }

    private fun drawPreviewCell(
        x: Int,
        y: Int,
        cellX: Int,
        cellZ: Int,
        id: String,
        role: GreenhouseLayoutAnalysis.Role,
    ) {
        if (cellX !in 0 until GRID_SIZE || cellZ !in 0 until GRID_SIZE) return
        val left = x + cellX * CELL_SIZE + 1
        val top = y + cellZ * CELL_SIZE + 1
        val right = x + (cellX + 1) * CELL_SIZE
        val bottom = y + (cellZ + 1) * CELL_SIZE
        if (role == GreenhouseLayoutAnalysis.Role.TARGET_OUTPUT) {
            GuiRenderUtils.drawRect(left, top, right, bottom, TARGET_CELL_COLOR)
            GuiRenderUtils.drawRect(left + 1, top + 1, right - 1, bottom - 1, PREVIEW_BACKGROUND)
        } else {
            val color = when (role) {
                GreenhouseLayoutAnalysis.Role.SPAWN_INPUT -> SPAWN_INPUT_COLOR
                GreenhouseLayoutAnalysis.Role.YIELD_BUFF -> YIELD_BUFF_COLOR
                GreenhouseLayoutAnalysis.Role.UNIQUE_CROP -> UNIQUE_CROP_COLOR
                GreenhouseLayoutAnalysis.Role.OTHER -> mutationColor(id)
                GreenhouseLayoutAnalysis.Role.TARGET_OUTPUT -> error("Handled above")
            }
            GuiRenderUtils.drawRect(left, top, right, bottom, color)
        }
    }

    private fun drawButton(x: Int, y: Int, buttonWidth: Int, buttonHeight: Int, label: String, action: () -> Unit) {
        val hovered = currentMouseX in x until (x + buttonWidth) && currentMouseY in y until (y + buttonHeight)
        GuiRenderUtils.drawRect(x, y, x + buttonWidth, y + buttonHeight, if (hovered) BUTTON_HOVER_COLOR else BUTTON_COLOR)
        val textX = x + (buttonWidth - mc.font.width(label)) / 2
        GuiRenderUtils.drawString(label, textX, y + (buttonHeight - 8) / 2)
        actions.add(ActionArea(x, y, x + buttonWidth, y + buttonHeight, action))
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        actions.lastOrNull { it.contains(originalMouseX, originalMouseY) }?.action?.invoke()
    }

    override fun onHandleMouseInput() {
        if (!MouseCompat.hasScrollDelta()) return
        scrollOffset = (scrollOffset - MouseCompat.getScrollDelta()).coerceIn(0, maximumScroll)
    }

    override fun isPauseScreen() = false

    private fun mutationColor(mutationId: String): Int {
        val hash = mutationId.hashCode()
        val red = 96 + (hash ushr 16 and 0x7F)
        val green = 96 + (hash ushr 8 and 0x7F)
        val blue = 96 + (hash and 0x7F)
        return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
    }

    private data class ActionArea(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
        val action: () -> Unit,
    ) {
        fun contains(x: Int, y: Int): Boolean = x in minX until maxX && y in minY until maxY
    }

    companion object {
        private const val PANEL_WIDTH = 430
        private const val PANEL_HEIGHT = 350
        private const val PADDING = 12
        private const val HEADER_HEIGHT = 42
        private const val FOOTER_HEIGHT = 38
        private const val CARD_HEIGHT = 70
        private const val CARD_SPACING = 6
        private const val GRID_SIZE = 10
        private const val CELL_SIZE = 4
        private const val PREVIEW_SIZE = GRID_SIZE * CELL_SIZE
        private const val CARD_COLOR = 0xCC181818.toInt()
        private const val ACTIVE_CARD_COLOR = 0xCC16301E.toInt()
        private const val BORDER_COLOR = 0xFF555555.toInt()
        private const val ACTIVE_BORDER_COLOR = 0xFF55FF55.toInt()
        private const val PREVIEW_BACKGROUND = 0xFF242A32.toInt()
        private const val GRID_LINE_COLOR = 0xFF343B45.toInt()
        private const val CHUNK_DIVIDER_COLOR = 0xFF87909B.toInt()
        private const val BUTTON_COLOR = 0xFF303030.toInt()
        private const val BUTTON_HOVER_COLOR = 0xFF505050.toInt()
        private const val TARGET_CELL_COLOR = 0xFFFFAA00.toInt()
        private const val SPAWN_INPUT_COLOR = 0xFF4CAF50.toInt()
        private const val YIELD_BUFF_COLOR = 0xFF3ABED8.toInt()
        private const val UNIQUE_CROP_COLOR = 0xFFE0B84C.toInt()
    }
}
