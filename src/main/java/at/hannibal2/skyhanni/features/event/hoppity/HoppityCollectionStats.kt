package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
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
    private val duplicatesFoundPattern by patternGroup.pattern(
        "duplicates.found",
        "§7Duplicates Found: §a(?<duplicates>[\\d,]+)"
    )

    /**
     * REGEX-TEST: §7§8You cannot find this rabbit until you
     * REGEX-TEST: §7§8You have not found this rabbit yet!
     */
    private val rabbitNotFoundPattern by patternGroup.pattern(
        "rabbit.notfound",
        "(?:§.)+You (?:have not found this rabbit yet!|cannot find this rabbit until you)"
    )

    private val rabbitsFoundPattern by patternGroup.pattern(
        "rabbits.found",
        "§.§l§m[ §a-z]+§r §.(?<current>[0-9]+)§./§.(?<total>[0-9]+)"
    )

    private var display = emptyList<Renderable>()
    private val loggedRabbits
        get() = ProfileStorageData.profileSpecific?.chocolateFactory?.rabbitCounts ?: mutableMapOf()

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

        val newList = mutableListOf<Renderable>()
        newList.add(Renderable.string("§eHoppity Rabbit Collection§f:"))
        newList.add(LorenzUtils.fillTable(getRabbitStats(), padding = 5))

        val loggedRabbitCount = loggedRabbits.size
        val foundRabbitCount = getFoundRabbitsFromHypixel(event)

        if (loggedRabbitCount < foundRabbitCount) {
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

        val table = mutableListOf<DisplayTableEntry>()
        for (rarity in RabbitCollectionRarity.entries) {
            val isTotal = rarity == RabbitCollectionRarity.TOTAL

            val foundOfRarity = loggedRabbits.filterKeys {
                HoppityCollectionData.getRarity(it) == rarity
            }

            val title = "${rarity.displayName} Rabbits"
            val amountFound = foundOfRarity.size
            val duplicates = foundOfRarity.values.sum() - amountFound

            val chocolateBonuses = foundOfRarity.keys.map {
                HoppityCollectionData.getChocolateBonuses(it)
            }

            val chocolatePerSecond = chocolateBonuses.sumOf { it.chocolate }
            val chocolateMultiplier = chocolateBonuses.sumOf { it.multiplier }

            if (!isTotal) {
                totalAmountFound += amountFound
                totalDuplicates += duplicates
                totalChocolatePerSecond += chocolatePerSecond
                totalChocolateMultiplier += chocolateMultiplier
            }

            val displayFound = if (isTotal) totalAmountFound else amountFound
            val displayTotal = if (isTotal) {
                HoppityCollectionData.knownRabbitCount
            } else {
                HoppityCollectionData.knownRabbitsOfRarity(rarity)
            }
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
                add("§7Chocolate Per Second: §a${displayChocolatePerSecond.addSeparators()}")
                add("§7Chocolate Multiplier: §a${displayChocolateMultiplier.round(3)}")
            }
            table.add(
                DisplayTableEntry(
                    title,
                    "§a$displayFound§7/§a$displayTotal",
                    displayTotal.toDouble(),
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

    // Gets the found rabbits according to the Hypixel progress bar
    // used to make sure that mod data is synchronized with Hypixel
    private fun getFoundRabbitsFromHypixel(event: InventoryFullyOpenedEvent): Int {
        return event.inventoryItems.firstNotNullOf {
            it.value.getLore().matchFirst(rabbitsFoundPattern) {
                group("current").formatInt()
            }
        }
    }

    private fun logRabbits(event: InventoryFullyOpenedEvent) {
        for ((_, item) in event.inventoryItems) {
            val itemName = item.displayName?.removeColor() ?: continue
            val isRabbit = HoppityCollectionData.isKnownRabbit(itemName)

            if (!isRabbit) continue

            val itemLore = item.getLore()
            val found = !rabbitNotFoundPattern.anyMatches(itemLore)

            if (!found) continue

            val duplicates = itemLore.matchFirst(duplicatesFoundPattern) {
                group("duplicates").formatInt()
            } ?: 0

            loggedRabbits[itemName] = duplicates + 1
        }
    }


    // bugfix for some weird potential user errors (e.g. if users play on alpha and get rabbits)
    fun clearSavedRabbits() {
        loggedRabbits.clear()
        ChatUtils.chat("Cleared saved rabbit data.")
    }


    // checks special rabbits whenever loggedRabbits is modified to update misc stored values
    // TODO: make this better than hard-coded checks
    private fun checkSpecialRabbits() {
        if (hasFoundRabbit("Einstein")) {
            ChocolateFactoryAPI.profileStorage?.timeTowerCooldown = 7
        }

        if (hasFoundRabbit("Mu")) {
            ChocolateFactoryAPI.profileStorage?.hasMuRabbit = true
        }
    }

    private fun hasFoundRabbit(rabbit: String): Boolean = loggedRabbits.containsKey(rabbit)

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.hoppityCollectionStats

    enum class RabbitCollectionRarity(
        val displayName: String,
        val item: NEUInternalName,
    ) {
        COMMON("§fCommon", "STAINED_GLASS".asInternalName()),
        UNCOMMON("§aUncommon", "STAINED_GLASS-5".asInternalName()),
        RARE("§9Rare", "STAINED_GLASS-11".asInternalName()),
        EPIC("§5Epic", "STAINED_GLASS-10".asInternalName()),
        LEGENDARY("§6Legendary", "STAINED_GLASS-1".asInternalName()),
        MYTHIC("§dMythic", "STAINED_GLASS-6".asInternalName()),
        DIVINE("§bDivine", "STAINED_GLASS-3".asInternalName()),
        TOTAL("§cTotal", "STAINED_GLASS-14".asInternalName()),
        ;

        companion object {
            fun fromDisplayName(displayName: String) = entries.firstOrNull { it.name == displayName }
        }
    }
}
