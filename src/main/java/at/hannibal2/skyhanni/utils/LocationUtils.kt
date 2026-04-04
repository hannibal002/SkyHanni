package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias PointSet = TimeLimitedSet<Pair<LorenzVec, Boolean>>
typealias FacePointEntry = Map.Entry<Direction, PointSet>
typealias FacePointSet = MutableMap<Direction, PointSet>

@Suppress("TooManyFunctions", "MemberVisibilityCanBePrivate")
object LocationUtils {

    fun canSee(a: LorenzVec, b: LorenzVec, offset: Double? = null): Boolean {
        return canSee0(a, b) && offset?.let { canSee0(a.add(y = it), b.add(y = it)) } ?: true
    }

    private fun canSee0(a: LorenzVec, b: LorenzVec): Boolean = BlockUtils.raycast(a, b)?.miss == true

    fun playerLocation() = PlayerUtils.getLocation()

    // Block heights are multiples of 1/16, so we subtract 1/16 to find the right block
    fun getBlockBelowPlayer() = playerLocation().add(0.0, -1.0 / 16.0, 0.0).roundToBlock()

    fun LorenzVec.distanceToPlayer() = distance(playerLocation())

    fun LorenzVec.distanceToPlayerIgnoreY() = distanceIgnoreY(playerLocation())

    fun LorenzVec.distanceSqToPlayer() = distanceSq(playerLocation())

    fun LorenzVec.distanceToPlayerSqIgnoreY() = distanceSqIgnoreY(playerLocation())

    fun Entity.distanceToPlayer() = getLorenzVec().distanceToPlayer()
    fun Entity.distanceSqToPlayer() = getLorenzVec().distanceSqToPlayer()

    fun Entity.distanceTo(location: LorenzVec) = getLorenzVec().distance(location)
    fun Entity.distanceTo(other: Entity) = getLorenzVec().distance(other.getLorenzVec())

    fun Entity.distanceToIgnoreY(location: LorenzVec) = getLorenzVec().distanceIgnoreY(location)

    fun playerEyeLocation(): LorenzVec {
        val player = MinecraftCompat.localPlayer
        val vec = player.getLorenzVec()
        return vec.up(player.eyeHeight.toDouble())
    }

    fun AABB.isInside(vec: LorenzVec) = contains(vec.toVec3())

    fun AABB.isPlayerInside() = isInside(playerLocation())

    /**
     * Extension function on top of [canSeeFace], allowing checking of more than one face with a singular call.
     * See lower function for more extensive documentation.
     *
     * @param ignoreFaces Which faces, if any, to ignore when checking visibility. Default is none (meaning all faces are checked).
     * @return True if any face can be seen, false otherwise.
     */
    fun canSeeAnyFace(
        min: LorenzVec,
        max: LorenzVec,
        viewDistance: Number = 150.0,
        stepCount: Int = 0,
        stepDensity: Int = 4,
        pointFill: FacePointSet? = null,
        offset: Double? = null,
        resultLifespan: Duration = 5.seconds,
        vararg ignoreFaces: Direction,
    ): Boolean {
        for (face in Direction.entries) {
            if (ignoreFaces.contains(face)) continue
            val faceResult = canSeeFace(face, min, max, viewDistance, stepCount, stepDensity, pointFill, offset, resultLifespan)
            if (faceResult && pointFill == null) return true
        }
        return pointFill?.values?.any { facedFill ->
            facedFill.any { (_, success) -> success }
        } ?: false
    }

    /**
     * When passed a corner pair of vectors, checks if the player can see the center point of, or optionally,
     * a number of other points on the specified face, defined by the [stepCount] parameter.
     *
     * A note about the [stepCount] parameter - it is linear, but at a rate of increasing by [stepDensity], per face, per step.
     * So, use both sparingly, as it can lead to a lot of axes being cast across a face.
     *
     * Another note, if [pointFill] is provided, this function will continue to fill out points in the map, even after
     * it finds a point that can be seen - this is useful for debugging, but should not be used in user-facing code.
     *
     * @param min The first corner of the face (minimum corner).
     * @param max The second corner of the face (maximum corner).
     * @param viewDistance The maximum distance at which the player can see the face.
     * @param stepCount The number of "middle points" between face<->edge to check. 0 would mean only the center point is checked.
     * @param stepDensity The number of contour rays to cast from the center of each face towards the nearest corner.
     * @param pointFill If provided, this map will be filled with points that can be seen (true) or not (false).
     * @param offset An optional vertical offset to apply to the face corners.
     * @param resultLifespan How long the results should be stored in the [pointFill] map, if provided. Default is 5 seconds.
     * @return True if the player can see the specified face, false otherwise.
     */
    fun canSeeFace(
        face: Direction,
        min: LorenzVec,
        max: LorenzVec,
        viewDistance: Number = 150.0,
        stepCount: Int = 0,
        stepDensity: Int = 4,
        pointFill: FacePointSet? = null,
        offset: Double? = null,
        resultLifespan: Duration = 5.seconds,
    ): Boolean {
        val aabb = AABB(min.x, min.y, min.z, max.x, max.y, max.z)
        val eye = playerEyeLocation()
        val center = aabb.getBoxCenter()
        val faceCenter = face.getCenterPos(center, aabb)

        if (eye.distance(faceCenter) > viewDistance.toDouble()) return false
        val wrappedSuccess = pointFill.wrapCanSee(face, eye, faceCenter, offset, resultLifespan)

        return if (wrappedSuccess && pointFill == null) true
        else if (stepCount == 0) {
            // If stepCount is 0, we only check the center point.
            // If the block could be seen from the center, it would have returned true already,
            // so we can assert that we can't see the block.
            false
        } else face.performStepping(aabb, faceCenter, eye, viewDistance, stepCount, stepDensity, pointFill, offset, resultLifespan)
    }

    private fun Direction.performStepping(
        aabb: AABB,
        faceCenter: LorenzVec,
        eye: LorenzVec,
        viewDistance: Number,
        stepCount: Int,
        stepDensity: Int,
        pointFill: FacePointSet?,
        offset: Double? = null,
        resultLifespan: Duration = 5.seconds,
    ): Boolean {
        val halfX = (aabb.maxX - aabb.minX) / 2
        val halfY = (aabb.maxY - aabb.minY) / 2
        val halfZ = (aabb.maxZ - aabb.minZ) / 2
        val (axis1, axis2, ext1, ext2) = getFaceRayConfig(halfX, halfY, halfZ) ?: return false
        for (densityIncrement in 0 until stepDensity) {
            val angle = 2 * PI * densityIncrement / stepDensity
            val dx = cos(angle)
            val dy = sin(angle)
            val boundaryDist = min(
                if (dx != 0.0) ext1 / abs(dx) else Double.POSITIVE_INFINITY,
                if (dy != 0.0) ext2 / abs(dy) else Double.POSITIVE_INFINITY
            )
            val dirVec = axis1 * dx + axis2 * dy

            stepLoop@for (step in 1..stepCount) {
                val frac = step.toDouble() / (stepCount + 1)
                val testPoint = faceCenter + dirVec * (boundaryDist * frac)
                if (eye.distance(testPoint) > viewDistance.toDouble()) continue@stepLoop
                val wrappedSuccess = pointFill.wrapCanSee(this, eye, testPoint, offset, resultLifespan)
                if (wrappedSuccess && pointFill == null) return true
            }
        }
        return false
    }

    private fun FacePointSet?.wrapCanSee(
        face: Direction,
        a: LorenzVec,
        b: LorenzVec,
        offset: Double?,
        resultLifespan: Duration = 5.seconds,
    ): Boolean {
        val canSeeResult = canSee(a, b, offset)
        this?.getOrPut(face) { TimeLimitedSet(resultLifespan) }?.add(b to canSeeResult)
        return canSeeResult
    }

    private fun Direction.getCenterPos(center: LorenzVec, aabb: AABB) = when (this) {
        Direction.DOWN -> LorenzVec(center.x, aabb.minY, center.z)
        Direction.UP -> LorenzVec(center.x, aabb.maxY, center.z)
        Direction.NORTH -> LorenzVec(center.x, center.y, aabb.minZ)
        Direction.SOUTH -> LorenzVec(center.x, center.y, aabb.maxZ)
        Direction.WEST -> LorenzVec(aabb.minX, center.y, center.z)
        Direction.EAST -> LorenzVec(aabb.maxX, center.y, center.z)
    }

    private val xIdentityVector = LorenzVec(1.0, 0.0, 0.0)
    private val yIdentityVector = LorenzVec(0.0, 1.0, 0.0)
    private val zIdentityVector = LorenzVec(0.0, 0.0, 1.0)

    // Cache the identity vectors for each face to avoid recalculating them every time
    private val faceMap: Map<Direction, Pair<LorenzVec, LorenzVec>> by lazy {
        val verticalSet = xIdentityVector to zIdentityVector
        val northSouthSet = xIdentityVector to yIdentityVector
        val eastWestSet = zIdentityVector to yIdentityVector
        mapOf(
            Direction.DOWN to verticalSet,
            Direction.UP to verticalSet,
            Direction.NORTH to northSouthSet,
            Direction.SOUTH to northSouthSet,
            Direction.WEST to eastWestSet,
            Direction.EAST to eastWestSet
        )
    }

    private fun Direction.getFaceRayConfig(
        halfX: Double,
        halfY: Double,
        halfZ: Double,
    ): FaceRayConfig? {
        // The identity vectors for each face
        val (axis1Iden, axis2Iden) = faceMap[this] ?: return null
        return when (this) {
            Direction.UP, Direction.DOWN -> FaceRayConfig(axis1Iden, axis2Iden, halfX, halfZ)
            Direction.NORTH, Direction.SOUTH -> FaceRayConfig(axis1Iden, axis2Iden, halfX, halfY)
            Direction.WEST, Direction.EAST -> FaceRayConfig(axis1Iden, axis2Iden, halfZ, halfY)
        }
    }

    private data class FaceRayConfig(
        val axis1: LorenzVec,
        val axis2: LorenzVec,
        val ext1: Double,
        val ext2: Double,
    )

    fun LorenzVec.canBeSeen(viewDistance: Number = 150.0, offset: Double? = null): Boolean {
        val a = playerEyeLocation()
        val b = this
        return a.distance(b) < viewDistance.toDouble() && canSee(a, b, offset)
    }

    fun LorenzVec.canBeSeen(yOffsetRange: IntRange, radius: Double = 150.0): Boolean =
        yOffsetRange.any { offset ->
            up(offset).canBeSeen(radius)
        }

    fun AABB.minBox() = LorenzVec(minX, minY, minZ)

    fun AABB.maxBox() = LorenzVec(maxX, maxY, maxZ)

    fun AABB.rayIntersects(origin: LorenzVec, direction: LorenzVec): Boolean {
        // Reference for Algorithm https://tavianator.com/2011/ray_box.html
        val rayDirectionInverse = direction.inverse()
        val t1 = (this.minBox() - origin) * rayDirectionInverse
        val t2 = (this.maxBox() - origin) * rayDirectionInverse

        val tMin = max(t1.minOfEachElement(t2).max(), Double.NEGATIVE_INFINITY)
        val tMax = min(t1.maxOfEachElement(t2).min(), Double.POSITIVE_INFINITY)
        return tMax >= tMin && tMax >= 0.0
    }

    fun AABB.union(aabbs: List<AABB>?): AABB? {
        if (aabbs.isNullOrEmpty()) {
            return null
        }

        var minX = this.minX
        var minY = this.minY
        var minZ = this.minZ
        var maxX = this.maxX
        var maxY = this.maxY
        var maxZ = this.maxZ

        for (aabb in aabbs) {
            if (aabb.minX < minX) minX = aabb.minX
            if (aabb.minY < minY) minY = aabb.minY
            if (aabb.minZ < minZ) minZ = aabb.minZ
            if (aabb.maxX > maxX) maxX = aabb.maxX
            if (aabb.maxY > maxY) maxY = aabb.maxY
            if (aabb.maxZ > maxZ) maxZ = aabb.maxZ
        }

        return AABB(minX, minY, minZ, maxX, maxY, maxZ)
    }

    fun AABB.getEdgeLengths() = maxBox() - minBox()

    fun AABB.getBoxCenter() = getEdgeLengths() * 0.5 + minBox()

    fun AABB.getTopCenter() = getBoxCenter().up((maxY - minY) / 2)

    fun AABB.clampTo(other: AABB): AABB {
        val minX = max(this.minX, other.minX)
        val minY = max(this.minY, other.minY)
        val minZ = max(this.minZ, other.minZ)
        val maxX = min(this.maxX, other.maxX)
        val maxY = min(this.maxY, other.maxY)
        val maxZ = min(this.maxZ, other.maxZ)
        return AABB(minX, minY, minZ, maxX, maxY, maxZ)
    }

    fun calculatePlayerYaw(): Float {
        val player = MinecraftCompat.localPlayer
        var yaw = player.yRot % 360
        if (yaw < 0) yaw += 360
        if (yaw > 180) yaw -= 360

        return yaw
    }

    fun calculatePlayerFacingDirection(): LorenzVec {
        val yaw = calculatePlayerYaw() + 180
        return when {
            yaw < 45 -> LorenzVec(0, 0, -1)
            yaw < 135 -> LorenzVec(1, 0, 0)
            yaw < 225 -> LorenzVec(0, 0, 1)
            yaw < 315 -> LorenzVec(-1, 0, 0)
            else -> LorenzVec(0, 0, -1)
        }
    }

    fun interpolateOverTime(
        startTime: SimpleTimeMark,
        maxTime: Duration,
        from: LorenzVec,
        to: LorenzVec,
    ): LorenzVec {
        if (startTime == SimpleTimeMark.farPast()) return from
        val now = SimpleTimeMark.now()

        val diff = now - startTime
        val location = if (diff < maxTime) {
            val percentage = diff / maxTime
            from.interpolate(to, percentage)
        } else to
        return location
    }

    fun AABB.calculateEdges(): Set<Pair<LorenzVec, LorenzVec>> {
        val bottomLeftFront = LorenzVec(minX, minY, minZ)
        val bottomLeftBack = LorenzVec(minX, minY, maxZ)
        val topLeftFront = LorenzVec(minX, maxY, minZ)
        val topLeftBack = LorenzVec(minX, maxY, maxZ)
        val bottomRightFront = LorenzVec(maxX, minY, minZ)
        val bottomRightBack = LorenzVec(maxX, minY, maxZ)
        val topRightFront = LorenzVec(maxX, maxY, minZ)
        val topRightBack = LorenzVec(maxX, maxY, maxZ)

        return setOf(
            // Bottom face
            bottomLeftFront to bottomLeftBack,
            bottomLeftBack to bottomRightBack,
            bottomRightBack to bottomRightFront,
            bottomRightFront to bottomLeftFront,
            // Top face
            topLeftFront to topLeftBack,
            topLeftBack to topRightBack,
            topRightBack to topRightFront,
            topRightFront to topLeftFront,
            // Vertical edges
            bottomLeftFront to topLeftFront,
            bottomLeftBack to topLeftBack,
            bottomRightBack to topRightBack,
            bottomRightFront to topRightFront,
        )
    }

    fun computePitchWeight(derivative: LorenzVec) = sqrt(24 * sin(getPitchFromDerivative(derivative) - PI) + 25)

    private fun getPitchFromDerivative(derivative: LorenzVec): Double {
        val xzLength = sqrt(derivative.x.pow(2) + derivative.z.pow(2))
        val pitchRadians = -atan2(derivative.y, xzLength)
        // Solve y = atan2(sin(x) - 0.75, cos(x)) for x from y
        var guessPitch = pitchRadians
        var resultPitch = atan2(sin(guessPitch) - 0.75, cos(guessPitch))
        var windowMax = PI / 2
        var windowMin = -PI / 2
        repeat(100) {
            if (resultPitch < pitchRadians) {
                windowMin = guessPitch
                guessPitch = (windowMin + windowMax) / 2
            } else {
                windowMax = guessPitch
                guessPitch = (windowMin + windowMax) / 2
            }
            resultPitch = atan2(sin(guessPitch) - 0.75, cos(guessPitch))
            if (resultPitch == pitchRadians) return guessPitch
        }
        return guessPitch
    }

    fun AABB.getCornersAtHeight(y: Double): List<LorenzVec> {
        val cornerOne = LorenzVec(minX, y, minZ)
        val cornerTwo = LorenzVec(minX, y, maxZ)
        val cornerThree = LorenzVec(maxX, y, maxZ)
        val cornerFour = LorenzVec(maxX, y, minZ)

        return listOf(cornerOne, cornerTwo, cornerThree, cornerFour)
    }
}
