package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import java.awt.Color

@SkyHanniModule
object CarnivalFruitDigging {

    private val config get() = SkyHanniMod.feature.event.carnival.fruitDigging

    private const val GRID_LENGTH = 7
    private const val TOTAL_TURNS = 15

    private var isPlayingFruitDigging = false
    private var lastRevealedSquare: GamePos? = null

    private var fruitToAmountRemaining = Fruit.entries.associateWith { it.count }.toMutableMap()
    private var fruitToAmountObtained = Fruit.entries.associateWith { 0 }.toMutableMap()

    private var turnsSoFar: Int? = null

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

    /**
     * REGEX-TEST: §fFruits: §a2§7/§c15
     * REGEX-TEST: Fruits: §a2§7/§c15
     */
    private val scoreboardFruitsPattern by patternGroup.pattern(
        "fruitdigging.scoreboard.fruits",
        "^(?:§f)?Fruits: §.(?<blocksDug>\\d+)§./§.15$",
    )

    enum class Fruit(val inGameName: String, val points: Int, val count: Int, val textureId: String = "", val isEdible: Boolean = true) {
        UNKNOWN("Unknown", 0, 0, "", false),
        NO_FRUIT("No Fruit", 0, 0, "", false),
        BOMB("Bomb", 0, 10, "a76a2811d1e176a07b6d0a657b910f134896ce30850f6e80c7c83732d85381ea", false),
        RUM("Rum", 0, 5, "407b275d28b927b1bf7f6dd9f45fbdad2af8571c54c8f027d1bff6956fbf3c16", false),
        MANGO("Mango", 300, 10, "f363a62126a35537f8189343a22660de75e810c6ac004a7d3da65f1c040a839"),
        APPLE("Apple", 100, 8, "17ea278d6225c447c5943d652798d0bbbd1418434ce8c54c54fdac79994ddd6c"),
        WATERMELON("Watermelon", 100, 4, "efe4ef83baf105e8dee6cf03dfe7407f1911b3b9952c891ae34139560f2931d6"),
        POMEGRANATE("Pomegranate", 200, 4, "40824d18079042d5769f264f44394b95b9b99ce689688cc10c9eec3f882ccc08"),
        COCONUT("Coconut", 200, 3, "10ceb1455b471d016a9f06d25f6e468df9fcf223e2c1e4795b16e84fcca264ee"),
        CHERRY("Cherry", 200, 2, "c92b099a62cd2fbf8ada09dec145c75d7fda4dc57b968bea3a8fa11e37aa48b2"),
        DURIAN("Durian", 800, 2, "ac268d36c2c6047ffeec00124096376b56dbb4d756a55329363a1b27fcd659cd"),
        DRAGON_FRUIT("Dragonfruit", 1200, 1, "3cc761bcb0579763d9b8ab6b7b96fa77eb6d9605a804d838fec39e7b25f95591");

        fun allFruitsWorthMore(): List<Fruit> = entries.filter { it.isEdible && it.points > this.points }

        fun allFruitsWorthLess(): List<Fruit> = entries.filter { it.isEdible && it.points < this.points }

        fun getAmountObtained(): Int {
            return fruitToAmountObtained[this] ?: 0
        }

        fun getPointValue(): Int {
            val lastRevealed = gameGrid.getLastRevealed()
            var multiplier = 1.0
            if (lastRevealed == POMEGRANATE) multiplier = 1.5
            else if (lastRevealed == DURIAN) multiplier = 0.5

            if (this == APPLE) {
                return (multiplier * (points + 100 * getAmountObtained())).toInt()
            }
            if (this == CHERRY) {
                return (multiplier * (points + 300 * getAmountObtained())).toInt()
            }
            return (multiplier * points).toInt()
        }

        fun getNameWithPointValue(): String {
            if (!isEdible) {
                return inGameName
            }
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
        var solvedFruit: Fruit = Fruit.UNKNOWN // if we know with certainty what this block is (anchor)
        var anchoredFruit: Fruit = Fruit.UNKNOWN // dousing output
        var treasureFruit: Fruit = Fruit.UNKNOWN // dousing output
        var treasureFulfilled: Boolean = false // the treasure fruit was found after the dousing

        var uncoveredFruit: Fruit = Fruit.UNKNOWN // this fruit as revealed by digging/watermelon
        var isDiggable: Boolean = true
        var removedFromRemaining: Boolean = false // counted toward map of remaining fruits
        var obtained: Boolean = false // dug or revealed by watermelon

        fun getFoundFruit(): Fruit = if (uncoveredFruit != Fruit.UNKNOWN) uncoveredFruit else solvedFruit
    }

    private class GameGrid {
        private val cells = Array(GRID_LENGTH) { Array(GRID_LENGTH) { Cell() } }

        operator fun get(pos: GamePos): Cell = cells[pos.row][pos.col]

        operator fun get(vec: LorenzVec): Cell? {
            val pos = GamePos.fromLorenzVec(vec) ?: return null
            return this[pos]
        }

        fun getLastRevealed(): Fruit {
            val lastRevealedPos = lastRevealedSquare ?: return Fruit.NO_FRUIT
            return this[lastRevealedPos].uncoveredFruit
        }
    }

    private var gameGrid = GameGrid()

    @HandleEvent
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (!isEnabled()) return

        scoreboardFruitsPattern.firstMatcher(event.new) {
            turnsSoFar = group("blocksDug").toInt()
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onGuiRenderOverlay() {
        if (!isEnabled() || !config.remainingFruitDisplay) return
        config.remainingFruitPosition.renderRenderables(buildRemainingFruitDisplay(), posLabel = "Remaining Fruit")
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

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
                    if (cell.treasureFruit != Fruit.UNKNOWN) {
                        // F for treasure that is found
                        val prefix = if (cell.treasureFulfilled) "≤ (F)" else "≤"
                        label.add(Pair("$prefix ${cell.treasureFruit.inGameName}", config.adjacentColor.toColor()))
                    } else if (cell.anchoredFruit != Fruit.UNKNOWN) {
                        // anchor always tells exact location, so we don't need to "find" it
                        label.add(Pair("≥ (F) ${cell.anchoredFruit.inGameName}", config.adjacentColor.toColor()))
                    }
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
            lastRevealedSquare = pos
        } else if (blockNew.block == Blocks.SANDSTONE_STAIRS) {
            updateRemainingFruit(cell, cell.solvedFruit)
        }
    }

    @HandleEvent
    fun onDataWatcherUpdate(event: DataWatcherUpdatedEvent<Entity>) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity is ItemEntity) {
            handleAnchor(entity)
        }
    }

    private fun handleAnchor(entity: ItemEntity): Boolean {

        val solvedPos = GamePos.fromLorenzVec(entity.position().toLorenzVec()) ?: return false
        val solvedCell = gameGrid[solvedPos]
        val dugCell = lastRevealedSquare?.let { gameGrid[it] } ?: return false

        val itemStack = entity.item
        if (itemStack.item == Items.AIR) return false // TODO check if this is even needed

        val textureHash = itemStack.getSkullTexture()?.let {
            runCatching { StringUtils.decodeBase64(it) }.getOrNull()
        } ?: return false

        val fruit = Fruit.fromTexture(textureHash) ?: return false

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
        return updated
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
            updateObtainedFruit(gamePos, cell, fruit)
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
            lastRevealedSquare?.let {
                gameGrid[it].adjacentMines = bombs
            }
            return
        }

        treasurePattern.matchMatcher(message) {
            val fruitName = group("fruit")
            val fruit = Fruit.entries.find { it.inGameName == fruitName } ?: return
            lastRevealedSquare?.let {
                gameGrid[it].treasureFruit = fruit
            }
            return
        }

        if (noFruitsNearbyPattern.matches(message)) {
            lastRevealedSquare?.let {
                val cell = gameGrid[it]
                cell.treasureFruit = Fruit.NO_FRUIT
                cell.anchoredFruit = Fruit.NO_FRUIT
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
        fruitToAmountRemaining = Fruit.entries.associateWith { it.count }.toMutableMap()
        fruitToAmountObtained = Fruit.entries.associateWith { 0 }.toMutableMap()
        lastRevealedSquare = null
        turnsSoFar = null
    }

    private fun startGame() {
        resetData()
        isPlayingFruitDigging = true
    }

    private fun updateRemainingFruit(cell: Cell, fruit: Fruit) {
        if (fruit == Fruit.UNKNOWN || fruit == Fruit.NO_FRUIT) return
        if (!cell.removedFromRemaining && !cell.isDiggable) {
            val count = fruitToAmountRemaining[fruit] ?: return
            fruitToAmountRemaining[fruit] = count - 1
            cell.removedFromRemaining = true
        }
    }

    private fun updateObtainedFruit(pos: GamePos, cell: Cell, fruit: Fruit) {
        if (fruit == Fruit.UNKNOWN || fruit == Fruit.NO_FRUIT) return
        if (cell.obtained) return

        fruitToAmountObtained[fruit] = (fruitToAmountObtained[fruit] ?: 0) + 1
        cell.obtained = true

        pos.getAdjacent().forEach {
            val adjacentCell = gameGrid[it]
            if (fruit == Fruit.BOMB) {
                adjacentCell.adjacentMines = adjacentCell.adjacentMines?.minus(1)
            } else if (adjacentCell.treasureFruit == fruit) {
                adjacentCell.treasureFulfilled = true
            }
        }
    }

    private fun buildRemainingFruitDisplay(): List<Renderable> {
        val fruitLines = Fruit.entries
            .filter { (fruitToAmountRemaining[it] ?: 0) > 0 }
            .sortedWith(compareByDescending<Fruit> { it.getPointValue() }.thenBy { it.inGameName })
            .map { fruit -> Renderable.text("§a${fruit.getNameWithPointValue()}§f: §c${fruitToAmountRemaining[fruit]}") }

        if (fruitLines.isEmpty() && turnsSoFar == null) return emptyList()

        val lines = mutableListOf(Renderable.text("§a§lFruit Digging"))
        turnsSoFar?.let {
            lines.add(Renderable.text("Blocks Dug: $it/$TOTAL_TURNS"))
        }
        lines.addAll(fruitLines)
        return lines
    }

    private data class GamePos(val row: Int, val col: Int) {
        fun isValid() = row in 0..<GRID_LENGTH && col in 0..<GRID_LENGTH

        fun toLorenzVec() = LorenzVec(START_X + col, GRID_Y, START_Z + row)

        fun toBlockPos() = BlockPos((START_X + col).toInt(), GRID_Y.toInt(), (START_Z + row).toInt())

        fun getAdjacent(): List<GamePos> {
            val list = mutableListOf<GamePos>()
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val pos = GamePos(row + dr, col + dc)
                    if (pos.isValid()) list.add(pos)
                }
            }
            return list
        }

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
