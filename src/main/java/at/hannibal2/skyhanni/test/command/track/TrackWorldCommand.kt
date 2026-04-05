package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.events.CancellableWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText

/**
 * Abstract class for world-tracking commands, extending [TrackCommand] with world rendering support.
 * Use this for events that have a world location, providing in-world visualization of tracked events.
 *
 * @param T The type of event to track, which must extend [CancellableWorldEvent].
 * @param K The type of identifier used to categorize the tracked events.
 * @param onlyOnSkyblock If true, the command will only work in SkyBlock.
 * @param commonName The singular name of the tracked event.
 * @param commonNamePlural The plural name of the tracked event.
 */
abstract class TrackWorldCommand<T : CancellableWorldEvent, K>(
    onlyOnSkyblock: Boolean = true,
    commonName: String,
    commonNamePlural: String = commonName + "s",
) : TrackCommand<T, K>(onlyOnSkyblock, commonName, commonNamePlural) {

    abstract fun T.formatForWorldRender(): String

    private var worldTracked: Map<LorenzVec, List<T>> = emptyMap()

    private fun SkyHanniRenderWorldEvent.drawSingleInWorld(vec: LorenzVec, event: T) {
        drawDynamicText(vec, "§7§l${event.getTypeIdentifier()}", 0.8)
        drawDynamicText(
            vec.down(0.2),
            event.formatForWorldRender(),
            scaleMultiplier = 0.8,
        )
    }

    private fun SkyHanniRenderWorldEvent.drawMultipleInWorld(vec: LorenzVec, events: List<T>) {
        drawDynamicText(vec, "§e${events.size} $commonNamePlural", 0.8)
        var offset = 0.2
        events.groupBy { it.getTypeIdentifier() }.forEach { (groupName, groupEvents) ->
            drawDynamicText(vec.down(offset), "§7§l$groupName §7(§e${groupEvents.size}§7)", 0.8)
            offset += 0.2
        }
    }

    open fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isActive) return
        for ((vec, eventList) in worldTracked) {
            if (eventList.isEmpty()) continue
            else if (eventList.size != 1) event.drawMultipleInWorld(vec, eventList)
            else event.drawSingleInWorld(vec, eventList.first())
        }
    }

    override fun onTickExtensions(recent: List<T>) {
        worldTracked = recent.groupBy { it.location }
    }
}
