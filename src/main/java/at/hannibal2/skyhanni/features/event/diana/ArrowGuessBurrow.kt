package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi.isDianaSpade
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.block.Blocks
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ArrowGuessBurrow {
    private val config get() = SkyHanniMod.feature.event.diana

    private const val SHAFT_LENGTH = 20
    private const val PARTICLE_DETECTION_TOLERANCE = 0.12
    private const val COUNT_NEAR_TIP = 4
    private const val COUNT_NEAR_BASE = 2
    private const val EPSILON = 1e-6

    private val points: MutableSet<LorenzVec> = mutableSetOf()
    private val recentArrowParticles = TimeLimitedSet<LorenzVec>(1.minutes)
    private val allGuesses = mutableListOf<GuessEntry>()

    private var newArrow = true

    // deific spade insta breaks grass and hits the block behind it before chat messages if you left-click
    private val recentBlocksClicked = TimeLimitedSet<LorenzVec>(0.5.seconds)
    private var lastBurrowPos = emptyList<LorenzVec>()

    private val patternGroup = RepoPattern.group("event.diana.mythological.burrows")

    /**
     * REGEX-TEST: §eYou finished the Griffin burrow chain! §r§7(8/8)
     * REGEX-TEST: §eYou dug out a Griffin Burrow! §r§7(4/8)
     */
    private val burrowDugPattern by patternGroup.pattern(
        "burrow-dug-capture",
        "§eYou (?<type>finished the Griffin burrow chain!|dug out a Griffin Burrow!) §r§7\\((?<current>\\d+)/(?<max>\\d+)\\)"
    )

    data class GuessEntry(
        val guesses: List<LorenzVec>,
        private var currentIndex: Int = 0
    ) {
        fun getCurrent(): LorenzVec = guesses[currentIndex]
        fun contains(vec: LorenzVec): Boolean = guesses.contains(vec)
        fun moveToNext(): Boolean {
            GriffinBurrowHelper.removePreciseGuess(getCurrent())
            val nextIndex = currentIndex + 1
            if (nextIndex in guesses.indices) {
                currentIndex = nextIndex
                if (!isBlockValid(guesses[nextIndex])) {
                    return moveToNext()
                }
                GriffinBurrowHelper.newBurrow = true // spade is probably not pointing to the burrow we are moving
                BurrowGuessEvent(guesses[nextIndex], precise = true, new = true).post()
                return true
            } else return false
        }
        fun removeGuesses() { guesses.forEach { GriffinBurrowHelper.removePreciseGuess(it) } }
    }

    private object DebugSesh {
        var debugActive = false
        var renderAll = false
        var timeStarted = SimpleTimeMark.farPast()

        var guessesMade = 0
        var preciseGuesses = 0
        var couldNotFindGuess = 0

        fun printData() {
            val output =
                """
                |=== Arrow Guess Debug Session ===
                |Active: $debugActive
                |Running for: ${timeStarted.passedSince().format()}
                |Current size of allGuesses: ${allGuesses.size}
                |Bobby detected: ${PlatformUtils.isModInstalled("bobby")}
                |Current rendered guesses: ${GriffinBurrowHelper.guessCount}
                |
                |Statistics:
                |  Total guesses made: $guessesMade
                |  Precise guesses: $preciseGuesses
                |  Could not find guess: $couldNotFindGuess
                |  Precision rate: ${"%.1f".format(preciseGuesses * 100.0 / guessesMade)}%
                """.trimMargin()

            println(output)
        }

        fun clear() {
            printData()
            allGuesses.clear()
            timeStarted = SimpleTimeMark.now()
            renderAll = false
            guessesMade = 0
            preciseGuesses = 0
            couldNotFindGuess = 0
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        points.clear()
        newArrow = true

        lastBurrowPos.forEach { point ->
            val toRemove = allGuesses.filter { it.contains(point) }
            toRemove.forEach { it.removeGuesses() }
            allGuesses.removeAll(toRemove)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (DebugSesh.debugActive && DebugSesh.renderAll) {
            allGuesses.forEach { guessEntry ->
                val color = if (guessEntry.guesses.size == 1) LorenzColor.BLUE.toChromaColor()
                else LorenzColor.GREEN.toChromaColor()
                guessEntry.guesses.forEach { value ->
                    event.drawColor(value.up(2), color)
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onBlockClick(event: BlockClickEvent) {
        recentBlocksClicked.add(event.position)
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onIslandChange() {
        allGuesses.clear()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebugarrowguesssession") {
            description = "start, stop, or check status of a diana arrow guess debug session"
            category = CommandCategory.DEVELOPER_DEBUG

            literalCallback("start") {
                ChatUtils.chat("debug session started")
                if (!DebugSesh.debugActive) {
                    DebugSesh.debugActive = true
                    DebugSesh.timeStarted = SimpleTimeMark.now()
                    DebugSesh.printData()
                }
            }

            literalCallback("stop") {
                ChatUtils.chat("check your console/latestlog to see data")
                DebugSesh.debugActive = false
                DebugSesh.clear()
            }

            literalCallback("status") {
                ChatUtils.chat("check your console/latestlog to see data")
                DebugSesh.printData()
            }

            literalCallback("toggleRenderAllGuesses") {
                DebugSesh.renderAll = !DebugSesh.renderAll
                ChatUtils.chat("render all now ${DebugSesh.renderAll}")
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onChat(event: SkyHanniChatEvent) {
        if (burrowDugPattern.matches(event.message)) {
            val matcher = burrowDugPattern.matcher(event.message)
            if (matcher.find()) {
                val current = matcher.group("current").toInt()
                val max = matcher.group("max").toInt()
                lastBurrowPos = recentBlocksClicked.toList()
                recentBlocksClicked.forEach { onBurrowDug(it, current, max) }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!newArrow) return

        if (event.distanceToPlayer > 6) return
        if (event.type != ParticleTypes.DUST) return
        if (event.count != 0) return
        if (event.speed != 1.0f) return

        // offset is color for some reason
        val range = getArrowRange(event.offset) ?: return

        if (!recentArrowParticles.add(event.location)) return
        points.add(event.location)

        val arrow = detectArrow(points) ?: return
        newArrow = false
        points.clear()
        val guess = findClosestValidBlockToRayNew(arrow, range) ?: run {
            if (DebugSesh.debugActive) {
                DebugSesh.couldNotFindGuess++
            }
            if (config.warnIfInaccurateArrowGuess) {
                TitleManager.sendTitle("§eUse Spade", duration = 3.seconds)
            }
            return
        }

        // if you dig a burrow while its tracking particles it doesn't create a new waypoint I think but this is rare and non-fatal
        GriffinBurrowHelper.newBurrow = false
        BurrowGuessEvent(
            guess,
            precise = true,
            new = true
        ).post()

    }

    fun getArrowRange(offset: LorenzVec): IntRange? {
        return when (offset) {
            LorenzVec(0, 128, 0) -> IntRange(0, 117) // yellow
            LorenzVec(255, 255, 0) -> IntRange(112, 282) // red
            LorenzVec(255, 0, 0) -> IntRange(281, 600) // black
            else -> null
        }
    }

    fun checkMoveGuess(particleBurrows: Map<LorenzVec, BurrowType>) {
        val burrows = particleBurrows.filter { it.value != BurrowType.START }.map { it.key }
        for (guessEntry in allGuesses) {
            if (!isBlockValid(guessEntry.getCurrent())) guessEntry.moveToNext()

            val shouldBeLoaded = InventoryUtils.getItemInHandAtTime(SimpleTimeMark.now() - 0.5.seconds)?.isDianaSpade
            if (shouldBeLoaded == true &&
                !burrows.contains(guessEntry.getCurrent()) && // burrow is not found
                guessEntry.getCurrent().distanceSq(MinecraftCompat.localPlayer.blockPosition().toLorenzVec()) < 900 // within 30 blocks
            ) {
                if (guessEntry.moveToNext()) {

                    return
                }
            }
        }
    }

    fun onBurrowDug(location: LorenzVec, chainNumber: Int, maxChains: Int) {
        if (chainNumber != maxChains) {
            points.clear()
            newArrow = true
        }

        if (chainNumber != 1) {
            val toRemove = allGuesses.filter { it.contains(location) }
            toRemove.forEach { it.removeGuesses() } // remove any potential incorrect guesses on screen
            allGuesses.removeAll(toRemove)
        }
    }

    private fun findClosestValidBlockToRayNew(ray: RaycastUtils.Ray, range: IntRange): LorenzVec? {
        val bounds = IslandType.HUB.islandData?.boundingBox ?: return null
        if (!bounds.isInside(ray.origin)) return null // guarantees exit point is first intersect
        // you technically don't need to find the endpoint for this, but it makes it simpler so why not
        val endPoint = RaycastUtils.intersectAABBWithRay(bounds, ray)?.second ?: return null

        val diff = endPoint.minus(ray.origin).toDoubleArray()
        val axisIndex = diff.withIndex()
            .filter { (_, value) -> abs(value) > 0.9 } // only if the axis isn't the same block
            .minByOrNull { (_, value) -> abs(value) } // find the axis with the least change
            ?.index
            ?: return null

        val candidates = mutableMapOf<LorenzVec, Pair<Double, Double>>() // position mapped to scaledDistToRay and distFromOrigin
        val endPointArray = endPoint.toDoubleArray()
        val originArray = ray.origin.toDoubleArray()
        val directionArray = ray.direction.toDoubleArray()

        val iterations = abs(endPointArray[axisIndex] - originArray[axisIndex])
        for (i in 1..iterations.toInt()) {
            val axisValue = originArray[axisIndex] + i * sign(directionArray[axisIndex])
            val candidatePoint = RaycastUtils.findPointOnRay(ray, axisIndex, axisValue) ?: continue
            val candidateBlock = candidatePoint.roundToBlock()
            if (!isBlockValid(candidateBlock)) continue
            val blockCenter = candidateBlock.add(0.5, 0.5, 0.5)
            val distanceToRay = RaycastUtils.findDistanceToRay(ray, blockCenter)

            val distanceFromOrigin = candidatePoint.distance(ray.origin)

            // take the ratio to account for errors
            val scaledDistance = (distanceToRay * 500000 / distanceFromOrigin)

            candidates[candidateBlock] = Pair(scaledDistance.roundTo(5), distanceFromOrigin)
        }

        if (candidates.isEmpty()) return null
        val minValue = candidates.values.minOf { it.first }
        val possibilities = candidates.filterValues { it.first == minValue }
        var withinRange = possibilities.filterValues { it.second.toInt() in range }.map { it.key }
        if (withinRange.isEmpty()) {
            ChatUtils.chat(
                "no guesses within range found for range $range" +
                    " please report this to SidOfThe7Cs - all options were $possibilities"
            )
            withinRange = possibilities.map { it.key }
        }

        allGuesses.add(GuessEntry(withinRange))

        if (DebugSesh.debugActive) {
            DebugSesh.guessesMade++
            if (withinRange.size == 1) DebugSesh.preciseGuesses++
        }

        if (withinRange.size > 1 && config.warnIfInaccurateArrowGuess) {
            TitleManager.sendTitle("§eUse Spade")
        }

        return withinRange[0]
    }

    private fun isBlockValid(pos: LorenzVec): Boolean {
        if (!pos.isInLoadedChunk()) {
            return true
        }
        val isGround = pos.getBlockAt() == Blocks.GRASS_BLOCK
        val isValidBlockAbove = pos.up().getBlockAt() in GriffinBurrowHelper.allowedBlocksAboveGround
        return isGround && isValidBlockAbove
    }

    private fun detectArrow(points: MutableSet<LorenzVec>): RaycastUtils.Ray? {
        val line = findLine(points, SHAFT_LENGTH, PARTICLE_DETECTION_TOLERANCE)
        if (line.isEmpty()) return null

        // the head of the arrow intersects with the particle one off the end
        // findLine only returns a full line with 20 points or an emptyList so this is safe
        val candidate1 = line[1]
        val candidate2 = line[line.size - 2]
        val count1 = getPointsWithinDistance(points, candidate1, PARTICLE_DETECTION_TOLERANCE)
        val count2 = getPointsWithinDistance(points, candidate2, PARTICLE_DETECTION_TOLERANCE)

        // One should be 2 (base) and the other 4 (tip)
        if (setOf(count1, count2) != setOf(COUNT_NEAR_BASE, COUNT_NEAR_TIP)) return null

        val base: LorenzVec
        val tip: LorenzVec

        if (count1 == COUNT_NEAR_TIP) { // if the first point is the base
            tip = line.first()
            base = line.last()
        } else {
            tip = line.last()
            base = line.first()
        }

        // arrow is a block above the center of the start block
        val adjustedBase = base.down(1.5) // this is always an exact multiple of 0.5
        val adjustedTip = tip.down(1.5)

        return RaycastUtils.Ray(adjustedBase, adjustedTip.minus(adjustedBase).normalize())
    }

    private fun getPointsWithinDistance(
        points: Iterable<LorenzVec>,
        origin: LorenzVec,
        maxDist: Double
    ): Int {
        val maxDistSq = maxDist * maxDist
        return points.count { it != origin && it.distanceSq(origin) <= maxDistSq }
    }

    private fun findLine(points: Iterable<LorenzVec>, shaftLength: Int, maxDist: Double): List<LorenzVec> {
        for (point in points) {
            val line = mutableListOf<LorenzVec>()
            val visited = mutableSetOf<LorenzVec>()
            line.add(point)
            visited.add(point)

            if (extendLine(line, visited, points, shaftLength, maxDist)) {
                return line.toList()
            }
        }
        return emptyList()
    }

    private fun extendLine(
        line: MutableList<LorenzVec>,
        visited: MutableSet<LorenzVec>,
        points: Iterable<LorenzVec>,
        numPoints: Int,
        maxDist: Double
    ): Boolean {
        if (line.size == numPoints) return true // line completed

        var nextPoint: LorenzVec? = null
        var minDist = Double.MAX_VALUE

        for (point in points) {
            if (visited.contains(point)) continue
            val dist = line.last().distance(point)
            if (dist > maxDist) continue

            // must be collinear with all existing points
            val second = if (line.size > 1) line[1] else line[0]
            if (!isCollinear(line.first(), second, point)) continue

            if (dist < minDist) {
                minDist = dist
                nextPoint = point
            }
        }

        if (nextPoint != null) {
            line.add(nextPoint)
            visited.add(nextPoint)
            if (extendLine(line, visited, points, numPoints, maxDist)) {
                return true
            }
            // backtrack
            line.removeLast()
            visited.remove(nextPoint)
        }

        return false
    }

    private fun isCollinear(a: LorenzVec, b: LorenzVec, c: LorenzVec): Boolean {
        val ab = b.minus(a)
        val ac = c.minus(a)
        val cross = ab.crossProduct(ac)
        return cross.lengthSquared() < EPSILON
    }

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess && config.guessFromArrow
}
