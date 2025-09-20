package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.GardenUptimeConfig.GardenUptimeDisplayText
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DateChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DisplayMode
import at.hannibal2.skyhanni.utils.tracker.SkyhanniTimedTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraft.entity.player.EntityPlayer
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenUptimeTracker {
    private val config get() = GardenApi.config.gardenUptime

    val tracker = SkyhanniTimedTracker(
        "Garden Uptime Tracker",
        { Data() },
        { it.garden.uptimeTracker },
        { drawDisplay(it) },
        trackerConfig = { config.perTrackerConfig }
    )

    class TimeData : TimedTrackerData<Data, SessionUptime.Garden>(SessionUptime.Garden::class, { Data() }) {
        init {
            if (config.resetSession) {
                getOrPutEntry(SkyHanniTracker.DisplayMode.SESSION).reset()
            }
        }
    }

    class Data : TrackerData<SessionUptime.Garden>(SessionUptime.Garden::class) {
        override fun resetData() {
            cropBreakTime = 0
            visitorTime = 0
            pestTime = 0
            blocksBroken = 0
        }
        @Expose
        var cropBreakTime: Int = 0

        @Expose
        var visitorTime: Int = 0

        @Expose
        var pestTime: Int = 0

        @Expose
        var blocksBroken: Int = 0

    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        tracker.renderDisplay(config.pos)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            config.includeVisitors,
            config.includePests
        ) {
            tracker.update()
        }
        config.uptimeDisplayText.afterChange {
            tracker.update()
        }
    }

    private var blockBreaksLastSecond = 0
    var storage = GardenApi.storage

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        val lineMap = mutableMapOf<GardenUptimeDisplayText, Searchable>()
        lineMap[GardenUptimeDisplayText.TITLE] = StringRenderable("§6Garden Uptime").toSearchable()

        lineMap[GardenUptimeDisplayText.DATE] = buildDate().toSearchable()

        val uptime = data.getTotalUptime()
        lineMap[GardenUptimeDisplayText.UPTIME] =
            StringRenderable(
                "§7Uptime: §e${if (uptime > 0.seconds) uptime else "§cnone"}${if (tracker.isPaused()) " §cPaused!" else ""}"
            ).toSearchable()

        var bps = 0.0
        if (uptime > 0.seconds) bps =
            (data.blocksBroken.toDouble() - blockBreaksLastSecond) / uptime.inPartialSeconds
        if (bps > 0) {
            lineMap[GardenUptimeDisplayText.BPS] =
                StringRenderable("§7Blocks/Second: §e${bps.roundTo(2)}").toSearchable()
        }

        lineMap[GardenUptimeDisplayText.BLOCKS_BROKEN] =
            StringRenderable("§7Blocks Broken: §e${data.blocksBroken.addSeparators()}").toSearchable()

        return formatDisplay(lineMap)
    }

    private fun formatDisplay(lineMap: MutableMap<GardenUptimeDisplayText, Searchable>): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        newList.addAll(config.uptimeDisplayText.get().mapNotNull { lineMap[it] })
        return newList
    }

    private fun buildDate() = Renderable.vertical(
        buildList {
            val displayText: String = when (tracker.displayMode) {
                DisplayMode.DAY -> {
                    "§7Date: §a${tracker.dateString()}"
                }
                DisplayMode.WEEK -> {
                    "§7Week: §a${tracker.dateString()}"
                }

                DisplayMode.MONTH -> {
                    "§7Month: §a${tracker.dateString()}"
                }

                DisplayMode.YEAR -> {
                    "§7Year: §a${tracker.dateString()}"
                }

                else -> {
                    "§7Mode: §a${tracker.displayMode?.displayName ?: "none"}"
                }
            }
            addString(displayText)
        }
    )

    private fun isEnabled() = GardenApi.inGarden() && config.showDisplay
}
