package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.addCollectionCounter
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.SkyhanniTimedTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import java.util.regex.Pattern

@SkyHanniModule
object DicerRngDropTracker {
    private val config get() = GardenApi.config.dicerRngDropTracker
    val tracker = SkyhanniTimedTracker(
        "Dicer RNG Drop Tracker",
        { Data() },
        { it.garden.dicerDropTracker },
        trackerConfig = { config.perTrackerConfig },
        customUptimeControl = true,
        drawDisplay = { drawDisplay(it) }
    )

    class TimeData : TimedTrackerData<Data>({ Data() })

    data class Data(
        @Expose var drops: MutableMap<CropType, MutableMap<DropRarity, Int>> = mutableMapOf()
    ) : TrackerData<SessionUptime.Garden>(SessionUptime.Garden::class)

    private val patternGroup = RepoPattern.group("garden.dicer")

    /**
     * REGEX-TEST: §a§lUNCOMMON DROP! §r§eDicer dropped §r§a4x §r§aEnchanted Melon§r§e!
     * REGEX-TEST: §9§lRARE DROP! §r§eDicer dropped §r§a16x §r§aEnchanted Melon§r§e!
     * REGEX-TEST: §d§lCRAZY RARE DROP! §r§eDicer dropped §r§92x §r§9Enchanted Melon Block§r§e!
     * REGEX-TEST: §5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§98x §r§9Enchanted Melon Block§r§e!
     * REGEX-TEST: §a§lUNCOMMON DROP! §r§eDicer dropped §r§a3x §r§aEnchanted Pumpkin§r§e!
     * REGEX-TEST: §9§lRARE DROP! §r§eDicer dropped §r§a5x §r§aEnchanted Pumpkin§r§e!
     * REGEX-TEST: §d§lCRAZY RARE DROP! §r§eDicer dropped §r§a45x §r§aEnchanted Pumpkin§r§e!
     * REGEX-TEST: §5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§92x §r§9Polished Pumpkin§r§e!
     */
    private val dicerDrop by patternGroup.pattern(
        "drop",
        "§.§l(?<drop>[\\w\\s]+) DROP! §r§eDicer dropped §r§.(?<amount>\\w+)x §r§.(?<item>[\\w\\s]+)§r§e!",
    )

    enum class DropRarity(val colorCode: Char, val displayName: String) {
        UNCOMMON('a', "UNCOMMON"),
        RARE('9', "RARE"),
        CRAZY_RARE('d', "CRAZY RARE"),
        PRAY_TO_RNGESUS('5', "PRAY TO RNGESUS"),
        ;

        companion object {
            fun getByName(name: String) = DropRarity.entries.firstOrNull { it.displayName == name }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        if (cropInHand?.isAnyOf(CropType.MELON, CropType.PUMPKIN) != true) return

        val message = event.message
        dicerDrop.matchMatcher(message) {
            val itemType = group("item")
            val amount = group("amount").toLong()
            val drop = group("drop")

            val internalName = NeuInternalName.fromItemNameOrNull(itemType) ?: return@matchMatcher
            GardenProfitTracker.addItem(GardenTrackerTypes.BREAKING_CROPS, internalName, amount.toInt(), false)
            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            val rawName = primitiveStack.internalName.itemNameWithoutColor
            val cropType = CropType.getByNameOrNull(rawName) ?: return@matchMatcher

            val dropType = DropRarity.getByName(drop) ?: return@matchMatcher

            addDrop(cropType, dropType)
            cropType.addCollectionCounter(CropCollectionType.DICER, primitiveStack.amount * amount)
            if (config.hideChat) {
                event.blockedReason = "dicer_drop_tracker"
            }
            return
        }
    }

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.compact) {
            tracker.update()
        }
    }

    private fun drawDisplay(data: Data) = buildList {
        val cropInHand = cropInHand ?: return@buildList

        add(
            Renderable.horizontal(
                Renderable.item(cropInHand.icon),
                Renderable.text("§7Dicer Tracker:"),
            ).toSearchable(),
        )

        val items = data.drops[cropInHand] ?: return@buildList
        if (config.compact.get()) {
            val compactLine = items.sortedDesc().map { (rarity, amount) ->
                "§${rarity.colorCode}${amount.addSeparators()}"
            }.joinToString("§7/")
            addSearchString(compactLine)

        } else {
            for ((rarity, amount) in items.sortedDesc()) {
                val colorCode = rarity.colorCode
                val displayName = rarity.displayName
                addSearchString(" §7- §e${amount.addSeparators()}x §$colorCode$displayName", displayName)
            }
        }
    }

    private var cropInHand: CropType? = null
    private var toolName = ""

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        val crop = event.crop
        cropInHand = if (crop == CropType.MELON || crop == CropType.PUMPKIN) crop else null
        if (cropInHand != null) {
            toolName = event.toolItem!!.displayName
        }
        tracker.update()
    }

    private fun addDrop(crop: CropType, rarity: DropRarity) {
        tracker.modify {
            val map = it.drops.getOrPut(crop) { mutableMapOf() }
            map.addOrPut(rarity, 1)
        }
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!isEnabled()) return false
        if (cropInHand == null) return false

        return true
    }

    class ItemDrop(val crop: CropType, val rarity: DropRarity, val pattern: Pattern)

    private fun isEnabled() = GardenApi.inGarden() && config.enabled

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.dicerCounterDisplay", "garden.dicerCounters.display")
        event.move(3, "garden.dicerCounterHideChat", "garden.dicerCounters.hideChat")
        event.move(3, "garden.dicerCounterPos", "garden.dicerCounters.pos")

        event.move(7, "#profile.garden.dicerRngDrops", "#profile.garden.dicerDropTracker.drops") { old ->
            val items: MutableMap<CropType, MutableMap<DropRarity, Int>> = mutableMapOf()
            val oldItems = ConfigManager.gson.fromJson<Map<String, Int>>(old, Map::class.java)
            for ((internalName, amount) in oldItems) {
                val split = internalName.split(".")
                val crop = CropType.getByName(split[0])
                val rarityName = split[1]
                val rarity = DropRarity.valueOf(rarityName)
                items.getOrPut(crop) { mutableMapOf() }[rarity] = amount
            }

            ConfigManager.gson.toJsonTree(items)
        }

        event.move(87, "garden.dicerCounters.pos", "garden.dicerCounters.position")
        event.move(87, "garden.dicerCounters.display", "garden.dicerCounters.enabled")
        event.move(88, "garden.dicerCounters", "garden.dicerRngDropTracker")

        event.transform(109, "#profile.garden.dicerDropTracker") { entry ->
            val timedTrackerData: TimedTrackerData<Data> = TimedTrackerData { Data() }
            timedTrackerData.createEntry(SkyHanniTracker.DisplayMode.TOTAL, "total", ConfigManager.gson.fromJson<Data>(entry))
            ConfigManager.gson.toJsonTree(timedTrackerData)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetdicertracker") {
            description = "Resets the Dicer Drop Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }
}
