package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class ComposterDisplay {
    private val config get() = SkyHanniMod.feature.garden
    private var display = listOf<List<Any>>()

    enum class DataType(val pattern: String, val icon: String) {
        ORGANIC_MATTER(" Organic Matter: §r(.*)", "WHEAT"),
        FUEL(" Fuel: §r(.*)", "OIL_BARREL"),
        TIME_LEFT(" Time Left: §r(.*)", "WATCH"),
        STORED_COMPOST(" Stored Compost: §r(.*)", "COMPOST");

        val displayItem by lazy {
            NEUItems.getItemStack(icon)
        }

        fun addToList(map: Map<DataType, String>): List<Any> {
            return listOf(displayItem, map[this]!!)
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        var next = false
        val data = mutableMapOf<DataType, String>()

        for (line in event.tabList) {
            if (line == "§b§lComposter:") {
                next = true
                continue
            }
            if (next) {
                if (line == "") break
                for (type in DataType.values()) {
                    val pattern = Pattern.compile(type.pattern)
                    val matcher = pattern.matcher(line)
                    if (matcher.matches()) {
                        data[type] = matcher.group(1)
                    }
                }
            }
        }

        val newList = mutableListOf<List<Any>>()
        newList.addAsSingletonList("§bComposter")

        for (type in DataType.values()) {
            if (!data.containsKey(type)) return
        }

        newList.add(DataType.TIME_LEFT.addToList(data))

        val list = mutableListOf<Any>()
        list.addAll(DataType.ORGANIC_MATTER.addToList(data))
        list.add(" ")
        list.addAll(DataType.FUEL.addToList(data))
        newList.add(list)

        newList.add(DataType.STORED_COMPOST.addToList(data))

        display = newList

        if(!config.composterNotify) return

        val organicMatterValue = data[DataType.ORGANIC_MATTER]!!.removeColor().formatNumber()
        val fuelValue = data[DataType.FUEL]!!.removeColor().formatNumber()
        notify(organicMatterValue, fuelValue)
    }

    private fun notify(organicMatterValue: Long, fuelValue: Long) {
        if (organicMatterValue <= config.notifyWhenOrganicMatterLow) {
            if (System.currentTimeMillis() >= SkyHanniMod.feature.hidden.informedAboutLowMatter) {
                SendTitleHelper.sendTitle("§cYour Organic Matter is low", 4_000)
                LorenzUtils.chat("§e[SkyHanni] §cYour Organic Matter is low!")
                SkyHanniMod.feature.hidden.informedAboutLowMatter = System.currentTimeMillis()+30_000
            }
        }

        if (fuelValue <= config.notifyWhenFuelLow &&
            System.currentTimeMillis() >= SkyHanniMod.feature.hidden.informedAboutLowFuel) {
            SendTitleHelper.sendTitle("§cYour Fuel is low", 4_000)
            LorenzUtils.chat("§e[SkyHanni] §cYour Fuel is low!")
            SkyHanniMod.feature.hidden.informedAboutLowFuel = System.currentTimeMillis()+30_000
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (isEnabled()) {
            config.composterDisplayPos.renderStringsAndItems(display, posLabel = "Composter Display")
        }
    }

    fun isEnabled() = config.composterDisplayEnabled && GardenAPI.inGarden()
}