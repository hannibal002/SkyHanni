package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.GardenBpsTrackerConfig.GardenUptimeDisplayText
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyhanniTimedTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenBpsTracker {
    private val config get() = GardenApi.config.gardenBpsTracker

    val tracker = SkyhanniTimedTracker(
        "Garden Block Break Tracker",
        { Data() },
        { it.garden.gardenBpsTracker },
        { drawDisplay(it) },
        customUptimeControl = true,
        trackerConfig = { config.perTrackerConfig }
    )

    class TimedData : TimedTrackerData<Data>({ Data() })

    class Data : TrackerData<SessionUptime.Garden>(SessionUptime.Garden::class) {
        @Expose
        var blocksBroken: Int = 0
    }

    @HandleEvent
    fun onCropBreak(event: CropClickEvent) {
        if (event.clickType != ClickType.LEFT_CLICK) return
        blockBreaksLastFiveTicks++

    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (blockBreaksLastFiveTicks == 0) return
        tracker.modify { it.blocksBroken += blockBreaksLastFiveTicks }
        blockBreaksLastFiveTicks = 0
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        tracker.renderDisplay(config.pos)
    }

    @HandleEvent
    fun onConfigLoad() {
        config.uptimeDisplayText.afterChange {
            tracker.update()
        }
    }

    private var blockBreaksLastFiveTicks = 0
    var storage = GardenApi.storage

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        val lineMap = mutableMapOf<GardenUptimeDisplayText, Searchable>()
        lineMap[GardenUptimeDisplayText.TITLE] = StringRenderable("§6Crop Break Tracker").toSearchable()

        val uptime = data.getTotalUptime()

        var bps = 0.0
        // impossible bps amounts when under a second
        if (uptime > 1.seconds) bps =
            (data.blocksBroken.toDouble()) / uptime.inWholeSeconds
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

    private fun isEnabled() = GardenApi.inGarden() && config.showDisplay
}
