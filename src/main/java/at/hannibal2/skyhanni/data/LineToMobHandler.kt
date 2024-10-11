package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object LineToMobHandler {

    data class Settings(
        val color: Color,
        val width: Int,
        val depth: Boolean,
    )

    private val map = mutableMapOf<Mob, Settings>()

    fun register(mob: Mob, color: Color, width: Int, depth: Boolean) = register(mob, Settings(color, width, depth))

    fun register(mob: Mob, settings: Settings) {
        map[mob] = settings
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn) {
        map.remove(event.mob)
    }

    @SubscribeEvent
    fun onLorenzRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (map.isEmpty()) return
        RenderUtils.LineDrawer.draw3D(event.partialTicks) {
            for ((mob, settings) in map) {
                if (!mob.canBeSeen()) continue
                draw3DLineFromPlayer(mob.centerCords, settings.color, settings.width, settings.depth)
            }
        }
    }
}
