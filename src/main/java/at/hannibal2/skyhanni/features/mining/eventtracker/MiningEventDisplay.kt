package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.mining.MiningEventConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventType.Companion.CompressFormat
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MiningEventDisplay {
    private val config get() = SkyHanniMod.feature.mining.miningEvent
    private var display = mutableListOf<Renderable>()

    private val islandEventData: MutableMap<IslandType, MiningIslandEventInfo> = mutableMapOf()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.repeatSeconds(1)) return
        updateDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!shouldDisplay()) return
        config.position.renderRenderables(MiningEventType.entries.map {
            Renderable.horizontalContainer(
                listOf(
                    it.getRenderable(), Renderable.string(" ${it.eventName}")
                )
            )
        }, posLabel = "Upcoming Events Display")
        // config.position.renderRenderables(display, posLabel = "Upcoming Events Display")
    }

    private fun updateDisplay() {
        display.clear()
        updateEvents()
    }

    private fun updateEvents() {
        islandEventData.forEach { (islandType, eventDetails) ->
            val shouldShow = when (config.showType) {
                MiningEventConfig.ShowType.DWARVEN -> islandType == IslandType.DWARVEN_MINES
                MiningEventConfig.ShowType.CRYSTAL -> islandType == IslandType.CRYSTAL_HOLLOWS
                MiningEventConfig.ShowType.MINESHAFT -> islandType == IslandType.MINESHAFT
                MiningEventConfig.ShowType.CURRENT -> islandType.isInIsland()
                else -> true
            }

            eventDetails.islandEvents.firstOrNull()?.let { firstEvent ->
                if (firstEvent.endsAt.asTimeMark().isInPast()) {
                    eventDetails.lastEvent = firstEvent.event
                }
            }

            if (shouldShow) {
                val upcomingEvents = formatUpcomingEvents(eventDetails.islandEvents, eventDetails.lastEvent)
                val island =
                    if (!config.islandAsIcon) Renderable.string("§a${islandType.displayName}§8:") else
                        Renderable.horizontalContainer(
                            listOf(
                                when (islandType) {
                                    IslandType.DWARVEN_MINES -> Renderable.itemStack(
                                        "PERFECT_RUBY_GEM".asInternalName().getItemStack()
                                    )

                                    IslandType.CRYSTAL_HOLLOWS -> Renderable.itemStack(
                                        "MITHRIL_ORE".asInternalName().getItemStack()
                                    )

                                    IslandType.MINESHAFT -> Renderable.itemStack(ItemStack(Blocks.packed_ice))
                                    else -> unknownDisplay
                                },
                                Renderable.string("§8:")
                            )
                        )
                display.add(
                    Renderable.horizontalContainer(
                        listOf(
                            island,
                            *upcomingEvents
                        ), 3
                    )
                )
            }
        }
    }

    private val unknownDisplay = Renderable.string("§7???")
    private val transitionDisplay = Renderable.string("§8->")

    private fun formatUpcomingEvents(events: List<RunningEventType>, lastEvent: MiningEventType?): Array<Renderable> {
        val upcoming = events.filter { !it.endsAt.asTimeMark().isInPast() }
            .flatMap {
                if (it.isDoubleEvent) listOf(it.event, it.event) else listOf(it.event)
                /* if (it.isDoubleEvent) "${it.event} §8-> ${it.event}" else it.event.toString() */
            }.map { it.getRenderable() }.toMutableList()

        if (upcoming.isEmpty()) upcoming.add(unknownDisplay)
        if (config.passedEvents && upcoming.size < 4) lastEvent?.let { upcoming.add(0, it.getRenderable()) }
        return upcoming.flatMap { listOf(it, transitionDisplay) }.dropLast(1).toTypedArray()
        /* return upcoming.joinToString(" §8-> ") */
    }

    fun updateData(eventData: MiningEventData) {
        eventData.runningEvents.forEach { (islandType, events) ->
            val sorted = events.sortedBy { it.endsAt - it.event.defaultLength.inWholeMilliseconds }

            val oldData = islandEventData[islandType]
            if (oldData == null) {
                //todo remove once mineshaft is on main server
                if (sorted.isNotEmpty() || islandType != IslandType.MINESHAFT) {
                    islandEventData[islandType] = MiningIslandEventInfo(sorted)
                }
            } else {
                oldData.islandEvents = sorted
            }
        }
    }

    private fun shouldDisplay() =
        LorenzUtils.inSkyBlock && config.enabled && !ReminderUtils.isBusy() && !(!config.outsideMining && !LorenzUtils.inAdvancedMiningIsland())

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(31, "mining.miningEvent.compressedFormat") {
            ConfigUtils.migrateBooleanToEnum(it, CompressFormat.COMPACT_TEXT, CompressFormat.DEFAULT)
        }
    }
}

private class MiningIslandEventInfo(var islandEvents: List<RunningEventType>, var lastEvent: MiningEventType? = null)
