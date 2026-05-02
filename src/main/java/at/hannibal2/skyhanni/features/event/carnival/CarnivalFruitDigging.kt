package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object CarnivalFruitDigging {

    private val config get() = SkyHanniMod.feature.event.carnival.fruitDigging

    private const val GRID_LENGTH = 7

    private var isPlayingFruitDigging = false
    private var lastSquareDug: GamePos? = null

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
     */
    private val noTreasurePattern by patternGroup.pattern(
        "fruitdigging.notreasure",
        "^TREASURE! There are no fruits nearby!$",
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

    // use Spanish names to prevent text from overflowing the block
    enum class Fruit(val inGameName: String, val points: Int, val count: Int, val isEdible: Boolean = true) {
        UNKNOWN("Unknown", 0, 0, false),
        NO_FRUIT("No Fruit", 0, 0, false),
        BOMB("Bomb", 0, 10, false),
        RUM("Rum", 0, 5, false),
        MANGO("Mango", 300, 10),
        APPLE("Apple", 0, 8),
        WATERMELON("Watermelon", 100, 4),
        POMEGRANATE("Pomegranate", 200, 4),
        COCONUT("Coconut", 200, 3),
        CHERRY("Cherry", 200, 2),
        DURIAN("Durian", 800, 2),
        DRAGON_FRUIT("Dragonfruit", 1200, 1);

        fun allFruitsWorthMore(): List<Fruit> = entries.filter { it.isEdible && it.points > this.points }

        fun allFruitsWorthLess(): List<Fruit> = entries.filter { it.isEdible && it.points < this.points }
    }

    private class Cell {
        var adjacentMines: Int? = null
        var adjacentTreasure: Fruit = Fruit.UNKNOWN
        var uncoveredFruit: Fruit = Fruit.UNKNOWN
    }

    private class GameGrid {
        private val cells = Array(GRID_LENGTH) { Array(GRID_LENGTH) { Cell() } }

        operator fun get(pos: GamePos): Cell = cells[pos.row][pos.col]

        operator fun get(vec: LorenzVec): Cell? {
            val pos = GamePos.fromLorenzVec(vec) ?: return null
            return this[pos]
        }
    }

    private var gameGrid = GameGrid()

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for (row in 0 until GRID_LENGTH) {
            for (col in 0 until GRID_LENGTH) {
                val pos = GamePos(row, col)
                val cell = gameGrid[pos]

                val worldPos = pos.toLorenzVec().add(0.5, 1.1, 0.5)
                val lines = mutableListOf<Pair<String, java.awt.Color>>()
                if (config.displayFoundFruit && cell.uncoveredFruit != Fruit.UNKNOWN) {
                    lines.add(Pair(cell.uncoveredFruit.inGameName, config.foundColor.toColor()))
                }
                if (config.displayAdjacentTreasure && cell.adjacentTreasure != Fruit.UNKNOWN) {
                    lines.add(Pair(cell.adjacentTreasure.inGameName, config.adjacentColor.toColor()))
                }
                if (config.displayAdjacentMines) {
                    cell.adjacentMines?.let { lines.add(Pair(it.toString(), config.minesColor.toColor())) }
                }

                if (lines.isEmpty()) continue
                val yOffsetStart = (lines.size - 1) * -5f
                lines.forEachIndexed { i, (text, color) ->
                    event.drawString(worldPos, text, seeThroughBlocks = true, yOffset = yOffsetStart + i * 10f,
                        color = color, scale = .3)
                }
            }
        }
    }

    @HandleEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        val blockOld = event.oldState
        val blockNew = event.newState
        if (blockOld.block == Blocks.SAND && blockNew.block == Blocks.SANDSTONE) {
            val pos = GamePos.fromLorenzVec(event.location) ?: return
            lastSquareDug = pos
        }
    }

    @HandleEvent
    fun onEntityNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!isEnabled()) return

        val entity = event.entity
        val name = event.newName?.removeColor() ?: return
        if (name.isBlank()) return

        val pos = entity.blockPosition().toLorenzVec()

        revealFruitPattern.matchMatcher(name) {
            val fruitName = group("name")
            val fruit = Fruit.entries.find { it.inGameName.equals(fruitName, ignoreCase = true) } ?: return@matchMatcher

            GamePos.fromLorenzVec(pos)?.let { gamePos ->
                gameGrid[gamePos].uncoveredFruit = fruit
            }
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled || !CarnivalAPI.inCarnivalArea) return

        val message = event.cleanMessage

        if (startPattern.matches(message)) {
            isPlayingFruitDigging = true
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
            }
            return
        }

        treasurePattern.matchMatcher(message) {
            val fruitName = group("fruit")
            val fruit = Fruit.entries.find { it.inGameName == fruitName } ?: return
            lastSquareDug?.let {
                gameGrid[it].adjacentTreasure = fruit
            }
            return
        }

        if (noTreasurePattern.matches(message)) {
            lastSquareDug?.let {
                gameGrid[it].adjacentTreasure = Fruit.NO_FRUIT
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
    }

    private data class GamePos(val row: Int, val col: Int) {
        fun isValid() = row in 0..<GRID_LENGTH && col in 0..<GRID_LENGTH

        fun toLorenzVec() = LorenzVec(START_X + col, GRID_Y, START_Z + row)

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
