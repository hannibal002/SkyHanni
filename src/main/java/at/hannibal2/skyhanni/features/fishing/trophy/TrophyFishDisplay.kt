package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.TrophyFishDisplayConfig
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.TrophyFishDisplayConfig.HideCaught
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.TrophyFishDisplayConfig.TextPart
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.fishing.TrophyFishCaughtEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.addSingleString
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class TrophyFishDisplay {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.display

    private val itemNameCache = mutableMapOf<String, NEUInternalName>()

    private var display = emptyList<Renderable>()

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.CRIMSON_ISLE) {
            DelayedRun.runDelayed(200.milliseconds) {
                update()
            }
        }
    }

    @SubscribeEvent
    fun onTrophyFishCaught(event: TrophyFishCaughtEvent) {
        update()
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
        update()
    }

    @SubscribeEvent
    fun onConfigReload(event: ConfigLoadEvent) {
        with(config) {
            ConditionalUtils.onToggle(
                enabled,
                extraSpace,
                sortingType,
                reverseOrder,
                textOrder,
                onlyShowMissing,
            ) {
                update()
            }
        }
    }

    fun update() {
        if (!isEnabled()) return
        val list = mutableListOf<Renderable>()
        list.addString("§e§lTrophy Fish Display")
        list.add(Renderable.table(createTable(), yPadding = config.extraSpace.get()))

        display = list
    }

    private fun createTable(): List<List<Renderable>> {
        val trophyFishes = TrophyFishManager.fish ?: return emptyList()
        val table = mutableListOf<List<Renderable>>()
        for ((rawName, data) in getOrder(trophyFishes)) {
            addRow(rawName, data, table)
        }
        if (table.isEmpty()) {
            get(config.onlyShowMissing.get())?.let {
                val name = it.formattedString
                table.addSingleString("§eYou caught all $name Trophy Fishes")
                if (it != TrophyRarity.DIAMOND) {
                    table.addSingleString("§cchange §eOnly Show Missing §cin the config to show more.")
                }
            }
        }
        return table
    }

    private fun addRow(
        rawName: String,
        data: MutableMap<TrophyRarity, Int>,
        table: MutableList<List<Renderable>>,
    ) {
        get(config.onlyShowMissing.get())?.let { atLeast ->
            val list = TrophyRarity.entries.filter { it <= atLeast }
            if (list.all { (data[it] ?: 0) > 0 }) {
                return
            }
        }

        val row = mutableMapOf<TextPart, Renderable>()
        row[TextPart.NAME] = Renderable.string(getItemName(rawName))

        val internalName = getInternalName(rawName)
        row[TextPart.ICON] = Renderable.itemStack(internalName.getItemStack())

        for (value in TrophyRarity.entries) {
            val amount = data[value] ?: 0
            val color = value.formatCode
            val format = "$color${amount.addSeparators()}"
            row[get(value)] = Renderable.string(format)
        }
        val total = data.sumAllValues()
        row[TextPart.TOTAL] = Renderable.string("§5${total.addSeparators()}")

        table.add(config.textOrder.get().mapNotNull { row[it] })
    }

    private fun get(value: TrophyRarity) = when (value) {
        TrophyRarity.BRONZE -> TextPart.BRONZE
        TrophyRarity.SILVER -> TextPart.SILVER
        TrophyRarity.GOLD -> TextPart.GOLD
        TrophyRarity.DIAMOND -> TextPart.DIAMOND
    }

    private fun get(value: HideCaught) = when (value) {
        HideCaught.NONE -> null
        HideCaught.BRONZE -> TrophyRarity.BRONZE
        HideCaught.SILVER -> TrophyRarity.SILVER
        HideCaught.GOLD -> TrophyRarity.GOLD
        HideCaught.DIAMOND -> TrophyRarity.DIAMOND
    }

    private fun getOrder(trophyFishes: MutableMap<String, MutableMap<TrophyRarity, Int>>) = sort(trophyFishes).let {
        if (config.reverseOrder.get()) it.reversed() else it
    }

    private fun sort(trophyFishes: Map<String, MutableMap<TrophyRarity, Int>>): List<Map.Entry<String, MutableMap<TrophyRarity, Int>>> =
//     @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (config.sortingType.get()!!) {
            TrophyFishDisplayConfig.TrophySorting.TOTAL_AMOUNT -> trophyFishes.entries.sortedBy { it.value.sumAllValues() }

            TrophyFishDisplayConfig.TrophySorting.BRONZE_AMOUNT -> count(trophyFishes, TrophyRarity.BRONZE)
            TrophyFishDisplayConfig.TrophySorting.SILVER_AMOUNT -> count(trophyFishes, TrophyRarity.SILVER)
            TrophyFishDisplayConfig.TrophySorting.GOLD_AMOUNT -> count(trophyFishes, TrophyRarity.GOLD)
            TrophyFishDisplayConfig.TrophySorting.DIAMOND_AMOUNT -> count(trophyFishes, TrophyRarity.DIAMOND)

            TrophyFishDisplayConfig.TrophySorting.ITEM_RARITY -> {
                trophyFishes.entries.sortedBy { data ->
                    val name = getInternalName(data.key)
                    name.getItemStack().getItemRarityOrNull()
                }
            }

            TrophyFishDisplayConfig.TrophySorting.HIGHEST_RARITY -> {
                trophyFishes.entries.sortedBy { data ->
                    TrophyRarity.entries.filter {
                        data.value.contains(it)
                    }.maxByOrNull { it.ordinal }
                }
            }

            TrophyFishDisplayConfig.TrophySorting.NAME -> {
                trophyFishes.entries.sortedBy { data ->
                    getItemName(data.key).removeColor()
                }
            }
        }

    private fun count(
        trophyFishes: Map<String, MutableMap<TrophyRarity, Int>>, rarity: TrophyRarity,
    ) = trophyFishes.entries.sortedBy { it.value[rarity] ?: 0 }

    private fun getItemName(rawName: String): String {
        val name = getInternalName(rawName).itemName
        return name.split(" ").dropLast(1).joinToString(" ").replace("§k", "")
    }

    private fun getInternalName(name: String): NEUInternalName {
        itemNameCache[name]?.let {
            return it
        }
        // getOrPut does not support our null check
        readInternalName(name)?.let {
            itemNameCache[name] = it
            return it
        }

        ErrorManager.skyHanniError(
            "No Trophy Fishing name found",
            "name" to name
        )
    }

    private fun readInternalName(rawName: String): NEUInternalName? {
        for ((name, internalName) in NEUItems.allItemsCache) {
            val test = name.removeColor().replace(" ", "").replace("-", "")
            if (test.startsWith(rawName)) {
                return internalName
            }
        }
        if (rawName.endsWith("1")) return "OBFUSCATED_FISH_1_BRONZE".asInternalName()
        if (rawName.endsWith("2")) return "OBFUSCATED_FISH_2_BRONZE".asInternalName()
        if (rawName.endsWith("3")) return "OBFUSCATED_FISH_3_BRONZE".asInternalName()

        return null
    }

    @SubscribeEvent
    fun onGuiRender(event: GuiRenderEvent) {
        if (!isEnabled()) return
        config.position.renderRenderables(
            display,
            extraSpace = config.extraSpace.get(),
            posLabel = "Trophy Fishing Display"
        )
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.enabled.get()
}
