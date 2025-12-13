package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.garden.greenhouse.DnaAnalyzerSolver.Colors.Companion.toColor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.item.ItemStack

@SkyHanniModule
object DnaAnalyzerSolver {

    private val config get() = SkyHanniMod.feature.garden.dnaAnalyzerSolver

    private var inInventory = false

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory = event.inventoryName.endsWith(" DNA")
    }

    @HandleEvent
    fun onInventoryClose() {
        inInventory = false
        currentBoard = null
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onTooltip(event: ToolTipTextEvent) {
        if (isEnabled() && config.hideTooltips) event.cancel()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !config.useMiddleClick) return
        event.makePickblock()
    }

    private var currentBoard: DnaBoard? = null

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        val initialBoard = MutableList(9) { MutableList(4) { Colors.GREEN } }
        for ((i, stack) in event.inventoryItems) {
            if (i < 9 || i > 44) continue
            val row = (i / 9) - 1
            val column = i % 9
            initialBoard[column][row] = stack.toColor()
        }
        currentBoard = DnaBoard(initialBoard)
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        val board = currentBoard ?: return

        val currentSwap = board.swaps.last()
        val slot1 = currentSwap.first.first + (currentSwap.first.second + 1) * 9
        val slot2 = currentSwap.second.first + (currentSwap.second.second + 1) * 9
        InventoryUtils.getSlotAtIndex(slot1)?.highlight(LorenzColor.GREEN)
        InventoryUtils.getSlotAtIndex(slot2)?.highlight(LorenzColor.GREEN)
    }

    private data class DnaBoard(val initialBoard: List<MutableList<Colors>>) {
        val start = initialBoard.first()
        val end = initialBoard.last()
        val middleColumns = initialBoard.drop(1).dropLast(1).toMutableList()
        var swapsNeeded: Int
        var swaps: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>

        init {
            val solved = solveBoard()
            swapsNeeded = solved.first
            swaps = solved.second
        }

        fun solveBoard(): Pair<Int, List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> {
            val dp = Array(MID_COLUMNS_SIZE) { IntArray(ROW_PERMUTATIONS.size) { UNREACHABLE } }
            val parent = Array(MID_COLUMNS_SIZE) { IntArray(ROW_PERMUTATIONS.size) { -1 } }

            val cost = Array(MID_COLUMNS_SIZE) { IntArray(ROW_PERMUTATIONS.size) }
            val swapMap = Array(MID_COLUMNS_SIZE) { Array(ROW_PERMUTATIONS.size) { emptyList<Pair<Int, Int>>() } }

            for (c in 0..<MID_COLUMNS_SIZE) {
                for (p in ROW_PERMUTATIONS.indices) {
                    val perm = ROW_PERMUTATIONS[p].map { middleColumns[c][it] }
                    val (cst, sw) = getMinimumColumnSwaps(middleColumns[c], perm)
                    cost[c][p] = cst
                    swapMap[c][p] = sw
                }
            }

            for (p in ROW_PERMUTATIONS.indices) {
                val perm = ROW_PERMUTATIONS[p].map { middleColumns[0][it] }
                if (canColumnsConnect(start, perm)) {
                    dp[0][p] = cost[0][p]
                }
            }

            for (c in 1..<MID_COLUMNS_SIZE) {
                for (p in ROW_PERMUTATIONS.indices) {
                    val cur = ROW_PERMUTATIONS[p].map { middleColumns[c][it] }
                    for (q in ROW_PERMUTATIONS.indices) {
                        if (dp[c - 1][q] == UNREACHABLE) continue
                        val prev = ROW_PERMUTATIONS[q].map { middleColumns[c - 1][it] }
                        if (canColumnsConnect(prev, cur)) {
                            val newCost = dp[c - 1][q] + cost[c][p]
                            if (newCost < dp[c][p]) {
                                dp[c][p] = newCost
                                parent[c][p] = q
                            }
                        }
                    }
                }
            }

            var best = UNREACHABLE
            var last = -1
            for (p in ROW_PERMUTATIONS.indices) {
                val perm = ROW_PERMUTATIONS[p].map { middleColumns.last()[it] }
                if (dp[MID_COLUMNS_SIZE - 1][p] < UNREACHABLE && canColumnsConnect(perm, end)) {
                    if (dp[MID_COLUMNS_SIZE - 1][p] < best) {
                        best = dp[MID_COLUMNS_SIZE - 1][p]
                        last = p
                    }
                }
            }

            val result = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
            var col = MID_COLUMNS_SIZE - 1
            var cur = last

            while (col >= 0) {
                for ((a, b) in swapMap[col][cur]) {
                    result += (col + 1 to a) to (col + 1 to b)
                }
                cur = parent[col][cur]
                col--
            }

            return best to result
        }

        companion object {
            private const val MID_COLUMNS_SIZE = 7
            private const val ROWS = 4
            private const val UNREACHABLE = 1_000

            val ROW_PERMUTATIONS: List<List<Int>> by lazy {
                val perms = mutableListOf<List<Int>>()
                generateRowPermutations(perms, mutableListOf(0, 1, 2, 3), 0)
                perms
            }

            private fun generateRowPermutations(perms: MutableList<List<Int>>, a: MutableList<Int>, l: Int) {
                if (l == ROWS) perms += a.toList()
                else for (i in l..<ROWS) {
                    a[l] = a[i].also { a[i] = a[l] }
                    generateRowPermutations(perms, a, l + 1)
                    a[l] = a[i].also { a[i] = a[l] }
                }
            }

            private fun canColumnsConnect(a: List<Colors>, b: List<Colors>): Boolean {
                for (r in 0..<ROWS) {
                    val v = a[r]
                    if (b[r] == v) continue
                    if (r > 0 && b[r - 1] == v) continue
                    if (r < ROWS - 1 && b[r + 1] == v) continue
                    return false
                }
                return true
            }

            private fun getMinimumColumnSwaps(from: List<Colors>, to: List<Colors>): Pair<Int, List<Pair<Int, Int>>> {
                val pos = IntArray(ROWS)
                for (i in 0..<ROWS) pos[from.indexOf(to[i])] = i

                val visited = BooleanArray(ROWS)
                val swaps = mutableListOf<Pair<Int, Int>>()
                var cost = 0

                for (i in 0..<ROWS) {
                    if (visited[i]) continue
                    var cur = i
                    val cycle = mutableListOf<Int>()
                    while (!visited[cur]) {
                        visited[cur] = true
                        cycle.add(cur)
                        cur = pos[cur]
                    }
                    if (cycle.size > 1) {
                        cost += cycle.size - 1
                        for (k in 1..<cycle.size) {
                            swaps += cycle[0] to cycle[k]
                        }
                    }
                }
                return cost to swaps
            }
        }
    }

    private enum class Colors {
        RED,
        GREEN,
        BLUE,
        YELLOW;

        companion object {
            fun ItemStack.toColor(): Colors {
                val name = this.displayName
                if (name.startsWith("§cDNA")) {
                    return RED
                } else if (name.startsWith("§eDNA")) {
                    return YELLOW
                } else if (name.startsWith("§9DNA")) {
                    return BLUE
                } else if (name.startsWith("§aDNA")) {
                    return GREEN
                }
                ErrorManager.skyHanniError("unknown color", "name" to name)
            }
        }
    }

    private fun isEnabled() = config.enabled && inInventory
}
