package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.util.AxisAlignedBB

@SkyHanniModule
object FrustumUtils {

    private var lastRenderEntityPos = LorenzVec(0.0, 0.0, 0.0)

    private val frustum
        get(): Frustum =
            //#if MC < 1.21
            Frustum().also { it.setPosition(lastRenderEntityPos.x, lastRenderEntityPos.y, lastRenderEntityPos.z) }
    //#else
    //$$ MinecraftClient.getInstance().worldRenderer.frustum
    //#endif


    fun isVisible(box: AxisAlignedBB): Boolean =
        //#if MC < 1.21
        frustum.isBoundingBoxInFrustum(box)
    //#else
    //$$ frustum.isVisible(box)
    //#endif

    fun isVisible(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) =
        isVisible(AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ))

    //#if MC < 1.21
    /**
     * We want to account for the render entity's position which is affected by partial ticks.
     */
    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        lastRenderEntityPos = RenderUtils.exactLocation(Minecraft.getMinecraft().renderViewEntity, event.partialTicks)
    }
    //#endif

}
