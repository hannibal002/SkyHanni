package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import java.awt.Color

@SkyHanniModule
object LineToMobHandler {

    data class LineSettings(
        val color: Color,
        val width: Int,
        val depth: Boolean,
        val condition: () -> Boolean,
    )

    private val lines = mutableMapOf<Mob, LineSettings>()

    fun register(mob: Mob, color: Color, width: Int, depth: Boolean, condition: () -> Boolean) =
        register(mob, LineSettings(color, width, depth, condition))

    fun register(mob: Mob, settings: LineSettings) {
        lines[mob] = settings
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn) {
        lines.remove(event.mob)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (lines.isEmpty()) return

        val playerLocation = event.exactPlayerEyeLocation()
        RenderUtils.LineDrawer.draw3D(event.partialTicks) {
            for ((mob, settings) in lines) {
                if (!settings.condition() || !mob.canBeSeen()) continue
                draw3DLine(mob.centerCords, playerLocation, settings.color, settings.width, settings.depth)
            }
        }
    }
}
