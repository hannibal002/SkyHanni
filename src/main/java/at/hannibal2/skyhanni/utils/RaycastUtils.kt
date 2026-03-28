package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.VectorUtils.isNormalized
import at.hannibal2.skyhanni.utils.VectorUtils.minus
import at.hannibal2.skyhanni.utils.VectorUtils.plus
import at.hannibal2.skyhanni.utils.VectorUtils.times
import at.hannibal2.skyhanni.utils.VectorUtils.toDoubleArray
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object RaycastUtils {

    private const val EPSILON = 1e-12

    data class Ray(val origin: Vec3, val direction: Vec3) {
        init {
            require(direction.isNormalized())
        }
    }

    data class Plane(val origin: Vec3, val normal: Vec3) {
        init {
            require(normal.isNormalized())
        }
    }

    fun createPlayerLookDirectionRay() = Ray(
        LocationUtils.playerEyeLocation(),
        MinecraftCompat.localPlayer.lookAngle,
    )

    /**
     * Create a plane that contains [point] and is orthogonal to [ray].
     */
    fun createOrthogonalPlaneToRayAtPoint(ray: Ray, point: Vec3) =
        Plane(point, ray.direction)

    /**
     * Intersect a plane (of any orientation) with a ray. The ray and plane may not be parallel to each other.
     */
    fun intersectPlaneWithRay(plane: Plane, ray: Ray): Vec3 {
        // require(plane.normal.dotProduct(ray.direction).absoluteValue != 0.0)
        val intersectionPointDistanceAlongRay = with(plane.normal) {
            (dot(plane.origin) - dot(ray.origin)) / dot(ray.direction)
        }
        return ray.origin + ray.direction.scale(intersectionPointDistanceAlongRay)
    }

    /**
     * Finds the distance between the given ray and the point. If the point is behind the ray origin (according to the ray's direction),
     * returns [Double.MAX_VALUE] instead.
     */
    fun findDistanceToRay(ray: Ray, point: Vec3): Double {
        val plane = createOrthogonalPlaneToRayAtPoint(ray, point)
        val intersectionPoint = intersectPlaneWithRay(plane, ray)
        if ((intersectionPoint - ray.origin).dot(ray.direction) < 0) return Double.MAX_VALUE
        return intersectionPoint.distanceTo(point)
    }

    inline fun <T> createDistanceToRayEstimator(
        ray: Ray,
        crossinline position: (T) -> Vec3,
    ): (T) -> Double = { findDistanceToRay(ray, position(it)) }

    /**
     * Intersect an axis-aligned bounding box with a ray.
     * Returns a pair of Vec3 (entry point, exit point) if the ray hits the box.
     * The entry point may be behind the ray origin if the ray starts inside the box.
     * Returns null if the ray points away from the box or misses it entirely.
     */
    fun intersectAABBWithRay(aabb: AABB, ray: Ray): Pair<Vec3, Vec3>? {
        val aabbMin = Vec3(aabb.minX, aabb.minY, aabb.minZ).toDoubleArray()
        val aabbMax = Vec3(aabb.maxX, aabb.maxY, aabb.maxZ).toDoubleArray()

        val dirArray = ray.direction.toDoubleArray()
        val originArray = ray.origin.toDoubleArray()

        var tMin = -Double.MAX_VALUE
        var tMax = Double.MAX_VALUE

        // Iterate over each axis (x, y, z)
        for (i in 0..2) {
            // If the ray is parallel to the slab (AABB plane pair)
            if (abs(dirArray[i]) < EPSILON) {
                // If the origin is outside the slab, there's no intersection
                if (originArray[i] < aabbMin[i] || originArray[i] > aabbMax[i]) return null
            } else {
                val ood = 1.0 / dirArray[i]
                var t1 = (aabbMin[i] - originArray[i]) * ood
                var t2 = (aabbMax[i] - originArray[i]) * ood

                // Ensure t1 is the intersection with the near plane, and t2 with the far plane
                if (t1 > t2) t1 = t2.also { t2 = t1 }

                // Update tMin and tMax to compute the intersection interval
                tMin = max(tMin, t1)
                tMax = min(tMax, t2)

                // If the interval becomes invalid, there is no intersection
                if (tMin > tMax) return null
            }
        }

        // If we reach here, the ray intersects the AABB on all 3 axes
        val entry = ray.origin + ray.direction * tMin
        val exit = ray.origin + ray.direction * tMax

        return Pair(entry, exit)
    }

    /**
     * Find the point on a ray where a specific axis has a given value.
     * Axis of 0, 1, 2 is x, y, z respectively
     */
    fun findPointOnRay(ray: Ray, axis: Int, targetValue: Double): Vec3? {
        val originArray = ray.origin.toDoubleArray()
        val dirComponent = ray.direction.toDoubleArray()[axis]

        if (abs(dirComponent) < EPSILON) {
            // Ray parallel to that axis
            return if (abs(originArray[axis] - targetValue) < EPSILON) {
                ray.origin // Ray is exactly at that coordinate
            } else {
                null // Can't reach target coordinate
            }
        }

        // Calculate t where point[axis] = targetValue
        val t = (targetValue - originArray[axis]) / dirComponent

        // Calculate full point
        return ray.origin + ray.direction * t
    }

    // TODO make private or no longer generic
    fun <T : Any> List<T>.findClosestPointToRay(ray: Ray, positionExtractor: (T) -> Vec3): T? =
        minByOrNull(createDistanceToRayEstimator(ray, positionExtractor))
}
