package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Collections
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class ComposterDisplay {
    private val config get() = SkyHanniMod.feature.garden.composters
    private val hidden get() = GardenAPI.config
    private var display = emptyList<List<Any>>()
    private var composterEmptyTime: Duration? = null

    private var tabListData by ComposterAPI::tabListData

    enum class DataType(rawPattern: String, val icon: String) {
        ORGANIC_MATTER(" Organic Matter: §r(.*)", "WHEAT"),
        FUEL(" Fuel: §r(.*)", "OIL_BARREL"),
        TIME_LEFT(" Time Left: §r(.*)", "WATCH"),
        STORED_COMPOST(" Stored Compost: §r(.*)", "COMPOST");

        val displayItem by lazy {
            NEUItems.getItemStack(icon, true)
        }

        val pattern by lazy { rawPattern.toPattern() }

        fun addToList(map: Map<DataType, String>): List<Any> {
            return listOf(displayItem, map[this]!!)
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!(config.displayEnabled && GardenAPI.inGarden())) return

        readData(event.tabList)

        if (tabListData.isNotEmpty()) {
            calculateEmptyTime()
            updateDisplay()
            sendNotify()
        }
    }

    private fun calculateEmptyTime() {
        val organicMatter = ComposterAPI.getOrganicMatter()
        val fuel = ComposterAPI.getFuel()

        if (ComposterAPI.composterUpgrades.isNullOrEmpty()) {
            composterEmptyTime = null
            return
        }

        val timePerCompost = ComposterAPI.timePerCompost(null)

        val organicMatterRequired = ComposterAPI.organicMatterRequiredPer(null)
        val fuelRequired = ComposterAPI.fuelRequiredPer(null)

        val organicMatterRemaining = floor(organicMatter / organicMatterRequired)
        val fuelRemaining = floor(fuel / fuelRequired)

        val endOfOrganicMatter = timePerCompost * organicMatterRemaining
        val endOfFuel = timePerCompost * fuelRemaining
        composterEmptyTime = if (endOfOrganicMatter > endOfFuel) endOfFuel else endOfOrganicMatter
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
            val millis = emptyTime.toDouble(DurationUnit.MILLISECONDS).toLong()
            GardenAPI.config?.composterEmptyTime = System.currentTimeMillis() + millis
            val format = TimeUtils.formatDuration(millis, maxUnits = 2)
            listOf(NEUItems.getItemStack("BUCKET", true), "§b$format")
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
        val hidden = hidden ?: return

        if (ComposterAPI.getOrganicMatter() <= config.notifyLow.organicMatter && System.currentTimeMillis() >= hidden.informedAboutLowMatter) {
            if (config.notifyLow.title) {
                LorenzUtils.sendTitle("§cYour Organic Matter is low", 4.seconds)
            }
            LorenzUtils.chat("§cYour Organic Matter is low!")
            hidden.informedAboutLowMatter = System.currentTimeMillis() + 60_000 * 5
        }

        if (ComposterAPI.getFuel() <= config.notifyLow.fuel &&
            System.currentTimeMillis() >= hidden.informedAboutLowFuel
        ) {
            if (config.notifyLow.title) {
                LorenzUtils.sendTitle("§cYour Fuel is low", 4.seconds)
            }
            LorenzUtils.chat("§cYour Fuel is low!")
            hidden.informedAboutLowFuel = System.currentTimeMillis() + 60_000 * 5
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (GardenAPI.inGarden() && config.displayEnabled) {
            config.displayPos.renderStringsAndItems(display, posLabel = "Composter Display")
        }

        checkWarningsAndOutsideGarden()
    }

    private fun checkWarningsAndOutsideGarden() {
        val storage = GardenAPI.config ?: return

        val format = if (storage.composterEmptyTime != 0L) {
            val duration = storage.composterEmptyTime - System.currentTimeMillis()
            if (duration > 0) {
                if (duration < 1000 * 60 * 20) {
                    warn("Your composter in the garden is soon empty!")
                }
                TimeUtils.formatDuration(duration, maxUnits = 3)
            } else {
                warn("Your composter is empty!")
                "§cComposter is empty!"
            }
        } else "?"

        if (!GardenAPI.inGarden() && config.displayOutsideGarden) {
            val list = Collections.singletonList(listOf(NEUItems.getItemStack("BUCKET", true), "§b$format"))
            config.outsideGardenPos.renderStringsAndItems(list, posLabel = "Composter Outside Garden Display")
        }
    }

    private fun warn(warningMessage: String) {
        if (!config.warnAlmostClose) return
        val storage = GardenAPI.config ?: return

        if (LorenzUtils.inDungeons) return
        if (LorenzUtils.inKuudraFight) return

        if (System.currentTimeMillis() < storage.lastComposterEmptyWarningTime + 1000 * 60 * 2) return
        storage.lastComposterEmptyWarningTime = System.currentTimeMillis()
        LorenzUtils.chat(warningMessage)
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
