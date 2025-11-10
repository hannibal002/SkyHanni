package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getRawCraftCostOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
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

    private val runNameCroesus by patternGroup.pattern(
        "runname",
        ".*Catacombs - Flo.*|Kuudra - .*",
    )

    private val kuudraChestFutureProofing by patternGroup.pattern(
        "kuudrachest",
        "§.(?<chestname>Free|Paid)(?: Chest)?",
    )

    private val chestCostCroesus by patternGroup.pattern(
        "croesuscost",
        "§6(?<amount>.*) Coins|§aFREE",
    )

    private val bookPattern by patternGroup.pattern(
        "bookpattern",
        "§fEnchanted Book \\((?<ultimate>§.§.)?(?<bookname>.*)(?:§.)+\\)",
    )

    private val config get() = SkyHanniMod.feature.combat.instanceChestProfit

    private var inDungeonChest = false
    private var inKuudraChest = false
    private var inCroesusRunMenu = false
    private var chestDisplay: Renderable? = null
    private var croesusDisplay: Renderable? = null
    private val croesusDisplayList = mutableListOf<List<Renderable>>()

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
                var newstackname = stackName
                kuudraChestFutureProofing.matchMatcher(stackName) {
                    newstackname = group("chestname")
                }
                return entries.firstOrNull { it.stackChestName == newstackname }
            }
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        ChatUtils.debug(event.inventoryName)
        if (!config.enabled) return

        val name = event.inventoryName
        when {
            DungeonApi.DungeonChest.getByInventoryName(name) != null -> {
                inDungeonChest = true
            }

            KuudraApi.KuudraChest.getByInventoryName(name) != null -> {
                inKuudraChest = true
            }

            runNameCroesus.matches(name) -> inCroesusRunMenu = true

            else -> return
        }
        inCroesusRunMenu = true

        event.inventoryItems.forEach {
            val chestType = CroesusChestType.getByStackName(it.value.displayName)
            if (chestType != null) parseCroesusChest(it.value, chestType)
        }

        createCroesusDisplay()

        createDisplay(event.inventoryItems)
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        inDungeonChest = false
        inKuudraChest = false
        inCroesusRunMenu = false
        croesusDisplayList.clear()
        croesusDisplay = null
    }

    private fun parseCroesusChest(itemStack: ItemStack?, chestType: CroesusChestType) {
        ChatUtils.debug("Reached ParseCroesusChest")
        var chestList = mutableListOf<NeuInternalName>()
        var chestItems = mutableListOf<Renderable>()
        chestItems.add(Renderable.text(chestType.stackChestName))
        var totalPrice = 0.0
        var cost = 0.0
        itemStack?.getLore()?.forEach {
            var itemPrice = 0.0
            var finprice = ""
            var itemInternalName = NeuInternalName.fromItemNameOrNull(it)
            bookPattern.matchMatcher(it) {
                var prefix = ""
                if ((group("ultimate") != null)) prefix = "ULTIMATE_"
                val bookID = prefix + group("bookname")
                itemInternalName = NeuInternalName.fromItemNameOrNull(bookID)
            }
            if (itemInternalName != null) {
                itemPrice = itemInternalName!!.getPrice(config.priceSource)
                essencePattern.matchMatcher(it) {
                    itemPrice = getEssence(it)
                }
                if (dungeonChestKey.matches(it)) {
                    cost += NeuInternalName.fromItemName(it).getPrice(config.priceSource).times(-1)
                    itemPrice = -1.0
                }

                finprice = itemPrice.formatCoin()
                if (itemPrice != -1.0) {
                    ChatUtils.debug("${itemInternalName?.repoItemName ?: ""}/${itemInternalName}: $finprice")
                    chestItems.add(Renderable.text(" ${it}: $finprice "))
                    totalPrice += itemPrice
                    chestList.add(itemInternalName!!)
                }
            }
            chestCostCroesus.matchMatcher(it) {
                cost += groupOrNull("amount")?.formatInt()?.toDouble()?.times(-1) ?: 0.0
            }
            kuudraChestKey.matchMatcher(it) {
                cost += NeuInternalName.fromItemName(it).getPriceOrNull(config.priceSource)?.times(-1) ?: 0.0
            }
            ChatUtils.debug(totalPrice.formatCoin())
        }
        totalPrice += cost
        croesusDisplayList.add(createCroesusSingleChestDisplay(chestType, totalPrice, chestItems))
        chestList.clear()
    }

    private fun createCroesusSingleChestDisplay(
        chestType: CroesusChestType,
        totalValue: Double,
        contents: MutableList<Renderable>,
    ): List<Renderable> {
        return buildList {
            add(
                (
                    Renderable.hoverTips(
                        Renderable.text("${chestType.stackChestName}: ${totalValue.formatCoin()}"),
                        contents,
                    )
                    ),
            )
        }
    }

    private fun createCroesusDisplay() {
        val newDisplay = buildList {
            add(listOf(Renderable.text("§6§lCroesus Profit Overlay§r")))
            croesusDisplayList.forEach {
                add(it)
            }
        }
        croesusDisplay = Renderable.table(newDisplay, ySpacing = 1)
    }

    private fun getEssence(essenceName: String): Double {
        essencePattern.matchMatcher(essenceName) {
            val name = group("name")
            val rawCount = group("count").toInt()
            val count = if (name == "Crimson") rawCount * (1 + getKuudraEssenceBonus())
            else rawCount.toDouble()
            return count * (NeuInternalName.fromItemName(name).getPriceOrNull(config.priceSource) ?: 0.0)
        }
        return 0.0
    }

    private fun getAttribute(attributeName: String): Double {
        attributeShardPattern.matchMatcher(attributeName) {
            val name = group("name")
            val count = group("count").toInt()
            return count * (NeuInternalName.fromItemName(name).getPriceOrNull(config.priceSource) ?: 0.0)
        }
        return 0.0
    }

    private fun createDisplay(items: Map<Int, ItemStack>) {
        val itemsWithCost: MutableMap<String, Double> = mutableMapOf()
        items.forEach {
            if (fakeItemNamePattern.matches(it.value.displayName)) return@forEach
            if (it.value.getInternalNameOrNull() != null) {
                val cost = EstimatedItemValueCalculator.getTotalPrice(it.value)
                if (cost != null) itemsWithCost.addOrPut(it.value.getInternalName().repoItemName, cost)
            }
            if (attributeShardPattern.matches(it.value.displayName)) {
                val price = getAttribute(it.value.displayName)
                itemsWithCost.addOrPut(it.value.displayName, price)
            }
            if (essencePattern.matches(it.value.displayName)) {
                val price = getEssence(it.value.displayName)
                if (price != 0.0) itemsWithCost.addOrPut(it.value.displayName, price)
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
                itemsWithCost.put(it, name.getPriceOrNull(config.priceSource)?.times(-1) ?: 0.0)
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

    private fun getKuudraEssenceBonus(): Double {
        return ProfileStorageData.petProfiles?.pets?.filter { PetUtils.getPetProperName(it.fauxInternalName) == "KUUDRA" }
            ?.maxByOrNull { it.rarity.id }
            ?.let {
                when (it.rarity) {
                    LorenzRarity.RARE -> 0.15 / 100 * it.level
                    LorenzRarity.EPIC, LorenzRarity.LEGENDARY -> 0.2 / 100 * it.level
                    else -> 0.0
                }
            } ?: 0.0
    }

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
