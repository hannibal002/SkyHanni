package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addHorizontalSpacer
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.addLine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ComposterDisplay {

    private val config get() = GardenApi.config.composters
    private val storage get() = GardenApi.storage
    private var display: Renderable? = null
    private var composterEmptyTime: Duration? = null

    private val bucket by lazy { "BUCKET".toInternalName().getItemStack() }
    private var tabListData by ComposterApi::tabListData

    enum class DataType(rawPattern: String, val icon: String) {
        ORGANIC_MATTER(" Organic Matter: §r(.*)", "WHEAT"),
        FUEL(" Fuel: §r(.*)", "OIL_BARREL"),
        TIME_LEFT(" Time Left: §r(.*)", "WATCH"),
        STORED_COMPOST(" Stored Compost: §r(.*)", "COMPOST");

        val displayItem by lazy { icon.toInternalName().getItemStack() }

        val pattern = rawPattern.toPattern()

        fun label(label: String) = Renderable.line {
            addItemStack(displayItem)
            addString(label)
        }

        fun labeledWithData(map: Map<DataType, String>): Renderable? {
            return map[this]?.let { label(it) }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.COMPOSTER)) return

        readData(event.lines)

        if (tabListData.isNotEmpty()) {
            composterEmptyTime = ComposterApi.estimateEmptyTimeFromTab()
            updateDisplay()
            sendNotify()
        }
    }

    private fun updateDisplay() {
        if (!config.displayEnabled) return
        display = Renderable.vertical {
            addString("§bComposter")
            addNotNull(DataType.TIME_LEFT.labeledWithData(tabListData))
            addLine {
                addNotNull(DataType.ORGANIC_MATTER.labeledWithData(tabListData))
                addHorizontalSpacer()
                addNotNull(DataType.FUEL.labeledWithData(tabListData))
            }
            addNotNull(DataType.STORED_COMPOST.labeledWithData(tabListData))
            add(composterEmptyTime(composterEmptyTime))
        }
    }

    private fun composterEmptyTime(emptyTime: Duration?): Renderable {
        return if (emptyTime != null) {
            GardenApi.storage?.composterEmptyTime = emptyTime.fromNow()
            val format = emptyTime.format()
            Renderable.line {
                addItemStack(bucket)
                addString("§b$format")
            }
        } else Renderable.string("§cOpen Composter Upgrades!")
    }

    private fun readData(tabList: List<String>) {
        var next = false
        val newData = mutableMapOf<DataType, String>()

        for (line in tabList) {
            if (line == "§b§lComposter:") {
                next = true
                continue
            }
            if (next) {
                if (line == "") break
                for (type in DataType.entries) {
                    type.pattern.matchMatcher(line) {
                        newData[type] = group(1)
                    }
                }
            }
        }

        for (type in DataType.entries) {
            if (!newData.containsKey(type)) {
                tabListData = emptyMap()
                return
            }
        }

        tabListData = newData
    }

    private fun sendNotify() {
        if (!config.notifyLow.enabled) return
        if (ReminderUtils.isBusy()) return

        val storage = storage ?: return

        if (ComposterApi.getOrganicMatter() <= config.notifyLow.organicMatter && storage.informedAboutLowMatter.isInPast()) {
            if (config.notifyLow.title) {
                TitleManager.sendTitle("§cYour Organic Matter is low", duration = 4.seconds)
            }
            ChatUtils.chat("§cYour Organic Matter is low!")
            storage.informedAboutLowMatter = 5.0.minutes.fromNow()
        }

        if (ComposterApi.getFuel() <= config.notifyLow.fuel && storage.informedAboutLowFuel.isInPast()) {
            if (config.notifyLow.title) {
                TitleManager.sendTitle("§cYour Fuel is low", duration = 4.seconds)
            }
            ChatUtils.chat("§cYour Fuel is low!")
            storage.informedAboutLowFuel = 5.0.minutes.fromNow()
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        @Suppress("InSkyBlockEarlyReturn")
        if (!LorenzUtils.inSkyBlock && !OutsideSBFeature.COMPOSTER_TIME.isSelected()) return

        if (GardenApi.inGarden() && config.displayEnabled) {
            config.displayPos.renderRenderable(display, posLabel = "Composter Display")
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(5)) return
        @Suppress("InSkyBlockEarlyReturn")
        if (!LorenzUtils.inSkyBlock && !OutsideSBFeature.COMPOSTER_TIME.isSelected()) return

        val format = GardenApi.storage?.let {
            if (!it.composterEmptyTime.isFarPast()) {
                val duration = it.composterEmptyTime.timeUntil()
                if (duration > 0.0.seconds) {
                    if (duration < 20.0.minutes) {
                        warn("Your composter in the garden is almost empty!")
                    }
                    duration.format(maxUnits = 3)
                } else {
                    warn("Your composter is empty!")
                    "§cComposter is empty!"
                }
            } else "?"
        } ?: "§cJoin SkyBlock to show composter timer."

        val inSB = LorenzUtils.inSkyBlock && config.displayOutsideGarden
        val outsideSB = !LorenzUtils.inSkyBlock && OutsideSBFeature.COMPOSTER_TIME.isSelected()
        if (!GardenApi.inGarden() && (inSB || outsideSB)) {
            val outsideGardenDisplay = Renderable.line {
                addItemStack(bucket)
                addString("§b$format")
            }
            config.outsideGardenPos.renderRenderable(outsideGardenDisplay, posLabel = "Composter Outside Garden")
        }
    }

    private fun warn(warningMessage: String) {
        if (!config.warnAlmostEmpty) return
        val storage = GardenApi.storage ?: return

        if (ReminderUtils.isBusy()) return
        if (storage.lastComposterEmptyWarningTime.passedSince() < 2.0.minutes) return
        storage.lastComposterEmptyWarningTime = SimpleTimeMark.now()
        if (IslandType.GARDEN.isInIsland()) {
            ChatUtils.chat(warningMessage)
        } else {
            ChatUtils.clickToActionOrDisable(
                warningMessage,
                config::warnAlmostEmpty,
                actionName = "warp to the Garden",
                action = { HypixelCommands.warp("garden") },
            )
        }
        TitleManager.sendTitle("§eComposter Warning!")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.composterDisplayEnabled", "garden.composters.displayEnabled")
        event.move(3, "garden.composterDisplayOutsideGarden", "garden.composters.displayOutsideGarden")
        event.move(3, "garden.composterWarnAlmostClose", "garden.composters.warnAlmostClose")
        event.move(3, "garden.composterDisplayPos", "garden.composters.displayPos")
        event.move(3, "garden.composterOutsideGardenPos", "garden.composters.outsideGardenPos")
        event.move(3, "garden.composterNotifyLowEnabled", "garden.composters.notifyLow.enabled")
        event.move(3, "garden.composterNotifyLowEnabled", "garden.composters.notifyLow.enabled")
        event.move(3, "garden.composterNotifyLowTitle", "garden.composters.notifyLow.title")
        event.move(3, "garden.composterNotifyLowOrganicMatter", "garden.composters.notifyLow.organicMatter")
        event.move(3, "garden.composterNotifyLowFuel", "garden.composters.notifyLow.fuel")

        event.move(85, "garden.composters.warnAlmostClose", "garden.composters.warnAlmostEmpty")
    }
}
