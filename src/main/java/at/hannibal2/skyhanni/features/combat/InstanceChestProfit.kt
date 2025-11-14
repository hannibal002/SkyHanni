package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getRawCraftCostOrNull
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.MISSING_ITEM
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.toSingletonListOrEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.emptyText
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@SkyHanniModule
object InstanceChestProfit {
    private val patternGroup = RepoPattern.group("combat.instance-chest-profit")

    /**
     * REGEX-TEST: §6Kraken Shard §8x1
     * REGEX-TEST: §6Apex Dragon Shard §8x1
     */
    private val attributeShardPattern by patternGroup.pattern(
        "attributeshard",
        "§.(?<name>.+ Shard) §.x(?<count>\\d+)",
    )

    /**
     * REGEX-TEST: §dCrimson Essence §8x250
     * REGEX-TEST: §dUndead Essence §8x10
     * REGEX-TEST: §dWither Essence §8x8
     */
    private val essencePattern by patternGroup.pattern(
        "essence",
        "§.(?<name>\\w+ Essence) §.x(?<count>\\d+)",
    )

    /**
     * REGEX-TEST: §6500,000 Coins
     * REGEX-TEST: §6100,000 Coins
     * REGEX-TEST: §6250,00 Coins
     */
    private val coinsPattern by patternGroup.pattern(
        "coins",
        "§6(?<amount>.*) Coins",
    )

    /**
     * REGEX-TEST: §9Dungeon Chest Key
     */
    private val dungeonChestKey by patternGroup.pattern(
        "dungeonchestkey",
        "§9Dungeon Chest Key",
    )

    /**
     * REGEX-TEST: §6Infernal Kuudra Key
     * REGEX-TEST: §5Burning Kuudra Key
     * REGEX-TEST: §9Kuudra Key
     */
    private val kuudraChestKey by patternGroup.pattern(
        "kuudrachestkey",
        "§.(?:\\w+ )?Kuudra Key",
    )

    /**
     * REGEX-TEST: §aReroll Shard
     */
    private val fakeItemNamePattern by patternGroup.pattern(
        "fakeitemname",
        "§aReroll Shard",
    )

    /**
     * REGEX-TEST: Master Catacombs - Floor II
     * REGEX-TEST: Catacombs - Floor V
     * REGEX-TEST: Kuudra - Infernal
     */
    private val runNameCroesus by patternGroup.pattern(
        "runname",
        ".*Catacombs - Flo.*|Kuudra - .*",
    )

    /**
     * REGEX-TEST: §6Paid Chest
     * REGEX-TEST: §6Paid
     * REGEX-TEST: §fFree Chest
     * REGEX-TEST: §fFree
     */
    /* Dungeon chests are just the chest Type for example just 'Emerald', Kuudra CURRENTLY has them as Free Chest/Paid Chest in the same UI
    if the Croesus main UI shows just Paid/Free this regex pattern should be removable mainly
    if the Croesus UI starts showing like "Emerald Chest" as the Chest Name the Regex should include all the cata chest names too then. */
    private val chestFutureProofing by patternGroup.pattern(
        "kuudrachest",
        "§.(?<chestname>Free|Paid)(?: Chest)?",
    )

    /**
     * REGEX-TEST: §61,000,000 Coins
     * REGEX-TEST: §aFREE
     * REGEX-TEST: §6250,000 Coins
     */
    private val chestCostCroesus by patternGroup.pattern(
        "croesuscost",
        "§6(?<amount>.*) Coins|§aFREE",
    )

    /**
     * REGEX-TEST: §aAlready opened!'
     */
    private val alreadyOpened by patternGroup.pattern(
        "alreadyopened",
        "§aAlready opened!",
    )

    /**
     * REGEX-TEST: §d§lUltimate Wise I§f
     * REGEX-TEST: §d§lCombo I§f
     */
    private val bookColorFixer by patternGroup.pattern(
        "bookcolorfix",
        "(?<item>.+)(?:§.)+",
    )

    private val config get() = SkyHanniMod.feature.combat.instanceChestProfit

    // TODO replace those three "in chest" booleans with inventory detectors
    private var inDungeonChest = false
    private var inKuudraChest = false
    private var inCroesusRunMenu = false
    private var chestDisplay: Renderable? = null
    private var croesusDisplay: Renderable? = null
    private val croesusDisplayList = mutableListOf<List<Renderable>>()
    private var slotToHighlight: Pair<Int, Double>? = null

    enum class CroesusChestType(val stackChestName: String) {
        WOOD("§fWood"),
        GOLD("§6Gold"),
        DIAMOND("§bDiamond"),
        EMERALD("§2Emerald"),
        OBSIDIAN("§5Obsidian"),
        BEDROCK("§8Bedrock"),
        FREE("§fFree"),
        PAID("§6Paid"),
        ;

        companion object {
            fun getByStackName(stackName: String): CroesusChestType? {
                var newStackName = stackName
                chestFutureProofing.matchMatcher(stackName) {
                    newStackName = group("chestname")
                }
                return entries.firstOrNull { it.stackChestName == newStackName }
            }
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.enabled && !config.croesusEnabled && !config.croesusHighlight) return

        val name = event.inventoryName
        when {
            DungeonApi.DungeonChest.getByInventoryName(name) != null && config.enabled -> {
                inDungeonChest = true
            }

            KuudraApi.KuudraChest.getByInventoryName(name) != null && config.enabled -> {
                inKuudraChest = true
            }

            runNameCroesus.matches(name) && (config.croesusHighlight || config.croesusEnabled) -> inCroesusRunMenu = true

            else -> return
        }

        if (inCroesusRunMenu) {
            event.inventoryItems.forEach { (slot, item) ->
                val chestType = CroesusChestType.getByStackName(item.displayName)
                if (chestType != null) parseCroesusChest(item, chestType, slot)
            }
            createCroesusDisplay()
        }

        createDisplay(event.inventoryItems)
    }

    @HandleEvent(priority = HandleEvent.LOWEST, onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        val slot = slotToHighlight?.first
        if (inCroesusRunMenu && slot != null && config.croesusHighlight) {
            event.container.inventorySlots[slot].highlight(LorenzColor.GREEN)
        }
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        inDungeonChest = false
        inKuudraChest = false
        inCroesusRunMenu = false
        croesusDisplayList.clear()
        slotToHighlight = null
        croesusDisplay = null
    }

    private fun parseCroesusChest(itemStack: ItemStack?, chestType: CroesusChestType, slot: Int) {
        val chestList = mutableListOf<NeuInternalName>()
        val chestTipsRenderables = mutableListOf<String>()
        chestTipsRenderables.add("${chestType.stackChestName}:")
        var totalPrice = 0.0
        var cost = 0.0
        itemStack?.getLore()?.forEach { loreLine ->
            if (alreadyOpened.matches(loreLine)) return
            var itemPrice: Double
            var itemName = ItemUtils.readBookType(loreLine) ?: loreLine
            var itemInternalName = NeuInternalName.fromItemName(itemName)
            bookColorFixer.matchMatcher(itemName) {
                itemName = ItemResolutionQuery.resolveEnchantmentByName(group("item")) ?: itemName
                itemInternalName = itemName.toInternalName()
            }
            if (itemInternalName != MISSING_ITEM) {
                itemPrice = getPrice(itemInternalName)
                essencePattern.matchMatcher(loreLine) {
                    itemPrice = getEssence(group("name"), group("count").toInt())
                }
                if (dungeonChestKey.matches(loreLine)) {
                    cost += getPrice(itemInternalName).times(-1)
                    itemPrice = -1.0
                }
                if (itemPrice != -1.0) {
                    chestTipsRenderables.add(" ${itemInternalName.repoItemName}: ${itemPrice.formatCoin()} ")
                    totalPrice += itemPrice
                    chestList.add(itemInternalName)
                }
                kuudraChestKey.matchMatcher(loreLine) {
                    cost += itemInternalName.getRawCraftCostOrNull(config.priceSource)?.times(-1) ?: 0.0
                }
            }
            chestCostCroesus.matchMatcher(loreLine) {
                cost += groupOrNull("amount")?.formatInt()?.toDouble()?.times(-1) ?: 0.0
            }
        }
        val preCostPrice = totalPrice
        totalPrice += cost
        if (slotToHighlight == null) slotToHighlight = Pair(slot, totalPrice)
        else {
            val nonNullSlot = slotToHighlight
            if (nonNullSlot != null) {
                if (nonNullSlot.second < totalPrice)
                    slotToHighlight = Pair(slot, totalPrice)
            }
        }
        chestTipsRenderables.add("Cost: ${cost.formatCoin()}")
        chestTipsRenderables.add("Profit: ${totalPrice.formatCoin()} §f(Pre Cost Profit ${preCostPrice.formatCoin()}§f)")
        croesusDisplayList.add(createCroesusSingleChestDisplay(chestType, totalPrice, createRenderableList(chestTipsRenderables)))
    }

    private fun getPrice(internalName: NeuInternalName): Double {
        return internalName.getPrice(config.priceSource)
    }

    private fun createRenderableList(mutableList: MutableList<String>): MutableList<Renderable> {
        val renderList = mutableListOf<Renderable>()
        mutableList.forEach {
            renderList.add(Renderable.text(it))
        }
        return renderList
    }

    private fun createCroesusSingleChestDisplay(
        chestType: CroesusChestType,
        totalValue: Double,
        contents: MutableList<Renderable>,
    ): List<Renderable> = Renderable.hoverTips(
        Renderable.text("${chestType.stackChestName}: ${totalValue.formatCoin()}"),
        contents,
    ).toSingletonListOrEmpty()

    private fun createCroesusDisplay() {
        val newDisplay = buildList {
            add(listOf(Renderable.text("§6§lCroesus Profit Overlay")))
            croesusDisplayList.forEach {
                add(it)
            }
        }
        croesusDisplay = Renderable.table(newDisplay, ySpacing = 1)
    }

    private fun getEssence(name: String, rawCount: Int): Double {
        val count = if (name == "Crimson") rawCount * (1 + getKuudraEssenceBonus())
        else rawCount.toDouble()
        return count * getPrice(NeuInternalName.fromItemName(name))
    }

    private fun getAttribute(attributeName: String): Double = attributeShardPattern.matchMatcher(attributeName) {
        val name = group("name")
        val count = group("count").toInt()
        count * getPrice(NeuInternalName.fromItemName(name))
    } ?: 0.0

    private fun createDisplay(items: Map<Int, ItemStack>) {
        val itemsWithCost: MutableMap<String, Double> = mutableMapOf()
        items.forEach {
            if (fakeItemNamePattern.matches(it.value.displayName)) return@forEach
            if (it.value.getInternalNameOrNull() != null) {
                val cost = EstimatedItemValueCalculator.getTotalPrice(it.value)
                if (cost != null) itemsWithCost.addOrPut(it.value.getInternalName().repoItemName, cost)
            }
            val name = it.value.displayName
            if (attributeShardPattern.matches(name)) {
                val price = getAttribute(name)
                itemsWithCost.addOrPut(name, price)
            }
            essencePattern.matchMatcher(name) {
                val price = getEssence(group("name"), group("count").toInt())
                // TODO remove if check, getEssence should return null if no price is found
                if (price != 0.0) itemsWithCost.addOrPut(name, price)
            }
        }

        // Slot 31 has the cost information for the chest
        items[31]?.getLore()?.forEach {
            coinsPattern.matchMatcher(it) {
                val amount = group("amount").formatInt()
                itemsWithCost.put(it, -amount.toDouble())
            }
            dungeonChestKey.matchMatcher(it) {
                val name = NeuInternalName.fromItemName(it)
                itemsWithCost.put(it, getPrice(name).times(-1))
            }
            kuudraChestKey.matchMatcher(it) {
                val name = NeuInternalName.fromItemName(it)
                itemsWithCost.put(it, name.getRawCraftCostOrNull(config.priceSource)?.times(-1) ?: 0.0)
            }
        }

        val newDisplay = buildList {
            val chestName = if (inDungeonChest) "Dungeon"
            else if (inKuudraChest) "Kuudra"
            else ""
            add(listOf(Renderable.text("§d§l$chestName Chest Profit")))
            add(listOf(Renderable.emptyText()))

            var total = 0.0
            var displayedCost = false

            val revenue = itemsWithCost.values.filter { it > 0 }.sum()
            add(listOf(Renderable.text("§a§lTotal Revenue"), Renderable.text("§a${revenue.formatCoin()}")))

            itemsWithCost.forEach {
                val coinsColor = if (it.value < 0) "§c"
                else "§a"

                if (!displayedCost && it.value < 0) {
                    val cost = itemsWithCost.values.filter { cost -> cost < 0 }.sum()
                    add(listOf(Renderable.emptyText()))
                    add(listOf(Renderable.text("§c§lTotal Cost"), Renderable.text("§c${cost.formatCoin()}")))
                    displayedCost = true
                }

                val coins = "$coinsColor${it.value.formatCoin()}"

                total += it.value
                add(listOf(Renderable.text(it.key), Renderable.text(coins)))
            }

            val color = if (total < 0) "§c"
            else "§a"

            add(listOf(Renderable.emptyText()))
            add(listOf(Renderable.text("$color§lProfit"), Renderable.text("$color ${total.formatCoin()}")))
        }

        chestDisplay = Renderable.table(newDisplay, ySpacing = 1)
    }

    private fun getKuudraEssenceBonus(): Double =
        ProfileStorageData.petProfiles?.pets?.filter { PetUtils.getPetProperName(it.fauxInternalName) == "KUUDRA" }
            ?.maxByOrNull { it.rarity.id }
            ?.let {
                when (it.rarity) {
                    LorenzRarity.RARE -> 0.15 / 100 * it.level
                    LorenzRarity.EPIC, LorenzRarity.LEGENDARY -> 0.2 / 100 * it.level
                    else -> 0.0
                }
            } ?: 0.0

    @HandleEvent(GuiRenderEvent::class)
    fun onRenderOverlay() {
        if (config.enabled && (inDungeonChest || inKuudraChest)) {
            config.position.renderRenderable(
                chestDisplay,
                posLabel = "Instance Chest Profit",
            )
        }
        if (config.croesusEnabled && inCroesusRunMenu) {
            config.croesusPosition.renderRenderable(
                croesusDisplay,
                posLabel = "Croesus Chest Profit",
            )
        }
    }
}
