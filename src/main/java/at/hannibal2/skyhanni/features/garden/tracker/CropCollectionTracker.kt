package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.cropcollections.CropCollectionsConfig.CropCollectionDisplayText
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.CropCollectionApi
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.getCollection
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.inPartialHours
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addNullableButton
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyhanniTimedTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import java.util.EnumMap
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CropCollectionTracker {
    private val config get() = GardenApi.config.cropCollectionTracker
    val tracker = SkyhanniTimedTracker(
        "Crop Collection Tracker",
        { Data() },
        { it.garden.cropCollectionTracker },
        { drawDisplay(it) },
        customUptimeControl = true,
        trackerConfig = { config.perTrackerConfig },
    )

    class TimedData : TimedTrackerData<Data, SessionUptime.Garden>(SessionUptime.Garden::class, { Data() })

    class Data : TrackerData<SessionUptime.Garden>(SessionUptime.Garden::class) {
        override fun resetData() {
            cropCollection.clear()
        }

        @Expose
        var cropCollection: MutableMap<CropType, CropCollectionApi.CropCollection> = EnumMap(CropType::class.java)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.statDisplayList) {
            tracker.update()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGardenJoin(event: IslandChangeEvent) {
        tracker.update()
    }

    @HandleEvent
    fun onCollectionAdd(event: CropCollectionAddEvent) {
        val crop = event.crop
        val type = event.cropCollectionType
        val amount = event.amount
        if (type == CropCollectionType.UNKNOWN || amount <= 0) {
            tracker.update()
            return
        }
        tracker.modify {
            val cropType = it.cropCollection.getOrPut(crop) { CropCollectionApi.CropCollection() }
            cropType.cropCollectionType[type] = cropType.cropCollectionType.getOrDefault(type, 0) + amount
        }
    }

    private var cropDisplayMode: CropType? = null

    private fun getDisplayCrop() = cropDisplayMode

    // Todo make bucketed tracker
    private fun drawDisplay(data: Data): List<Searchable> {
        val crop: CropType = cropDisplayMode ?: CropCollectionApi.lastGainedCrop ?: return emptyList()
        val allTime = crop.getCollection()
        val cropData = data.cropCollection.getOrPut(crop) { CropCollectionApi.CropCollection() }
        val lineMap = mutableMapOf<CropCollectionDisplayText, Searchable>()

        lineMap[CropCollectionDisplayText.TITLE] = Renderable.horizontal(
            buildList {
                addItemStack(crop.icon)
                addString("§6 ${crop.cropName} Collection")
            }
        ).toSearchable()

        lineMap[CropCollectionDisplayText.ALL_TIME] = StringRenderable("§7All-Time: §e${allTime.addSeparators()}").toSearchable()

        val total: Long = cropData.getTotal()
        lineMap[CropCollectionDisplayText.SESSION] =
            Renderable.hoverTips(
                "§7${tracker.dateString()}: §e${total.addSeparators()}", buildCropBreakdown(cropData)
            ).toSearchable()

        val uptime = data.getTotalUptime()

        if (uptime > 0.seconds) {
            val collectionPerHour = total / uptime.inPartialHours
            lineMap[CropCollectionDisplayText.PER_HOUR] =
                StringRenderable("§7Per hour: §e${collectionPerHour.toLong().addSeparators()}").toSearchable()
        }

        lineMap[CropCollectionDisplayText.BREAKDOWN] = StringRenderable("§6§lCollection Breakdown").toSearchable()

        val farming = cropData.getCollection(CropCollectionType.MOOSHROOM_COW) +
            cropData.getCollection(CropCollectionType.BREAKING_CROPS) +
            cropData.getCollection(CropCollectionType.DICER)

        lineMap[CropCollectionDisplayText.FARMING] = StringRenderable("§7Farming: §e${farming.addSeparators()}").toSearchable()

        lineMap[CropCollectionDisplayText.BREAKING_CROPS] =
            StringRenderable(
                "§7- Breaking Crops: §e${cropData.getCollection(CropCollectionType.BREAKING_CROPS).addSeparators()}"
            ).toSearchable()

        if (crop == CropType.MUSHROOM) lineMap[CropCollectionDisplayText.MOOSHROOM_COW] =
            StringRenderable(
                "§7- Mooshroom Cow: §e${cropData.getCollection(CropCollectionType.MOOSHROOM_COW).addSeparators()}"
            ).toSearchable()

        lineMap[CropCollectionDisplayText.DICER] =
            StringRenderable(
                "§7- Dicer Drops: §e${cropData.getCollection(CropCollectionType.DICER).addSeparators()}"
            ).toSearchable()

        val pests = cropData.getCollection(CropCollectionType.PEST_BASE) + cropData.getCollection(CropCollectionType.PEST_RNG)
        lineMap[CropCollectionDisplayText.PESTS] = StringRenderable("§7Pests: §e${pests.addSeparators()}").toSearchable()

        lineMap[CropCollectionDisplayText.PEST_BASE] =
            StringRenderable(
                "§7- Pest Base Drops: §e${cropData.getCollection(CropCollectionType.PEST_BASE).addSeparators()}"
            ).toSearchable()

        lineMap[CropCollectionDisplayText.PEST_RNG] =
            StringRenderable(
                "§7- Pest Crop RNG: §e${cropData.getCollection(CropCollectionType.PEST_RNG).addSeparators()}"
            ).toSearchable()

        return formatDisplay(lineMap)
    }

    private fun buildCropBreakdown(cropData: CropCollectionApi.CropCollection) = buildList {

        add("§6§lCollection Breakdown")

        val farming = cropData.getCollection(CropCollectionType.MOOSHROOM_COW) +
            cropData.getCollection(CropCollectionType.BREAKING_CROPS) +
            cropData.getCollection(CropCollectionType.DICER)

        add("§7Farming: §e${farming.addSeparators()}")

        add("§7- Breaking Crops: §e${cropData.getCollection(CropCollectionType.BREAKING_CROPS).addSeparators()}")

        add("§7- Mooshroom Cow: §e${cropData.getCollection(CropCollectionType.MOOSHROOM_COW).addSeparators()}")

        add("§7- Dicer Drops: §e${cropData.getCollection(CropCollectionType.DICER).addSeparators()}")

        val pests = cropData.getCollection(CropCollectionType.PEST_BASE) + cropData.getCollection(CropCollectionType.PEST_RNG)
        add("§2Pests: §e${pests.addSeparators()}")

        add("§7- Pest Base Drops: §e${cropData.getCollection(CropCollectionType.PEST_BASE).addSeparators()}")

        add("§7- Pest Crop RNG: §e${cropData.getCollection(CropCollectionType.PEST_RNG).addSeparators()}")
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (GardenApi.hideExtraGuis()) return

        tracker.renderDisplay(config.collectionDisplayPos)
    }

    private fun MutableList<Searchable>.buildCropSwitcher() {
        this.addNullableButton(
            label = "Crop Type",
            current = getDisplayCrop(),
            nullLabel = "Default",
            onChange = { new ->
                cropDisplayMode = new
                tracker.update()
            },
            universe = CropType.entries,
            enableUniverseScroll = false // would infinitely scroll while hovered
        )
    }

    private fun formatDisplay(lineMap: MutableMap<CropCollectionDisplayText, Searchable>): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        if (tracker.isInventoryOpen()) newList.buildCropSwitcher() else newList.add(Renderable.placeholder(10).toSearchable())
        newList.addAll(config.statDisplayList.get().mapNotNull { lineMap[it] })
        return newList
    }

    private fun isEnabled() = config.collectionDisplay && GardenApi.inGarden()

}
