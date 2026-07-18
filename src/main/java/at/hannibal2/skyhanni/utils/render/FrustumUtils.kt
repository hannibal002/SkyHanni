package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB

@SkyHanniModule
object FrustumUtils {

    private val frustum get() =
        //? if >= 26.1 {
        Minecraft.getInstance().gameRenderer.mainCamera().cullFrustum
        //?} else {
        /*Minecraft.getInstance().levelRenderer.capturedFrustum
        *///?}

    fun isVisible(box: AABB): Boolean =
        //? if >= 26.1 {
        frustum.isVisible(box)
        //?} else {
        /*frustum?.isVisible(box) ?: true
        *///?}

    fun isVisible(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean =
        isVisible(AABB(minX, minY, minZ, maxX, maxY, maxZ))
}
