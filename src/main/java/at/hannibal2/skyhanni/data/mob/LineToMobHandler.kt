package at.hannibal2.hanni.data.mob

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.minecraft.WorldChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawLineToEye
import io.github.notenoughupdates.moulconfig.ChromaColour

@HanniModule
object LineToMobHandler {

    data class LineSettings(
        val color: ChromaColour,
        val width: Int,
        val depth: Boolean,
        val condition: () -> Boolean,
    )

    private val lines = mutableMapOf<Mob, LineSettings>()

    fun register(mob: Mob, color: ChromaColour, width: Int, depth: Boolean, condition: () -> Boolean) =
        register(mob, LineSettings(color, width, depth, condition))

    fun register(mob: Mob, settings: LineSettings) {
        lines[mob] = settings
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn) {
        lines.remove(event.mob)
    }

    // TODO remove workaround once we can confirm why lines show up after world switch
    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        lines.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (lines.isEmpty()) return

        for ((mob, settings) in lines) {
            if (!settings.condition() || !mob.canBeSeen()) continue
            event.drawLineToEye(
                mob.centerCords,
                settings.color,
                settings.width,
                settings.depth,
            )
        }
    }
}
