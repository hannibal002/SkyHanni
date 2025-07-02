package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import java.util.regex.Pattern

@SkyHanniModule
object DicerRngDropTracker {

    private val itemDrops = mutableListOf<ItemDrop>()
    private val config get() = GardenApi.config.dicerRngDropTracker
    private val tracker = SkyHanniTracker("Dicer RNG Drop Tracker", { Data() }, { it.garden.dicerDropTracker }) {
        drawDisplay(it)
    }

    class Data : TrackerData() {

        override fun reset() {
            drops.clear()
        }

        @Expose
        var drops: MutableMap<CropType, MutableMap<DropRarity, Int>> = mutableMapOf()
    }

    private val melonPatternGroup = RepoPattern.group("garden.dicer.melon")
    private val melonUncommonDropPattern by melonPatternGroup.pattern(
        "uncommon",
        "§a§lUNCOMMON DROP! §r§eDicer dropped §r§a\\d+x §r§aEnchanted Melon§r§e!",
    )
    private val melonRareDropPattern by melonPatternGroup.pattern(
        "rare",
        "§9§lRARE DROP! §r§eDicer dropped §r§a\\d+x §r§aEnchanted Melon§r§e!",
    )
    private val melonCrazyRareDropPattern by melonPatternGroup.pattern(
        "crazyrare",
        "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§[a|9]\\d+x §r§[a|9]Enchanted Melon(?: Block)?§r§e!",
    )
    private val melonRngesusDropPattern by melonPatternGroup.pattern(
        "rngesus",
        "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§9\\d+x §r§9Enchanted Melon Block§r§e!",
    )

    private val pumpkinPatternGroup = RepoPattern.group("garden.dicer.pumpkin")
    private val pumpkinUncommonDropPattern by pumpkinPatternGroup.pattern(
        "uncommon",
        "§a§lUNCOMMON DROP! §r§eDicer dropped §r§a\\d+x §r§aEnchanted Pumpkin§r§e!",
    )
    private val pumpkinRareDropPattern by pumpkinPatternGroup.pattern(
        "rare",
        "§9§lRARE DROP! §r§eDicer dropped §r§a\\d+x §r§aEnchanted Pumpkin§r§e!",
    )
    private val pumpkinCrazyRareDropPattern by pumpkinPatternGroup.pattern(
        "crazyrare",
        "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§a\\d+x §r§aEnchanted Pumpkin§r§e!",
    )
    private val pumpkinRngesusDropPattern by pumpkinPatternGroup.pattern(
        "rngesus",
        "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§[a|9]\\d+x §r§(?:aEnchanted|9Polished) Pumpkin§r§e!",
    )

    init {
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.UNCOMMON, melonUncommonDropPattern))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.RARE, melonRareDropPattern))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.CRAZY_RARE, melonCrazyRareDropPattern))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.PRAY_TO_RNGESUS, melonRngesusDropPattern))

        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.UNCOMMON, pumpkinUncommonDropPattern))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.RARE, pumpkinRareDropPattern))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.CRAZY_RARE, pumpkinCrazyRareDropPattern))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.PRAY_TO_RNGESUS, pumpkinRngesusDropPattern))
    }

    enum class DropRarity(val colorCode: Char, val displayName: String) {
        UNCOMMON('a', "UNCOMMON"),
        RARE('9', "RARE"),
        CRAZY_RARE('d', "CRAZY RARE"),
        PRAY_TO_RNGESUS('5', "PRAY TO RNGESUS"),
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        val message = event.message
        for (drop in itemDrops) {
            drop.pattern.matchMatcher(message) {
                addDrop(drop.crop, drop.rarity)
                if (config.hideChat) {
                    event.blockedReason = "dicer_drop_tracker"
                }
                return
            }
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.compact) {
            tracker.update()
        }
    }

    private fun drawDisplay(data: Data) = buildList {
        val cropInHand = cropInHand ?: return@buildList

        val topLine = mutableListOf<Renderable>()
        topLine.add(ItemStackRenderable(cropInHand.icon))
        topLine.add(StringRenderable("§7Dicer Tracker:"))
        add(HorizontalContainerRenderable(topLine).toSearchable())

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
