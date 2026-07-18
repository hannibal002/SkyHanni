package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB

@SkyHanniModule
object FrustumUtils {

    private val frustum get() =
        //? if >= 26.2
        //Minecraft.getInstance().gameRenderer.mainCamera.cullFrustum
        //? if < 26.2 && >= 26.1
        Minecraft.getInstance().gameRenderer.mainCamera.cullFrustum
        //? if < 26.1
        //Minecraft.getInstance().levelRenderer.capturedFrustum

    //~ if < 26.1 'frustum.isVisible(box)' -> 'frustum?.isVisible(box) ?: true'
    fun isVisible(box: AABB): Boolean = frustum.isVisible(box)

    fun isVisible(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) =
        isVisible(AABB(minX, minY, minZ, maxX, maxY, maxZ))

}
