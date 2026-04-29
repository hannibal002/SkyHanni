package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlin.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object QuadLinkLegacySolver {

    private val config get() = SkyHanniMod.feature.rift.area.mountaintop

    private const val CONNECT_FOUR_CHEST_TITLE = "Quad Link Legacy - Wizardman"
    private const val WIZARD_PIECE_INDICATOR_SLOT = 18
    private const val PLAYER_PIECE_INDICATOR_SLOT = 26

    private const val COLUMN_COUNT = 7
    private const val ROW_COUNT = 6
    const val WIZARD_PIECE = 'X'
    const val PLAYER_PIECE = 'O'
    const val EMPTY_PIECE = '_'

    private var solver: Solver? = null

    private val inInventory = AtomicBoolean(false)
    private val currentPieceCount = AtomicInteger(0)
    private var currentBoard: CharArray? = null
    private var latestResponse: QLLResponse? = null
    private var solverAdapterJob: Job? = null

    private val solverCommand = AtomicReference<QLLCommand?>(null)
    private val solverResponse = AtomicReference<QLLResponse?>(null)

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory.set(event.inventoryName.contains(CONNECT_FOUR_CHEST_TITLE, ignoreCase = true))
        solver = Solver()
    }

    @HandleEvent
    fun onInventoryClose() {
        if (!inInventory.getAndSet(false)) return
        currentPieceCount.set(0)
        currentBoard = null
        latestResponse = null
        solverCommand.set(null)
        solverResponse.set(null)
        solverAdapterJob?.cancel()
        solverAdapterJob = null
        solver = null // garbage collection
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.THE_RIFT)
    fun onSecondPassed(event: SecondPassedEvent) {
        checkBoardForUpdates()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        checkBoardForUpdates()
    }

    private fun checkBoardForUpdates() {

        if (!isEnabled()) return

        if (!updateBoardState()) return

        val solver = solver ?: return // it's impossible for this to be null but detekt complains about !!
        val currentBoard = currentBoard ?: return
        solverCommand.set(QLLCommand(currentPieceCount.get(), currentBoard))

        if (solverAdapterJob?.isActive != true) {
            solverAdapterJob = CoroutineSettings(
                "Quad Link Legacy Solver",
                timeout = Duration.INFINITE,
            ).launch {
                while (true) {
                    val command = solverCommand.getAndSet(null)
                    if (command == null) {
                        delay(20.milliseconds)
                        continue
                    }
                    solver.recommendMovesUntil(String(command.boardSnapshot), command.pieceCount, solverResponse) {
                        !inInventory.get() || solverCommand.get() != null
                    }
                }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChestGuiRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val currentBoard = currentBoard ?: return
        val recommendedMove = latestResponse
            ?.takeIf { it.pieceCount == currentPieceCount.get() }
            ?.recommendedMove

        config.connectFourDebugPosition.renderRenderable(
            createDebugBoard(currentBoard, recommendedMove),
            posLabel = "Quad Link Legacy Debug",
        )
    }

    @HandleEvent(GuiContainerEvent.BackgroundDrawnEvent::class, onlyOnSkyblock = true)
    fun onBackgroundDrawn() {
        if (!isEnabled()) return

        if (currentPieceCount.get() % 2 == 0) return

        solverResponse.getAndSet(null)?.let { latestResponse = it }
        val response = latestResponse ?: return
        if (response.pieceCount != currentPieceCount.get()) return

        val column = response.recommendedMove
        if (column !in 0..<COLUMN_COUNT) return

        val guiColumn = column + 1
        for (row in 0..<ROW_COUNT) {
            val slotIndex = guiColumn + row * 9
            InventoryUtils.getSlotAtIndex(slotIndex)?.highlight(LorenzColor.GREEN)
        }
    }

    private fun updateBoardState(): Boolean {
        // use InventoryUtils because hypixel keeps sending garbage inventory update packets
        val inventoryItems = InventoryUtils.getItemsInOpenChestWithNull().associate { it.containerSlot to it.item.orNull() }

        val enemyColor = inventoryItems[WIZARD_PIECE_INDICATOR_SLOT]?.getSkullTexture() ?: return false
        val playerColor = inventoryItems[PLAYER_PIECE_INDICATOR_SLOT]?.getSkullTexture() ?: return false

        val board = CharArray(COLUMN_COUNT * ROW_COUNT) { EMPTY_PIECE }
        var pieceCount = 0

        for (row in 0..<ROW_COUNT) {
            for (column in 0..<COLUMN_COUNT) {
                val slot = column + 1 + row * 9
                val piece = when (inventoryItems[slot]?.getSkullTexture()) {
                    enemyColor -> WIZARD_PIECE
                    playerColor -> PLAYER_PIECE
                    else -> EMPTY_PIECE
                }
                board[row * COLUMN_COUNT + column] = piece
                if (piece != EMPTY_PIECE) pieceCount++
            }
        }

        if (pieceCount <= currentPieceCount.get()) return false

        currentPieceCount.set(pieceCount)
        currentBoard = board
        return true
    }

    private fun createDebugBoard(
        board: CharArray,
        recommendedMove: Int?,
    ) = with(Renderable) {
        vertical(
            listOf(
                text("Recommended Move: $recommendedMove"),
                table(
                    List(ROW_COUNT) { row ->
                        List(COLUMN_COUNT) { column ->
                            text(board[row * COLUMN_COUNT + column].toString())
                        }
                    },
                    xSpacing = 3,
                    ySpacing = 1,
                ),
            ),
            spacing = 1,
        )
    }

    private fun isEnabled() = config.connectFourSolver && inInventory.get()

    private data class QLLCommand(val pieceCount: Int, val boardSnapshot: CharArray)

    data class QLLResponse(val pieceCount: Int, val recommendedMove: Int)
}
