package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.diana.BurrowDetectEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper.allowedBlocksAboveGround
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ArrowGuessBurrow {
    private val config get() = SkyHanniMod.feature.event.diana

    private const val SHAFT_LENGTH = 20
    private const val PARTICLE_DETECTION_TOLERANCE = 0.12
    private const val COUNT_NEAR_TIP = 4
    private const val COUNT_NEAR_BASE = 2
    private const val EPSILON = 1e-6
    private val allowedOffsets = setOf(0.0, 128.0, 255.0)

    private val points: MutableSet<LorenzVec> = mutableSetOf()
    private val recentArrowParticles = TimeLimitedSet<LorenzVec>(1.minutes)

    private val allGuesses = mutableListOf<List<LorenzVec>>() // the first entry is the best guess other entries are other possibilities

    private var distanceDivisor = 0.0
        set(value) {
            if (field == value) return
            DebugSesh.clear()
            field = value
            DebugSesh.printData()
        }

    private var newArrow = true

    private object DebugSesh {
        var debugActive = false
        var timeStarted = SimpleTimeMark.farPast()

        var guessesMade = 0
        var incorrectGuesses = 0
        var preciseGuesses = 0
        var nonStartBurrowsFoundWithoutArrowGuess = 0
        var couldNotFindGuess = 0
        var bad = 0

        fun printData() {
            val warnings = if (bad > 0) "|  WARNING bad things happened $bad times" else ""
            val output = """
            |=== Arrow Guess Debug Session ===
            |Active: $debugActive
            |Running for: ${timeStarted.passedSince().format()}
            |Distance function: $distanceDivisor
            |
            |Statistics:
            |  Total guesses made: $guessesMade
            |  Incorrect guesses: $incorrectGuesses
            |  Precise guesses: $preciseGuesses
            |  Non-start burrows w/o arrow guess: $nonStartBurrowsFoundWithoutArrowGuess
            |  Could not find guess: $couldNotFindGuess
            |  Accuracy: ${"%.1f".format((guessesMade - incorrectGuesses) * 100.0 / guessesMade)}%
            $warnings""".trimMargin()

            println(output)
        }

        fun clear() {
            printData()
            timeStarted = SimpleTimeMark.now()
            guessesMade = 0
            incorrectGuesses = 0
            preciseGuesses = 0
            nonStartBurrowsFoundWithoutArrowGuess = 0
            couldNotFindGuess = 0
            bad = 0
        }
    }

    @HandleEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        // removes incorrect guesses
        if (event.type == BurrowType.START) return
        val location = event.burrowLocation
        val containingLists = allGuesses.filter { location in it }
        if (containingLists.size > 1) {
            if (DebugSesh.debugActive) DebugSesh.bad++
            return
        }
        if (containingLists.isEmpty()) {
            if (DebugSesh.debugActive) DebugSesh.nonStartBurrowsFoundWithoutArrowGuess++
            return
        }
        val containingList = containingLists.first()
        // we were correct
        if (containingList.first() == location) {
            allGuesses.remove(containingList)
        } else {
            if (DebugSesh.debugActive) DebugSesh.incorrectGuesses++
            containingList.forEach { GriffinBurrowHelper.removePreciseGuess(it) }
        }

    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebugarrowguesssession") {
            description = "start, stop, or check status of a diana arrow guess debug session"
            category = CommandCategory.DEVELOPER_DEBUG

            literalCallback("start") {
                if (!DebugSesh.debugActive) {
                    DebugSesh.debugActive = true
                    DebugSesh.timeStarted = SimpleTimeMark.now()
                    DebugSesh.printData()
                }
            }

            literalCallback("stop") {
                DebugSesh.debugActive = false
                DebugSesh.clear()
            }

            literalCallback("status") {
                DebugSesh.printData()
            }

            literal("setDistanceDivisor") {
                argCallback("divisor", BrigadierArguments.double(), emptyList()) { divisor ->
                    distanceDivisor = divisor
                }
            }
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (event.message.startsWith("§eYou dug out a Griffin Burrow!")) {
            points.clear()
            newArrow = true
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!newArrow) return

        if (event.distanceToPlayer > 6) return
        if (event.type != EnumParticleTypes.REDSTONE) return
        if (event.count != 0) return
        if (event.speed != 1.0f) return

        if (!event.offset.toDoubleArray().all(allowedOffsets::contains)) return

        if (!recentArrowParticles.add(event.location)) return
        points.add(event.location)

        val arrow = detectArrow(points) ?: return
        newArrow = false
        points.clear()
        val guess = findClosestValidBlockToRayNew(arrow) ?: run {
            if (DebugSesh.debugActive) {
                DebugSesh.couldNotFindGuess++
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

    private fun findClosestValidBlockToRayNew(ray: RaycastUtils.Ray): LorenzVec? {
        val bounds = IslandType.HUB.islandData?.boundingBox ?: return null
        if (!bounds.isInside(ray.origin)) return null // guarantees exit point is first intersect
        // you technically don't need to find the endpoint for this, but it makes it simpler so why not
        val endPoint = RaycastUtils.intersectAABBWithRay(bounds, ray)?.second ?: return null

        val diff = endPoint.minus(ray.origin).toDoubleArray()
        val axisIndex = diff.withIndex()
            .filter { (_, value) -> abs(value) > 0.9 }  // only if the axis isn't the same block
            .minByOrNull { (_, value) -> abs(value) }   // find the axis with the least change
            ?.index
            ?: return null

        val candidates = mutableMapOf<LorenzVec, Double>()
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
            val distanceToRay = RaycastUtils.findDistanceToRay(ray, blockCenter) * 50000

            val distanceFromOrigin = candidatePoint.distance(ray.origin)

            // testing required
            var scaledDistance = distanceToRay
            if (distanceDivisor != 0.0) scaledDistance = (distanceToRay) / (1 + distanceFromOrigin / distanceDivisor)

            candidates[candidateBlock] = scaledDistance
        }

        val sortedEntries = candidates.entries.sortedBy { it.value }
        val possibilities = sortedEntries.filterIndexed { index, entry ->
            if (index == 0) true  // Always include the smallest
            else entry.value < sortedEntries[0].value * 2
        }.map { it.key }

        allGuesses.add(possibilities)

        if (DebugSesh.debugActive) {
            DebugSesh.guessesMade++
            if (possibilities.size == 1) DebugSesh.preciseGuesses++
        }

        return possibilities[0]
    }

    private fun isBlockValid(pos: LorenzVec): Boolean {
        if (!pos.isInLoadedChunk()) {
            return true
        }
        val isGround = pos.getBlockAt() == Blocks.grass
        val isValidBlockAbove = pos.up().getBlockAt() in allowedBlocksAboveGround
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
        if (!((count1 == COUNT_NEAR_BASE && count2 == COUNT_NEAR_TIP)
                || (count1 == COUNT_NEAR_TIP && count2 == COUNT_NEAR_BASE))
        ) return null

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
