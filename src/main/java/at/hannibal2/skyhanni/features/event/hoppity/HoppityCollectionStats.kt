package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuHoppityJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.anyMatches
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object HoppityCollectionStats {

    private val config get() = ChocolateFactoryAPI.config

    private val patternGroup = ChocolateFactoryAPI.patternGroup.group("collection")
    private val pagePattern by patternGroup.pattern(
        "page.current",
        "\\((?<page>\\d+)/(?<maxPage>\\d+)\\) Hoppity's Collection"
    )
    private val rabbitRarityPattern by patternGroup.pattern(
        "rabbit.rarity",
        "§.§L(?<rarity>\\w+) RABBIT"
    )
    private val duplicatesFoundPattern by patternGroup.pattern(
        "duplicates.found",
        "§7Duplicates Found: §a(?<duplicates>[\\d,]+)"
    )
    private val rabbitsFoundPattern by patternGroup.pattern(
        "rabbits.found",
        "§.§l§m[ §a-z]+§r §.(?<current>[0-9]+)§./§.(?<total>[0-9]+)"
    )

    private var display = emptyList<Renderable>()
    private val loggedRabbits
        get() = ProfileStorageData.profileSpecific?.chocolateFactory?.rabbitCounts ?: mutableMapOf()

    private var totalRabbits = 0
    private val rabbitRarities = mutableMapOf<String, RabbitCollectionRarity>()

    var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!pagePattern.matches(event.inventoryName)) return

        inInventory = true
        display = buildDisplay(event)
        checkSpecialRabbits()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        display = emptyList()
    }

    @SubscribeEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        rabbitRarities.clear()

        val data = event.readConstant<NeuHoppityJson>("hoppity").hoppity
        for ((rarityString, rarityData) in data.rarities.entries) {
            val rarity = RabbitCollectionRarity.valueOf(rarityString.uppercase())

            for (rabbit in rarityData.rabbits) {
                rabbitRarities[rabbit] = rarity
            }
        }
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!inInventory) return

        config.hoppityStatsPosition.renderRenderables(
            display,
            extraSpace = 5,
            posLabel = "Hoppity's Collection Stats"
        )
    }

    private fun buildDisplay(event: InventoryFullyOpenedEvent): MutableList<Renderable> {
        logRabbits(event)
        val totalAmount = getTotalRabbits(event)

        val newList = mutableListOf<Renderable>()
        newList.add(Renderable.string("§eHoppity Rabbit Collection§f:"))
        newList.add(LorenzUtils.fillTable(getRabbitStats(), padding = 5))

        if (totalAmount != totalRabbits) {
            newList.add(Renderable.string(""))
            newList.add(
                Renderable.wrappedString(
                    "§cPlease Scroll through \n" +
                        "§call pages!",
                    width = 200,
                )
            )
        }
        return newList
    }

    private fun getRabbitStats(): MutableList<DisplayTableEntry> {
        var totalAmountFound = 0
        var totalDuplicates = 0
        var totalChocolatePerSecond = 0
        var totalChocolateMultiplier = 0.0
        totalRabbits = 0

        val table = mutableListOf<DisplayTableEntry>()
        for (rarity in RabbitCollectionRarity.entries) {
            val filtered = loggedRabbits.filterKeys {
                val apiName = toApiRabbitName(it)
                rabbitRarities[apiName] == rarity
            }

            val isTotal = rarity == RabbitCollectionRarity.TOTAL

            val title = "${rarity.displayName} Rabbits"
            val amountFound = filtered.count { it.value > 0 }
            val totalOfRarity = filtered.size
            val duplicates = filtered.values.sumOf { (it - 1).coerceAtLeast(0) }
            val chocolatePerSecond = rarity.chocolatePerSecond * amountFound
            val chocolateMultiplier = (rarity.chocolateMultiplier * amountFound)

            if (!isTotal) {
                totalAmountFound += amountFound
                totalRabbits += totalOfRarity
                totalDuplicates += duplicates
                totalChocolatePerSecond += chocolatePerSecond
                totalChocolateMultiplier += chocolateMultiplier
            }

            val displayFound = if (isTotal) totalAmountFound else amountFound
            val displayTotal = if (isTotal) totalRabbits else totalOfRarity
            val displayDuplicates = if (isTotal) totalDuplicates else duplicates
            val displayChocolatePerSecond = if (isTotal) totalChocolatePerSecond else chocolatePerSecond
            val displayChocolateMultiplier = if (isTotal) totalChocolateMultiplier else chocolateMultiplier

            val hover = buildList {
                add(title)
                add("")
                add("§7Unique Rabbits: §a$displayFound§7/§a$displayTotal")
                add("§7Duplicate Rabbits: §a$displayDuplicates")
                add("§7Total Rabbits Found: §a${displayFound + displayDuplicates}")
                add("")
                add("§7Chocolate Per Second: §a$displayChocolatePerSecond")
                add("§7Chocolate Multiplier: §a${displayChocolateMultiplier.round(3)}")
            }
            table.add(
                DisplayTableEntry(
                    title,
                    "§a$displayFound§7/§a$displayTotal",
                    displayFound.toDouble(),
                    rarity.item,
                    hover
                )
            )
        }
        return table
    }

    fun incrementRabbit(name: String) {
        loggedRabbits[name] = (loggedRabbits[name] ?: 0) + 1
        checkSpecialRabbits()
    }

    // Used because NEU stores rabbit using their API names rather than in-game names
    private fun toApiRabbitName(name: String): String {
        return name.lowercase().replace("[- ]".toRegex(), "_")
    }

    private fun getTotalRabbits(event: InventoryFullyOpenedEvent): Int {
        return event.inventoryItems.firstNotNullOf {
            it.value.getLore().matchFirst(rabbitsFoundPattern) {
                group("total").formatInt()
            }
        }
    }

    private fun logRabbits(event: InventoryFullyOpenedEvent) {
        for ((_, item) in event.inventoryItems) {
            val itemName = item.displayName?.removeColor() ?: continue
            val itemLore = item.getLore()
            var isRabbit = rabbitRarityPattern.anyMatches(itemLore)
            if (!isRabbit) continue

            var count = itemLore.matchFirst(duplicatesFoundPattern) {
                group("duplicates").formatInt() + 1
            } ?: 0

            loggedRabbits[itemName] = count
        }
    }

    // checks special rabbits whenever loggedRabbits is modified to update misc stored values
    // TODO: make this better than hard-coded checks
    private fun checkSpecialRabbits() {
        if ((loggedRabbits["einstein"] ?: 0) > 0) {
            ChocolateFactoryAPI.profileStorage?.timeTowerCooldown = 7
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.hoppityCollectionStats

    // todo in future make the amount and multiplier work with mythic rabbits (can't until I have some)
    enum class RabbitCollectionRarity(
        val displayName: String,
        val chocolatePerSecond: Int,
        val chocolateMultiplier: Double,
        val item: NEUInternalName,
    ) {
        COMMON("§fCommon", 1, 0.002, "STAINED_GLASS".asInternalName()),
        UNCOMMON("§aUncommon", 2, 0.003, "STAINED_GLASS-5".asInternalName()),
        RARE("§9Rare", 4, 0.004, "STAINED_GLASS-11".asInternalName()),
        EPIC("§5Epic", 10, 0.005, "STAINED_GLASS-10".asInternalName()),
        LEGENDARY("§6Legendary", 0, 0.02, "STAINED_GLASS-1".asInternalName()),
        MYTHIC("§dMythic", 0, 0.0, "STAINED_GLASS-6".asInternalName()),
        TOTAL("§cTotal", 0, 0.0, "STAINED_GLASS-14".asInternalName()),
        ;

        companion object {
            fun fromDisplayName(displayName: String) = entries.firstOrNull { it.name == displayName }
        }
    }
}
