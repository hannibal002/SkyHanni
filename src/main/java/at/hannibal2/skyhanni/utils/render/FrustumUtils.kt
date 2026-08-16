package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB

@SkyHanniModule
object FrustumUtils {

    private val frustum get() = Minecraft.getInstance().gameRenderer.getMainCamera().getCullFrustum()

    fun isVisible(box: AABB): Boolean = frustum.isVisible(box)

}
