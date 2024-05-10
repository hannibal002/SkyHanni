package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.TrophyFishingDisplayConfig
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.TrophyFishingDisplayConfig.TextPart
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.CollectionUtils.sumAllValues
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

class TrophyFishingDisplay {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.display

    private val itemNameCache = mutableMapOf<String, NEUInternalName>()

    private var display = emptyList<Renderable>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        update()
    }

    fun update() {
        val list = mutableListOf<Renderable>()
        list.addString("§e§lTrophy Fish Display")
        list.add(Renderable.table(createTable(), yPadding = config.extraSpace))

        display = list
    }

    private fun createTable(): List<List<Renderable>> {
        val trophyFishes = TrophyFishManager.fish ?: return emptyList()
        val table = mutableListOf<List<Renderable>>()
        for ((rawName, data) in getOrder(trophyFishes)) {
            val displayName = getItemName(rawName)
            val map = mutableMapOf<TextPart, Renderable>()
            map[TextPart.NAME] = Renderable.string(displayName)
            for (value in TrophyRarity.entries) {
                val amount = data[value] ?: 0
                val color = value.formatCode
                val format = "$color${amount.addSeparators()}"

                map[get(value)] = Renderable.string(format)
            }
            table.add(config.textOrder.mapNotNull { map[it] })
        }
        return table
    }

    private fun get(value: TrophyRarity) = when (value) {
        TrophyRarity.BRONZE -> TextPart.BRONZE
        TrophyRarity.SILVER -> TextPart.SILVER
        TrophyRarity.GOLD -> TextPart.GOLD
        TrophyRarity.DIAMOND -> TextPart.DIAMOND
    }

    private fun getOrder(trophyFishes: MutableMap<String, MutableMap<TrophyRarity, Int>>) = sort(trophyFishes).let {
        if (config.reverseOrder) it.reversed() else it
    }

    private fun sort(trophyFishes: Map<String, MutableMap<TrophyRarity, Int>>): List<Map.Entry<String, MutableMap<TrophyRarity, Int>>> =
        when (config.sortingType) {
            TrophyFishingDisplayConfig.TrophySorting.TOTAL_AMOUNT -> trophyFishes.entries.sortedBy { it.value.sumAllValues() }

            TrophyFishingDisplayConfig.TrophySorting.BRONZE_AMOUNT -> count(trophyFishes, TrophyRarity.BRONZE)
            TrophyFishingDisplayConfig.TrophySorting.SILVER_AMOUNT -> count(trophyFishes, TrophyRarity.SILVER)
            TrophyFishingDisplayConfig.TrophySorting.GOLD_AMOUNT -> count(trophyFishes, TrophyRarity.GOLD)
            TrophyFishingDisplayConfig.TrophySorting.DIAMOND_AMOUNT -> count(trophyFishes, TrophyRarity.DIAMOND)

            TrophyFishingDisplayConfig.TrophySorting.ITEM_RARITY -> {
                trophyFishes.entries.sortedBy { data ->
                    val name = getInternalName(data.key)
                    name?.getItemStack()?.getItemRarityOrNull()
                }
            }

            TrophyFishingDisplayConfig.TrophySorting.HIGHEST_RARITY -> {
                trophyFishes.entries.sortedBy { data ->
                    TrophyRarity.entries.filter {
                        data.value.contains(it)
                    }.maxByOrNull { it.ordinal }
                }
            }

            TrophyFishingDisplayConfig.TrophySorting.NAME -> {
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
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderRenderables(display, extraSpace = config.extraSpace, posLabel = "Trophy Fishing Display")
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.enabled
}
