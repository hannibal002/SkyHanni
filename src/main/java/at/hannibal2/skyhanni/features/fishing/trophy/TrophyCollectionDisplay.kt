package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.HideCaught
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.TextPart
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.TrophySorting
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.WhenToShow
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName
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

/**
 * Generic on-screen collection display shared by Trophy Fish and Trophy Frogs. Subclasses are the
 * [at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule] objects that hold the event handlers and
 * provide the collection-specific details below.
 */
@Suppress("TooManyFunctions")
abstract class TrophyCollectionDisplay {

    protected abstract val config: TrophyCollectionDisplayConfig

    /** The saved counts, keyed by the same raw name used by [getInternalName]/[getDisplayName]. */
    protected abstract val data: Map<String, MutableMap<TrophyRarity, Int>>?

    protected abstract val header: String
    protected abstract val posLabel: String
    protected abstract val collectionName: String
    protected abstract val dataSourceName: String

    protected abstract fun getInternalName(rawName: String): NeuInternalName
    protected abstract fun getDisplayName(rawName: String): String
    protected abstract fun hoverInfo(rawName: String): String?
    protected abstract fun isOnIsland(): Boolean
    protected abstract fun holdingRod(): Boolean

    /** Whether the required trophy gear is worn. Only checked when [config].requireArmor is on. */
    protected abstract fun passesGearCheck(): Boolean

    /** Extra always-on render guard (e.g. hide while a treasure hook is active). */
    protected open fun canRenderExtra(): Boolean = true

    private val recentlyDropped = TimeLimitedCache<String, TrophyRarity>(5.seconds)
    private var display = emptyList<Renderable>()

    private fun markRecentlyDropped(rawName: String, rarity: TrophyRarity) {
        recentlyDropped[rawName] = rarity
    }

    /** Call from the island-join handler: rebuilds shortly after joining, once data has loaded. */
    protected fun delayedIslandJoinUpdate() {
        DelayedRun.runDelayed(200.milliseconds) {
            update()
        }
    }

    /** Call from the trophy-caught handler: highlights the catch and rebuilds now and after the highlight expires. */
    protected fun onCaught(rawName: String, rarity: TrophyRarity) {
        markRecentlyDropped(rawName, rarity)
        update()
        DelayedRun.runDelayed(5.1.seconds) {
            update()
        }
    }

    /** Call from the profile-join handler: drops the stale display before rebuilding for the new profile. */
    protected fun resetAndUpdate() {
        display = emptyList()
        update()
    }

    /** Hook run before every rebuild (e.g. to backfill missing entries). */
    protected open fun beforeUpdate() {}

    fun update() {
        beforeUpdate()
        if (!isEnabled()) return
        val list = mutableListOf<Renderable>()
        list.addString(header)
        list.add(Renderable.table(createTable(), ySpacing = config.extraSpace.get()))
        display = list
    }

    private fun createTable(): List<List<Renderable>> {
        val entries = data ?: return emptyList()
        val table = mutableListOf<List<Renderable>>()

        if (entries.isEmpty()) {
            table.addSingleString("§cNo Trophy data found!")
            table.addSingleString("§eTalk to $dataSourceName to load the data!")
            return table
        }

        for ((rawName, counts) in getOrder(entries)) {
            addRow(rawName, counts, table)
        }
        if (table.isNotEmpty()) return table

        get(config.onlyShowMissing.get())?.let { rarity ->
            val name = rarity.formattedString
            table.addSingleString("§eYou caught all $name $collectionName")
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

        val hover = hoverInfo(rawName)
        fun string(string: String): Renderable = hover?.let {
            Renderable.hoverTips(
                Renderable.text(string),
                tips = it.split("\n"),
            )
        } ?: Renderable.text(string)

        val row = mutableMapOf<TextPart, Renderable>()
        row[TextPart.NAME] = string(getDisplayName(rawName))
        row[TextPart.ICON] = Renderable.item(getInternalName(rawName))

        val recentlyDroppedRarity = recentlyDropped[rawName]?.takeIf { config.highlightNew.get() }

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

    private fun getOrder(entries: Map<String, MutableMap<TrophyRarity, Int>>) = sort(entries).let {
        if (config.reverseOrder.get()) it.reversed() else it
    }

    private fun sort(entries: Map<String, MutableMap<TrophyRarity, Int>>): List<Map.Entry<String, MutableMap<TrophyRarity, Int>>> =
        when (config.sortingType.get()!!) {
            TrophySorting.TOTAL_AMOUNT -> entries.entries.sortedBy { it.value.sumAllValues() }

            TrophySorting.BRONZE_AMOUNT -> count(entries, TrophyRarity.BRONZE)
            TrophySorting.SILVER_AMOUNT -> count(entries, TrophyRarity.SILVER)
            TrophySorting.GOLD_AMOUNT -> count(entries, TrophyRarity.GOLD)
            TrophySorting.DIAMOND_AMOUNT -> count(entries, TrophyRarity.DIAMOND)

            TrophySorting.ITEM_RARITY -> {
                entries.entries.sortedBy { data ->
                    getInternalName(data.key).getItemStack().getItemRarityOrNull()
                }
            }

            TrophySorting.HIGHEST_RARITY -> {
                entries.entries.sortedBy { data ->
                    TrophyRarity.entries.filter {
                        data.value.contains(it)
                    }.maxByOrNull { it.ordinal }
                }
            }

            TrophySorting.NAME -> {
                entries.entries.sortedBy { data ->
                    getDisplayName(data.key).removeColor()
                }
            }
        }

    private fun count(
        entries: Map<String, MutableMap<TrophyRarity, Int>>, rarity: TrophyRarity,
    ) = entries.entries.sortedBy { it.value[rarity] ?: 0 }

    protected fun render() {
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
        if (!canRenderExtra()) return
        if (config.requireArmor.get() && !passesGearCheck()) return

        config.position.renderRenderables(
            display,
            extraSpace = config.extraSpace.get(),
            posLabel = posLabel,
        )
    }

    private fun canRender(): Boolean = when (config.whenToShow.get()!!) {
        WhenToShow.ALWAYS -> true
        WhenToShow.ONLY_IN_INVENTORY -> MinecraftCompat.screen is InventoryScreen
        WhenToShow.ONLY_WITH_ROD_IN_HAND -> holdingRod()
        WhenToShow.ONLY_WITH_KEYBIND -> config.keybind.isKeyHeld()
    }

    /** Registers config listeners; call from the subclass's onConfigLoad handler. */
    protected fun watchConfig() {
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

    private fun isEnabled() = isOnIsland() && config.enabled.get()
}
