package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.world.phys.AABB

@SkyHanniModule
object FrustumUtils {

    //#if MC < 1.21
    //$$ private var frustum: Frustum? = null
    //#else
    private val frustum get() = Minecraft.getInstance().levelRenderer.cullingFrustum
    //#endif

    fun isVisible(box: AABB): Boolean =
        //#if MC < 1.21
        //$$ frustum?.isBoundingBoxInFrustum(box) ?: false
    //#else
    frustum?.isVisible(box) ?: true
    //#endif

    fun isVisible(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) =
        isVisible(AABB(minX, minY, minZ, maxX, maxY, maxZ))

    //#if MC < 1.21
    //$$ /**
    //$$  * We want to account for the render entity's position which is affected by partial ticks.
    //$$  */
    //$$ @HandleEvent(priority = HandleEvent.HIGHEST)
    //$$ fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
    //$$     val pos = WorldRenderUtils.exactLocation(MinecraftClient.getInstance().getCameraEntity(), event.partialTicks)
    //$$     frustum = Frustum().also { it.setPosition(pos.x, pos.y, pos.z) }
    //$$ }
    //#endif

}
