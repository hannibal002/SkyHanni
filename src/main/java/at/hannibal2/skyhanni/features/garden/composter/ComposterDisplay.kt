package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Collections
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ComposterDisplay {

    private val config get() = GardenAPI.config.composters
    private val storage get() = GardenAPI.storage
    private var display = emptyList<List<Any>>()
    private var composterEmptyTime: Duration? = null

    private val bucket by lazy { "BUCKET".asInternalName().getItemStack() }
    private var tabListData by ComposterAPI::tabListData

    enum class DataType(rawPattern: String, val icon: String) {
        ORGANIC_MATTER(" Organic Matter: §r(.*)", "WHEAT"),
        FUEL(" Fuel: §r(.*)", "OIL_BARREL"),
        TIME_LEFT(" Time Left: §r(.*)", "WATCH"),
        STORED_COMPOST(" Stored Compost: §r(.*)", "COMPOST");

        val displayItem by lazy { icon.asInternalName().getItemStack() }

        val pattern by lazy { rawPattern.toPattern() }

        fun addToList(map: Map<DataType, String>): List<Any> {
            return listOf(displayItem, map[this]!!)
        }
    }

    private val BUCKET by lazy { "BUCKET".asInternalName().getItemStack() }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!(config.displayEnabled && GardenAPI.inGarden())) return

        readData(event.tabList)

        if (tabListData.isNotEmpty()) {
            composterEmptyTime = ComposterAPI.estimateEmptyTimeFromTab()
            updateDisplay()
            sendNotify()
        }
    }

    private fun updateDisplay() {
        val newDisplay = mutableListOf<List<Any>>()
        newDisplay.addAsSingletonList("§bComposter")


        newDisplay.add(DataType.TIME_LEFT.addToList(tabListData))

        val list = mutableListOf<Any>()
        list.addAll(DataType.ORGANIC_MATTER.addToList(tabListData))
        list.add(" ")
        list.addAll(DataType.FUEL.addToList(tabListData))
        newDisplay.add(list)

        newDisplay.add(DataType.STORED_COMPOST.addToList(tabListData))
        newDisplay.add(addComposterEmptyTime(composterEmptyTime))

        display = newDisplay
    }

    private fun addComposterEmptyTime(emptyTime: Duration?): List<Any> {
        return if (emptyTime != null) {
            GardenAPI.storage?.composterEmptyTime = System.currentTimeMillis() + emptyTime.inWholeMilliseconds
            val format = emptyTime.format()
            listOf(bucket, "§b$format")
        } else {
            listOf("§cOpen Composter Upgrades!")
        }
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

        if (ComposterAPI.getOrganicMatter() <= config.notifyLow.organicMatter && System.currentTimeMillis() >= storage.informedAboutLowMatter) {
            if (config.notifyLow.title) {
                LorenzUtils.sendTitle("§cYour Organic Matter is low", 4.seconds)
            }
            ChatUtils.chat("§cYour Organic Matter is low!")
            storage.informedAboutLowMatter = System.currentTimeMillis() + 60_000 * 5
        }

        if (ComposterAPI.getFuel() <= config.notifyLow.fuel &&
            System.currentTimeMillis() >= storage.informedAboutLowFuel
        ) {
            if (config.notifyLow.title) {
                LorenzUtils.sendTitle("§cYour Fuel is low", 4.seconds)
            }
            ChatUtils.chat("§cYour Fuel is low!")
            storage.informedAboutLowFuel = System.currentTimeMillis() + 60_000 * 5
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock && !OutsideSbFeature.COMPOSTER_TIME.isSelected()) return

        if (GardenAPI.inGarden() && config.displayEnabled) {
            config.displayPos.renderStringsAndItems(display, posLabel = "Composter Display")
        }

        checkWarningsAndOutsideGarden()
    }

    private fun checkWarningsAndOutsideGarden() {
        val format = GardenAPI.storage?.let {
            if (it.composterEmptyTime != 0L) {
                val duration = it.composterEmptyTime - System.currentTimeMillis()
                if (duration > 0) {
                    if (duration < 1000 * 60 * 20) {
                        warn("Your composter in the garden is almost empty!")
                    }
                    TimeUtils.formatDuration(duration, maxUnits = 3)
                } else {
                    warn("Your composter is empty!")
                    "§cComposter is empty!"
                }
            } else "?"
        } ?: "§cJoin SkyBlock to show composter timer."

        val inSb = LorenzUtils.inSkyBlock && config.displayOutsideGarden
        val outsideSb = !LorenzUtils.inSkyBlock && OutsideSbFeature.COMPOSTER_TIME.isSelected()
        if (!GardenAPI.inGarden() && (inSb || outsideSb)) {
            val list = Collections.singletonList(listOf(bucket, "§b$format"))
            config.outsideGardenPos.renderStringsAndItems(list, posLabel = "Composter Outside Garden Display")
        }
    }

    private fun warn(warningMessage: String) {
        if (!config.warnAlmostClose) return
        val storage = GardenAPI.storage ?: return

        if (ReminderUtils.isBusy()) return

        if (System.currentTimeMillis() < storage.lastComposterEmptyWarningTime + 1000 * 60 * 2) return
        storage.lastComposterEmptyWarningTime = System.currentTimeMillis()
        if (IslandType.GARDEN.isInIsland()) {
            ChatUtils.chat(warningMessage)
        } else {
            ChatUtils.clickableChat(warningMessage, onClick = {
                HypixelCommands.warp("garden")
            })
        }
        LorenzUtils.sendTitle("§eComposter Warning!", 3.seconds)
    }

    @SubscribeEvent
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
    }
}
