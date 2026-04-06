package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.GardenBpsTrackerConfig.GardenUptimeDisplayText
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.GardenTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.TimedPerTrackerConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyhanniTimedTracker
import at.hannibal2.skyhanni.utils.tracker.data.TimedTrackerData
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenBpsTracker : SkyhanniTimedTracker<GardenBpsTracker.TimedData>("Garden BPS Tracker") {
    override val config get() = GardenApi.config.gardenBpsTracker
    override val perTrackerConfig: TimedPerTrackerConfig<GardenTrackerSettings> get() = config.perTrackerConfig
    override val storageAccessor: (ProfileSpecificStorage) -> TimedData = { it.garden.gardenBpsTracker }
    override val renderConfig = RenderDisplayConfig(
        condition = { isEnabled() },
    )
    override val customUptimeControl: Boolean = true

    class TimedData : TimedTrackerData<SessionUptime.Garden>() {
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
        modify { it.blocksBroken += blockBreaksLastFiveTicks }
        blockBreaksLastFiveTicks = 0
    }

    @HandleEvent
    fun onConfigLoad() {
        config.uptimeDisplayText.afterChange {
            update()
        }
    }

    private var blockBreaksLastFiveTicks = 0

    override fun drawDisplayF(data: TimedData): List<Searchable> = buildList {
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

    private fun isEnabled() = GardenApi.inGarden() && config.enabled
}
