package at.hannibal2.skyhanni.features.fishing.trophyfrog

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.fishing.trophyfrog.TrophyFrogDisplayConfig.HideCaught
import at.hannibal2.skyhanni.config.features.fishing.trophyfrog.TrophyFrogDisplayConfig.TextPart
import at.hannibal2.skyhanni.config.features.fishing.trophyfrog.TrophyFrogDisplayConfig.TrophySorting
import at.hannibal2.skyhanni.config.features.fishing.trophyfrog.TrophyFrogDisplayConfig.WhenToShow
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.fishing.TrophyFrogCaughtEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSingleString
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.InventoryGuiScaleCompat
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TrophyFrogDisplay {
    private val config get() = SkyHanniMod.feature.fishing.trophyFrogs.display

    private val recentlyDroppedFrogs = TimeLimitedCache<String, TrophyRarity>(5.seconds)

    private var display = emptyList<Renderable>()

    @HandleEvent(onlyOnIsland = LOTUS_ATOLL)
    private fun onIslandJoin() {
        DelayedRun.runDelayed(200.milliseconds) {
            update()
        }
    }

    @HandleEvent
    private fun onTrophyFrogCaught(event: TrophyFrogCaughtEvent) {
        recentlyDroppedFrogs[event.trophyFrogName] = event.rarity
        update()
        DelayedRun.runDelayed(5.1.seconds) {
            update()
        }
    }

    @HandleEvent
    private fun onProfileJoin() {
        display = emptyList()
        update()
    }

    @HandleEvent
    private fun onConfigLoad() {
        with(config) {
            ConditionalUtils.onToggle(
                enabled,
                highlightNew,
                extraSpace,
                sortingType,
                reverseOrder,
                textOrder,
                showCross,
                showCheckmark,
                onlyShowMissing,
                showCaughtHigher,
                requireArmor,
            ) {
                update()
            }
        }
    }

    fun update() {
        if (!isEnabled()) return
        val list = mutableListOf<Renderable>()
        list.addString("§e§lTrophy Frog Display")
        list.add(Renderable.table(createTable(), ySpacing = config.extraSpace.get()))
        display = list
    }

    private fun createTable(): List<List<Renderable>> {
        val trophyFrogs = TrophyFrogManager.frog ?: return emptyList()
        val table = mutableListOf<List<Renderable>>()

        if (trophyFrogs.isEmpty()) {
            table.addSingleString("§cNo Trophy data found!")
            table.addSingleString("§eTalk to Researcher Ribery to load the data!")
            return table
        }

        for ((rawName, data) in getOrder(trophyFrogs)) {
            addRow(rawName, data, table)
        }
        if (table.isNotEmpty()) return table

        get(config.onlyShowMissing.get())?.let { rarity ->
            val name = rarity.formattedString
            table.addSingleString("§eYou caught all $name Trophy Frogs")
            if (rarity != TrophyRarity.DIAMOND) {
                table.addSingleString("§cChange §eOnly Show Missing §cin the config to show more.")
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
            val list = TrophyRarity.entries.filter { it == atLeast || (!config.showCaughtHigher.get() && it > atLeast) }
            if (list.any { (data[it] ?: 0) > 0 }) {
                return
            }
        }

        val hover = TrophyFrogApi.hoverInfo(rawName)
        fun string(string: String): Renderable = hover?.let {
            Renderable.hoverTips(
                Renderable.text(string),
                tips = it.split("\n"),
            )
        } ?: Renderable.text(string)

        val row = mutableMapOf<TextPart, Renderable>()
        row[TextPart.NAME] = string(TrophyFrogManager.getDisplayName(rawName))
        row[TextPart.ICON] = Renderable.item(TrophyFrogManager.getInternalName(rawName))

        val recentlyDroppedRarity = recentlyDroppedFrogs[rawName]?.takeIf { config.highlightNew.get() }

        for (rarity in TrophyRarity.entries) {
            val amount = data[rarity] ?: 0
            val recentlyDropped = rarity == recentlyDroppedRarity
            val format = if (config.showCross.get() && amount == 0) "§c✖" else {
                val color = if (recentlyDropped) "§a" else rarity.formatCode
                val numberFormat = if (config.showCheckmark.get() && amount >= 1) "§l✔" else amount.addSeparators()
                "$color$numberFormat"
            }
            row[get(rarity)] = string(format)
        }
        val total = data.sumAllValues()
        val color = if (recentlyDroppedRarity != null) "§a" else "§5"
        row[TextPart.TOTAL] = string("$color${total.addSeparators()}")

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

    private fun getOrder(trophyFrogs: Map<String, MutableMap<TrophyRarity, Int>>) = sort(trophyFrogs).let {
        if (config.reverseOrder.get()) it.reversed() else it
    }

    private fun sort(trophyFrogs: Map<String, MutableMap<TrophyRarity, Int>>): List<Map.Entry<String, MutableMap<TrophyRarity, Int>>> =
        when (config.sortingType.get()!!) {
            TrophySorting.TOTAL_AMOUNT -> trophyFrogs.entries.sortedBy { it.value.sumAllValues() }

            TrophySorting.BRONZE_AMOUNT -> count(trophyFrogs, TrophyRarity.BRONZE)
            TrophySorting.SILVER_AMOUNT -> count(trophyFrogs, TrophyRarity.SILVER)
            TrophySorting.GOLD_AMOUNT -> count(trophyFrogs, TrophyRarity.GOLD)
            TrophySorting.DIAMOND_AMOUNT -> count(trophyFrogs, TrophyRarity.DIAMOND)

            TrophySorting.ITEM_RARITY -> {
                trophyFrogs.entries.sortedBy { data ->
                    TrophyFrogManager.getInternalName(data.key).getItemStack().getItemRarityOrNull()
                }
            }

            TrophySorting.HIGHEST_RARITY -> {
                trophyFrogs.entries.sortedBy { data ->
                    TrophyRarity.entries.filter {
                        data.value.contains(it)
                    }.maxByOrNull { it.ordinal }
                }
            }

            TrophySorting.NAME -> {
                trophyFrogs.entries.sortedBy { data ->
                    TrophyFrogManager.getDisplayName(data.key).removeColor()
                }
            }
        }

    private fun count(
        trophyFrogs: Map<String, MutableMap<TrophyRarity, Int>>, rarity: TrophyRarity,
    ) = trophyFrogs.entries.sortedBy { it.value[rarity] ?: 0 }

    @HandleEvent
    private fun onGuiRenderTop() {
        if (InventoryUtils.inAnyInventory()) {
            InventoryGuiScaleCompat.withOriginalHudScale {
                renderDisplay()
            }
        } else {
            renderDisplay()
        }
    }

    private fun renderDisplay() {
        if (!isEnabled() || !canRender()) return
        if (EstimatedItemValue.isCurrentlyShowing()) return
        if (config.requireArmor.get() && !FishingApi.isWearingAnyTrophyArmor()) return

        config.position.renderRenderables(
            display,
            extraSpace = config.extraSpace.get(),
            posLabel = "Trophy Frog Display",
        )
    }

    private fun canRender(): Boolean = when (config.whenToShow.get()!!) {
        WhenToShow.ALWAYS -> true
        WhenToShow.ONLY_IN_INVENTORY -> MinecraftCompat.screen is InventoryScreen
        WhenToShow.ONLY_WITH_KEYBIND -> config.keybind.isKeyHeld()
    }

    private fun isEnabled() = IslandType.LOTUS_ATOLL.isInIsland() && config.enabled.get()
}
