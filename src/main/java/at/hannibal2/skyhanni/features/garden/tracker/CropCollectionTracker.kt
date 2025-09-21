package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.cropcollections.CropCollectionsConfig.CropCollectionDisplayText
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.CropCollectionApi
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.getCollection
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DateChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.garden.DisplayCropChange
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionUpdateEvent
import at.hannibal2.skyhanni.events.utils.TimedTrackerUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.storage
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.inPartialHours
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.SkyhanniTimedTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import java.util.EnumMap
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CropCollectionTracker {
    private val config get() = GardenApi.config.cropCollections
    private val tracker = SkyhanniTimedTracker(
        "Crop Collection Tracker",
        { Data() },
        { it.garden.cropCollectionTracker },
        { drawDisplay(it) },
        trackerConfig = { config.perTrackerConfig },
    )

    class Data : TrackerData<SessionUptime.Garden>(SessionUptime.Garden::class) {
        override fun resetData() {
            cropCollection.clear()
        }

        @Expose
        var cropCollection: MutableMap<CropType, CropCollection> = EnumMap(CropType::class.java)
    }

    class CropCollection {
        fun getTotal(): Long {
            return cropCollectionType.sumAllValues().toLong()
        }

        fun getCollection(collectionType: CropCollectionType): Long {
            return cropCollectionType.getOrPut(collectionType) { 0 }
        }

        @Expose
        var cropCollectionType: MutableMap<CropCollectionType, Long> = EnumMap(CropCollectionType::class.java)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        tracker.update()
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
            val cropType = it.cropCollection.getOrPut(crop) { CropCollection() }
            cropType.cropCollectionType[type] = cropType.cropCollectionType.getOrDefault(type, 0) + amount
        }
    }

    private var cropDisplayMode: CropType? = null

    private fun selectNextCrop() {
        cropDisplayMode = if (cropDisplayMode == null) CropType.entries.first()
        else cropDisplayMode?.let { sb ->
            CropType.entries.filter { it.ordinal > sb.ordinal }.minByOrNull { it.ordinal }
        }
    }

    // Todo add bucketed tracker
    private fun drawDisplay(data: Data): List<Searchable> {
        val crop: CropType = cropDisplayMode ?: CropCollectionApi.lastGainedCrop ?: return emptyList()
        val allTime = crop.getCollection()
        val cropData = data.cropCollection.getOrPut(crop) { CropCollection() }
        val lineMap = mutableMapOf<CropCollectionDisplayText, Searchable>()
        val displayMode = tracker.displayMode ?: return emptyList()
        val date = when (displayMode) {
            SkyHanniTracker.DisplayMode.WEEK -> tracker.week
            SkyHanniTracker.DisplayMode.MONTH -> tracker.month
            SkyHanniTracker.DisplayMode.YEAR -> tracker.year
            else -> tracker.date
        }

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

        val uptimeData = storage?.uptimeTracker?.getEntry(displayMode, date)

        // Todo respect config
        if (uptimeData != null) {
            val uptime = uptimeData.pestTime + uptimeData.visitorTime + uptimeData.cropBreakTime
            if (uptime != 0) {
                val collectionPerHour = total / uptime.seconds.inPartialHours
                lineMap[CropCollectionDisplayText.PER_HOUR] =
                    StringRenderable("§7Per hour: §e${collectionPerHour.toLong().addSeparators()}").toSearchable()
            }
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

    private fun buildCropBreakdown(cropData: CropCollection) = buildList {

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

    private fun formatDisplay(lineMap: MutableMap<CropCollectionDisplayText, Searchable>): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        if (tracker.isInventoryOpen()) {
            newList.add(
                Renderable.clickable(
                    "§7[§a${cropDisplayMode ?: "Default"}§7]",
                    tips = listOf("Click for next crop"),
                    onLeftClick = {
                        selectNextCrop()
                        tracker.update()
                        DisplayCropChange(cropDisplayMode).post()
                    }
                ).toSearchable()
            )
        }
        newList.addAll(config.statDisplayList.mapNotNull { lineMap[it] })
        return newList
    }

    private fun isEnabled() = config.collectionDisplay && GardenApi.inGarden()

}
