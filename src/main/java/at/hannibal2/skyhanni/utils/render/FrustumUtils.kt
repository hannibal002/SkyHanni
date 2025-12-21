package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB

@SkyHanniModule
object FrustumUtils {

    //#if MC < 1.21.9
    private val frustum get() = Minecraft.getInstance().levelRenderer.cullingFrustum
    //#else
    //$$ private val frustum get() = Minecraft.getInstance().levelRenderer.capturedFrustum
    //#endif

    fun isVisible(box: AABB): Boolean = frustum?.isVisible(box) ?: true

    fun isVisible(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) =
        isVisible(AABB(minX, minY, minZ, maxX, maxY, maxZ))

}
