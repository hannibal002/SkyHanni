package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import java.awt.Color

@SkyHanniModule
object CarnivalFruitDigging {

    private val config get() = SkyHanniMod.feature.event.carnival.fruitDigging

    private const val GRID_LENGTH = 7
    const val MAX_DIGS = 15

    private var isPlayingFruitDigging = false
    private var lastSquareDug: GamePos? = null
    private var remainingFruit = Fruit.entries.associateWith { it.count }.toMutableMap()

    private val solver = FruitDiggingSolver(GRID_LENGTH)
    private var recommendation: FruitDiggingSolver.Recommendation? = null
    private var solverDirty = false
    private var digsUsed = 0

    private val patternGroup = RepoPattern.group("event.carnival")

    /**
     * REGEX-TEST: [NPC] Carnival Pirateman: Good luck, matey!
     */
    private val startPattern by patternGroup.pattern(
        "fruitdigging.started",
        "^\\[NPC] Carnival Pirateman: Good luck, matey!$",
    )

    /**
     * WRAPPED-REGEX-TEST: "                               Fruit Digging"
     */
    private val endPattern by patternGroup.pattern(
        "fruitdigging.end",
        " {31}Fruit Digging",
    )

    /**
     * REGEX-TEST: TREASURE! There is a Durian nearby.
     * REGEX-TEST: TREASURE! There is an Apple nearby.
     */
    private val treasurePattern by patternGroup.pattern(
        "fruitdigging.treasure",
        "^TREASURE! There is an? (?<fruit>.*) nearby\\.$",
    )

    /**
     * REGEX-TEST: TREASURE! There are no fruits nearby!
     * REGEX-TEST: ANCHOR! There are no fruits nearby!
     */
    private val noFruitsNearbyPattern by patternGroup.pattern(
        "fruitdigging.nofruitsnearby",
        "^(?:TREASURE|ANCHOR)! There are no fruits nearby!$",
    )

    /**
     * REGEX-TEST: MINES! There is 1 bomb hidden nearby.
     * REGEX-TEST: MINES! There are 2 bombs hidden nearby.
     */
    private val minesPattern by patternGroup.pattern(
        "fruitdigging.mines",
        "^MINES! There (?:is|are) (?<bombs>\\d+) bombs? hidden nearby\\.$",
    )

    /**
     * REGEX-TEST: Pomegranate (+300)
     * REGEX-TEST: Bomb
     * REGEX-TEST: Rum
     */
    private val revealFruitPattern by patternGroup.pattern(
        "fruitdigging.reveal",
        "^(?<name>[A-Za-z ]+)(?: \\(\\+\\d+\\))?$",
    )

    enum class Fruit(val inGameName: String, val points: Int, val count: Int, val textureKey: String = "", val isEdible: Boolean = true) {
        UNKNOWN("Unknown", 0, 0, "", false),
        NO_FRUIT("No Fruit", 0, 0, "", false),
        BOMB("Bomb", 0, 10, "CARNIVAL_BOMB", false),
        RUM("Rum", 0, 5, "CARNIVAL_RUM", false),
        MANGO("Mango", 300, 10, "CARNIVAL_MANGO"),
        APPLE("Apple", 100, 8, "CARNIVAL_APPLE"),
        WATERMELON("Watermelon", 100, 4, "CARNIVAL_WATERMELON"),
        POMEGRANATE("Pomegranate", 200, 4, "CARNIVAL_POMEGRANATE"),
        COCONUT("Coconut", 200, 3, "CARNIVAL_COCONUT"),
        CHERRY("Cherry", 200, 2, "CARNIVAL_CHERRY"),
        DURIAN("Durian", 800, 2, "CARNIVAL_DURIAN"),
        DRAGON_FRUIT("Dragonfruit", 1200, 1, "CARNIVAL_DRAGON_FRUIT"),
        ;

        private var cachedTextureId: String? = null

        private val textureId: String
            get() {
                cachedTextureId?.let { return it }

                if (textureKey.isEmpty()) return ""
                val id = SkullTextureHolder.getTexture(textureKey)
                    ?.let(StringUtils::decodeBase64)
                    ?.substringAfterLast("/texture/")
                    ?.substringBefore("\"")
                    ?: return ""

                cachedTextureId = id
                return id
            }

        fun getAmountDugSoFar(): Int {
            return count - (remainingFruit[this] ?: 0)
        }

        fun getPointValue(): Int {
            val lastDug = gameGrid.getLastDug()
            var multiplier = 1.0
            if (lastDug == POMEGRANATE) multiplier = 1.5
            else if (lastDug == DURIAN) multiplier = 0.5

            if (this == APPLE) {
                return (multiplier * (points + 100 * getAmountDugSoFar())).toInt()
            }
            if (this == CHERRY) {
                return (multiplier * (points + 300 * getAmountDugSoFar())).toInt()
            }
            return (multiplier * points).toInt()
        }

        fun getNameWithPointValue(): String {
            return inGameName + " (" + getPointValue() + ")"
        }

        companion object {
            fun fromTexture(texture: String): Fruit? {
                return Fruit.entries.find { it.textureId.isNotEmpty() && texture.contains(it.textureId) }
            }

            fun fromName(name: String): Fruit? {
                return Fruit.entries.find { it.inGameName.equals(name, ignoreCase = true) }
            }
        }
    }

    private class Cell {
        var adjacentMines: Int? = null
        var treasureFruit: Fruit = Fruit.UNKNOWN
        var solvedFruit: Fruit = Fruit.UNKNOWN
        var anchoredFruit: Fruit = Fruit.UNKNOWN
        var uncoveredFruit: Fruit = Fruit.UNKNOWN
        var isDiggable: Boolean = true
        var removedFromRemaining: Boolean = false

        fun getFoundFruit(): Fruit = if (uncoveredFruit != Fruit.UNKNOWN) uncoveredFruit else solvedFruit

        fun toSolverInput(): FruitDiggingSolver.CellInput {
            val content = when {
                uncoveredFruit != Fruit.UNKNOWN -> uncoveredFruit
                solvedFruit != Fruit.UNKNOWN -> solvedFruit
                else -> null
            }
            return FruitDiggingSolver.CellInput(
                content = content,
                diggable = isDiggable,
                // not diggable with no known content -> a fruit destroyed by a bomb
                ghost = !isDiggable && content == null,
                mustFruit = false,
                minesCount = adjacentMines,
                treasure = treasureFruit.takeIf { it != Fruit.UNKNOWN },
                anchor = anchoredFruit.takeIf { it != Fruit.UNKNOWN },
            )
        }
    }

    private class GameGrid {
        private val cells = Array(GRID_LENGTH) { Array(GRID_LENGTH) { Cell() } }

        operator fun get(pos: GamePos): Cell = cells[pos.row][pos.col]

        operator fun get(vec: LorenzVec): Cell? {
            val pos = GamePos.fromLorenzVec(vec) ?: return null
            return this[pos]
        }

        fun getLastDug(): Fruit {
            val lastDugPos = lastSquareDug ?: return Fruit.NO_FRUIT
            return this[lastDugPos].uncoveredFruit
        }
    }

    private var gameGrid = GameGrid()

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onGuiRenderOverlay() {
        if (!isEnabled()) return

        if (config.remainingFruitDisplay) {
            config.remainingFruitPosition.renderRenderables(buildRemainingFruitDisplay(), posLabel = "Remaining Fruit")
        }

        if (config.displayBestDig) {
            val display = buildSolverDisplay()
            if (display.isNotEmpty()) {
                config.bestDigPosition.renderRenderables(display, posLabel = "Fruit Digging Solver")
            }
        }
    }

    private fun buildSolverDisplay(): List<Renderable> {
        val rec = recommendation ?: return emptyList()
        val pos = GamePos(rec.targetRow, rec.targetCol)
        return listOf(
            Renderable.text("§6§lFruit Digging Solver"),
            Renderable.text("§7Rounds: §e$digsUsed§7/§e$MAX_DIGS"),
            Renderable.text("§aDig: §f${pos.label()}"),
            Renderable.text("§7Use: §aCarnival Shovel §8(§c${rec.shovel}§8)"),
            Renderable.text("§cBomb: §e${rec.pBomb.formatPercentage()} §7Samples: §e${rec.samples}"),
        )
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (solverDirty) {
            updateRecommendation()
            solverDirty = false
        }

        for (row in 0 until GRID_LENGTH) {
            for (col in 0 until GRID_LENGTH) {
                val pos = GamePos(row, col)
                val cell = gameGrid[pos]

                // indicate if not diggable
                if (!cell.isDiggable) {
                    val aabb = AABB(pos.toBlockPos()).expandTowards(0.0, 0.1, 0.0)
                    event.drawFilledBoundingBox(aabb, Color.GRAY, seeThroughBlocks = false)
                }

                val label = mutableListOf<Pair<String, Color>>()

                // label uncovered fruit
                if (config.displayFoundFruit && !cell.isDiggable) {
                    val foundFruit = cell.getFoundFruit()
                    if (foundFruit != Fruit.UNKNOWN)
                        label.add(Pair(foundFruit.inGameName, config.foundColor.toColor()))
                }

                // label solved fruit if not dug or destroyed
                if (config.displayFruitGuesses && cell.uncoveredFruit == Fruit.UNKNOWN && cell.isDiggable) {
                    if (cell.solvedFruit != Fruit.UNKNOWN)
                        label.add(Pair(cell.solvedFruit.getNameWithPointValue(), config.fruitGuessColor.toColor()))
                }

                // label treasure
                if (config.displayAdjacentTreasure) {
                    if (cell.treasureFruit != Fruit.UNKNOWN)
                        label.add(Pair("≤ ${cell.treasureFruit.inGameName}", config.adjacentColor.toColor()))
                    if (cell.anchoredFruit != Fruit.UNKNOWN && cell.anchoredFruit != cell.treasureFruit)
                        label.add(Pair("≥ ${cell.anchoredFruit.inGameName}", config.adjacentColor.toColor()))
                }

                // label num of adjacent mines
                if (config.displayAdjacentMines) {
                    cell.adjacentMines?.let { label.add(Pair(it.toString(), config.minesColor.toColor())) }
                }

                if (label.isEmpty()) continue
                val textPos = pos.toLorenzVec().add(0.5, 1.1, 0.5)
                val yOffsetStart = -5f * (label.size - 1)
                label.forEachIndexed { i, (text, color) ->
                    event.drawString(textPos, text, seeThroughBlocks = true, yOffset = yOffsetStart + i * 10, color = color, scale = .3)
                }
            }
        }

        // highlight the solver's recommended next dig
        if (config.displayBestDig) {
            recommendation?.let { rec ->
                val pos = GamePos(rec.targetRow, rec.targetCol)
                if (gameGrid[pos].isDiggable) {
                    val vec = pos.toLorenzVec()
                    event.drawWaypointFilled(vec, config.bestDigColor.toColor(), seeThroughBlocks = true, minimumAlpha = 0.35f)
                    event.drawDynamicText(
                        vec.add(0.5, 1.35, 0.5),
                        "§aDig §cHERE §7with §8(§c${rec.shovel}§8) §7${rec.pBomb.formatPercentage()} bomb",
                        1.35,
                        seeThroughBlocks = true,
                    )
                }
            }
        }
    }

    private fun updateRecommendation() {
        val grid = Array(GRID_LENGTH) { row ->
            Array(GRID_LENGTH) { col -> gameGrid[GamePos(row, col)].toSolverInput() }
        }
        val lastDug = gameGrid.getLastDug()
        val nextMultiplier = when (lastDug) {
            Fruit.POMEGRANATE -> 1.5
            Fruit.DURIAN -> 0.5
            else -> 1.0
        }
        recommendation = solver.recommend(grid, digsUsed, nextMultiplier, coconutProtection = lastDug == Fruit.COCONUT)
    }

    @HandleEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        val blockOld = event.oldState
        val blockNew = event.newState

        if (blockOld.block != Blocks.SAND) return
        val pos = GamePos.fromLorenzVec(event.location) ?: return
        val cell = gameGrid[pos]
        cell.isDiggable = false

        if (blockNew.block == Blocks.SANDSTONE) {
            lastSquareDug = pos
            digsUsed++
        } else if (blockNew.block == Blocks.SANDSTONE_STAIRS) {
            updateRemainingFruit(cell, cell.solvedFruit)
        }
        solverDirty = true
    }

    @HandleEvent
    fun onDataWatcherUpdate(event: DataWatcherUpdatedEvent<ItemEntity>) {
        if (!isEnabled()) return
        handleAnchor(event.entity)
    }

    private fun handleAnchor(entity: ItemEntity) {
        val solvedPos = GamePos.fromLorenzVec(entity.position().toLorenzVec()) ?: return
        val solvedCell = gameGrid[solvedPos]
        val dugCell = lastSquareDug?.let { gameGrid[it] } ?: return

        val itemStack = entity.item
        val textureHash = itemStack.getSkullTexture()?.let {
            runCatching { StringUtils.decodeBase64(it) }.getOrNull()
        } ?: return

        val fruit = Fruit.fromTexture(textureHash) ?: return

        var updated = false
        if (solvedCell.solvedFruit == Fruit.UNKNOWN) {
            solvedCell.solvedFruit = fruit
            updateRemainingFruit(solvedCell, fruit)
            updated = true
        }
        if (solvedCell.isDiggable && dugCell.anchoredFruit == Fruit.UNKNOWN) {
            dugCell.anchoredFruit = fruit
            updated = true
        }
        if (updated) solverDirty = true
    }

    @HandleEvent
    fun onEntityNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!isEnabled()) return

        // Armor stand appears when a fruit is dug or exposed by watermelon
        val entity = event.entity
        val name = event.newName?.removeColor() ?: return
        if (name.isBlank()) return

        val pos = entity.blockPosition().toLorenzVec()
        val gamePos = GamePos.fromLorenzVec(pos) ?: return

        revealFruitPattern.matchMatcher(name) {
            val fruitName = group("name")
            val fruit = Fruit.fromName(fruitName) ?: return
            val cell = gameGrid[gamePos]
            updateRemainingFruit(cell, fruit)
            cell.uncoveredFruit = fruit
            solverDirty = true
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled || !CarnivalAPI.inCarnivalArea) return

        val message = event.cleanMessage

        if (startPattern.matches(message)) {
            startGame()
            return
        }
        if (endPattern.matches(message)) {
            resetData()
            return
        }

        minesPattern.matchMatcher(message) {
            val bombs = group("bombs").toInt()
            lastSquareDug?.let {
                gameGrid[it].adjacentMines = bombs
                solverDirty = true
            }
            return
        }

        treasurePattern.matchMatcher(message) {
            val fruitName = group("fruit")
            val fruit = Fruit.entries.find { it.inGameName == fruitName } ?: return
            lastSquareDug?.let {
                gameGrid[it].treasureFruit = fruit
                solverDirty = true
            }
            return
        }

        if (noFruitsNearbyPattern.matches(message)) {
            lastSquareDug?.let {
                val cell = gameGrid[it]
                cell.treasureFruit = Fruit.NO_FRUIT
                cell.anchoredFruit = Fruit.NO_FRUIT
                solverDirty = true
            }
            return
        }
    }

    @HandleEvent
    fun onWorldChange() {
        resetData()
    }

    fun resetData() {
        isPlayingFruitDigging = false
        gameGrid = GameGrid()
        remainingFruit = Fruit.entries.associateWith { it.count }.toMutableMap()
        lastSquareDug = null
        recommendation = null
        solverDirty = false
        digsUsed = 0
    }

    private fun startGame() {
        resetData()
        isPlayingFruitDigging = true
        // compute an opening recommendation before the first dig
        solverDirty = true
    }

    private fun updateRemainingFruit(cell: Cell, fruit: Fruit) {
        if (fruit == Fruit.UNKNOWN || fruit == Fruit.NO_FRUIT) return
        if (!cell.removedFromRemaining && !cell.isDiggable) {
            val count = remainingFruit[fruit] ?: return
            remainingFruit[fruit] = count - 1
            cell.removedFromRemaining = true
        }
    }

    private fun buildRemainingFruitDisplay(): List<Renderable> {
        val fruitLines = Fruit.entries
            .filter { (remainingFruit[it] ?: 0) > 0 }
            .sortedWith(compareByDescending<Fruit> { it.getPointValue() }.thenBy { it.inGameName })
            .map { fruit -> Renderable.text("${fruit.getNameWithPointValue()}: ${remainingFruit[fruit]}") }

        if (fruitLines.isEmpty()) return emptyList()
        return listOf(Renderable.text("Fruit Digging")) + fruitLines
    }

    private data class GamePos(val row: Int, val col: Int) {
        fun isValid() = row in 0..<GRID_LENGTH && col in 0..<GRID_LENGTH

        fun label() = "${'A' + row}${col + 1}"

        fun toLorenzVec() = LorenzVec(START_X + col, GRID_Y, START_Z + row)

        fun toBlockPos() = BlockPos((START_X + col).toInt(), GRID_Y.toInt(), (START_Z + row).toInt())

        companion object {
            const val START_X = -112.0
            const val START_Z = 19.0
            const val GRID_Y = 72.0

            fun fromLorenzVec(vec: LorenzVec): GamePos? {
                val col = (vec.x - START_X).toInt()
                val row = (vec.z - START_Z).toInt()
                return GamePos(row, col).takeIf { it.isValid() }
            }
        }
    }

    private fun isEnabled() = config.enabled && CarnivalAPI.inCarnivalArea && isPlayingFruitDigging
}
