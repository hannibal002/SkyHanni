package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.garden.greenhouse.DnaAnalyzerSolver.Colors.Companion.toColor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object DnaAnalyzerSolver {

    private val config get() = SkyHanniMod.feature.garden.dnaAnalyzerSolver

    private var inInventory = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory = event.inventoryName.endsWith(" DNA")
    }

    @HandleEvent
    fun onInventoryClose() {
        inInventory = false
        currentBoard = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled() || !config.hideTooltips) return
        if (event.slot?.containerSlot !in 9..44) return
        event.cancel()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (event.slotId == 49 && config.blockAccidentalClosing) {
            event.cancel()
            return
        }
        if (config.useMiddleClick) event.makePickblock()
    }

    private var currentBoard: DnaBoard? = null

    @HandleEvent(onlyOnSkyblock = true)
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

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        val board = currentBoard ?: return

        val currentSwap = board.swaps.last()
        val slot1 = currentSwap.first.first + (currentSwap.first.second + 1) * 9
        val slot2 = currentSwap.second.first + (currentSwap.second.second + 1) * 9
        InventoryUtils.getSlotAtIndex(slot1)?.highlight(LorenzColor.GREEN)
        InventoryUtils.getSlotAtIndex(slot2)?.highlight(LorenzColor.GREEN)
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        dnaSolverAllowsEnds = data.dnaSolverAllowsEnds
    }

    private var dnaSolverAllowsEnds = false

    private data class DnaBoard(val initialBoard: List<MutableList<Colors>>) {
        var swapsNeeded: Int
        var swaps: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>

        init {
            val solved = solveBoard(dnaSolverAllowsEnds)
            swapsNeeded = solved.first
            swaps = solved.second
        }

        private fun solveBoard(allowEnds: Boolean): Pair<Int, List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> {
            val firstMutable = if (allowEnds) 0 else 1
            val lastMutable = if (allowEnds) initialBoard.lastIndex else initialBoard.lastIndex - 1
            val mutableCount = lastMutable - firstMutable + 1

            val dp = Array(mutableCount) { IntArray(ROW_PERMUTATIONS.size) { UNREACHABLE } }
            val parent = Array(mutableCount) { IntArray(ROW_PERMUTATIONS.size) { -1 } }

            val cost = Array(mutableCount) { IntArray(ROW_PERMUTATIONS.size) }
            val swapMap = Array(mutableCount) { Array(ROW_PERMUTATIONS.size) { emptyList<Pair<Int, Int>>() } }

            for (i in 0..<mutableCount) {
                val colIndex = firstMutable + i
                val column = initialBoard[colIndex]

                for (p in ROW_PERMUTATIONS.indices) {
                    val perm = ROW_PERMUTATIONS[p].map { column[it] }
                    val (cst, sw) = getMinimumColumnSwaps(column, perm)
                    cost[i][p] = cst
                    swapMap[i][p] = sw
                }
            }

            for (p in ROW_PERMUTATIONS.indices) {
                val perm = ROW_PERMUTATIONS[p].map { initialBoard[firstMutable][it] }
                if (!allowEnds && !canColumnsConnect(initialBoard[0], perm)) continue
                dp[0][p] = cost[0][p]
            }

            for (i in 1..<mutableCount) {
                for (p in ROW_PERMUTATIONS.indices) {
                    val cur = ROW_PERMUTATIONS[p].map { initialBoard[firstMutable + i][it] }
                    for (q in ROW_PERMUTATIONS.indices) {
                        if (dp[i - 1][q] == UNREACHABLE) continue
                        val prev = ROW_PERMUTATIONS[q].map { initialBoard[firstMutable + i - 1][it] }
                        if (canColumnsConnect(prev, cur)) {
                            val newCost = dp[i - 1][q] + cost[i][p]
                            if (newCost < dp[i][p]) {
                                dp[i][p] = newCost
                                parent[i][p] = q
                            }
                        }
                    }
                }
            }

            var best = UNREACHABLE
            var last = -1

            for (p in ROW_PERMUTATIONS.indices) {
                val perm = ROW_PERMUTATIONS[p].map { initialBoard[lastMutable][it] }
                if (!allowEnds && !canColumnsConnect(perm, initialBoard.last())) continue
                if (dp[mutableCount - 1][p] < best) {
                    best = dp[mutableCount - 1][p]
                    last = p
                }
            }

            val result = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
            var i = mutableCount - 1
            var cur = last

            while (i >= 0) {
                val colIndex = firstMutable + i
                for ((a, b) in swapMap[i][cur]) {
                    result += (colIndex to a) to (colIndex to b)
                }
                cur = parent[i][cur]
                i--
            }

            return best to result
        }

        companion object {
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
                val name = this.hoverName.formattedTextCompatLeadingWhiteLessResets()
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
