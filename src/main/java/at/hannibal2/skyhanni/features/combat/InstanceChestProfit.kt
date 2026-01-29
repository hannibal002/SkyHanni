package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonEnterEvent
import at.hannibal2.skyhanni.events.kuudra.KuudraEnterEvent
import at.hannibal2.skyhanni.features.combat.InstanceChestAPI.CroesusChestType
import at.hannibal2.skyhanni.features.combat.InstanceChestAPI.isInCroesusMenu
import at.hannibal2.skyhanni.features.combat.InstanceChestAPI.isInstanceChestGUI
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getRawCraftCostOrNull
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.NONE
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhite
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.emptyText
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.ItemStack

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

    private var croesusDisplay: Renderable? = null
    private val alreadyProcessedChests = mutableListOf<CroesusChestType>()
    private val croesusDisplayList = mutableListOf<Renderable>()
    private var slotToHighlight: Pair<Int, Double>? = null
    private val slotsWithFavorites: MutableList<String> = mutableListOf()
    private var chestDisplay: Renderable? = null
    private val chestProfits: MutableMap<String, Double> = mutableMapOf()
    private val profileStorage get() = ProfileStorageData.profileSpecific


    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.enabled && !config.croesusAllChestsOverlay && !config.croesusHighlight) return

        if (isInCroesusMenu() && (config.croesusAllChestsOverlay || config.croesusHighlight)) {
            event.inventoryItems.forEach { (slot, item) ->
                val chestType = CroesusChestType.getByStackName(item.hoverName.formattedTextCompatLeadingWhite())
                if (chestType != null) {
                    if (!alreadyProcessedChests.contains(chestType)) {
                        alreadyProcessedChests.add(chestType)
                        parseCroesusChest(item, chestType, slot)
                    }
                }
            }
            createCroesusDisplay()
        }

        if (isInstanceChestGUI() && config.enabled) createDisplay(event.inventoryName, event.inventoryItems)
    }

    @HandleEvent(priority = HandleEvent.LOWEST, onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        val slot = slotToHighlight?.first
        if (isInCroesusMenu() && slot != null && config.croesusHighlight) {
            event.container.slots[slot].highlight(LorenzColor.GREEN)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTip(event: RenderItemTipEvent) {
        val slots = slotsWithFavorites
        if (isInCroesusMenu()) {
            slots.forEach {
                if (it == event.stack.hoverName.formattedTextCompatLeadingWhite()) event.stackTip = "§6✯"
            }
        }
        if (isInstanceChestGUI()) {
            if (profileStorage?.instanceChestFavoriteItems?.contains(event.stack.getInternalName()) == true) event.stackTip = "§6✯"
        }
    }

    @HandleEvent
    fun onKey(event: GuiKeyPressEvent) {
        if (!config.keybind.isKeyHeld()) return
        val favoriteItems = profileStorage?.instanceChestFavoriteItems ?: mutableListOf()
        stackUnderCursor()?.getInternalNameOrNull()?.let {
            if (favoriteItems.contains(it)) {
                favoriteItems.remove(it)
                ChatUtils.chat("Removed ${it.repoItemName}§e from Favorites List.")
            } else {
                favoriteItems.add(it)
                ChatUtils.chat("Added ${it.repoItemName}§e to Favorites List.")
            }
        }
        profileStorage?.instanceChestFavoriteItems = favoriteItems
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        alreadyProcessedChests.clear()
        croesusDisplayList.clear()
        slotsWithFavorites.clear()
        slotToHighlight = null
        croesusDisplay = null
    }

    private fun parseCroesusChest(itemStack: ItemStack?, chestType: CroesusChestType, slot: Int) {
        val chestList = mutableListOf<NeuInternalName>()
        val chestTips = mutableListOf<String>()
        chestTips.add("${chestType.stackChestName}:")
        var totalPrice = 0.0
        var cost = 0.0
        var favoriteText = ""
        itemStack?.getLore()?.forEach { loreLine ->
            if (alreadyOpened.matches(loreLine)) return
            if (chestCostCroesus.matches(loreLine) || dungeonChestKey.matches(loreLine)) {
                cost += getPrice(NeuInternalName.fromItemNameOrNull(loreLine) ?: NONE).times(-1)
                chestCostCroesus.matchMatcher(loreLine) {
                    cost += groupOrNull("amount")?.formatInt()?.toDouble()?.times(-1) ?: 0.0
                }
            } else {
                var itemPrice: Double
                var itemName = ItemUtils.readBookType(loreLine) ?: loreLine
                var itemInternalName = NeuInternalName.fromItemNameOrNull(itemName)
                bookColorFixer.matchMatcher(itemName) {
                    itemName = ItemResolutionQuery.resolveEnchantmentByName(group("item")) ?: itemName
                    itemInternalName = itemName.toInternalName()
                }
                val internalName = itemInternalName
                var favorited = ""
                if (profileStorage?.instanceChestFavoriteItems?.contains(internalName) == true) {
                    slotsWithFavorites.add(chestType.stackChestName)
                    favorited = "§6✯"
                    favoriteText = "§6Contains Favorited Item"
                }
                if (internalName != null) {
                    itemPrice = getPrice(internalName)
                    essencePattern.matchMatcher(loreLine) {
                        itemPrice = getEssence(group("name"), group("count").toInt())
                    }
                    if (dungeonChestKey.matches(loreLine)) {
                        cost += getPrice(internalName).times(-1)
                        itemPrice = -1.0
                    }
                    if (itemPrice != -1.0) {
                        chestTips.add(" ${internalName.repoItemName}: ${itemPrice.formatCoin()} $favorited")
                        totalPrice += itemPrice
                        chestList.add(internalName)
                    }
                    kuudraChestKey.matchMatcher(loreLine) {
                        cost += internalName.getRawCraftCostOrNull(config.priceSource)?.times(-1) ?: 0.0
                    }
                }
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
        chestTips.add("Cost: ${cost.formatCoin()}")
        chestTips.add("Profit: ${totalPrice.formatCoin()} §f(Pre Cost Profit ${preCostPrice.formatCoin()}§f) ")
        croesusDisplayList.add(createCroesusSingleChestDisplay(chestType, totalPrice, chestTips, favoriteText))
    }

    private fun getPrice(internalName: NeuInternalName): Double = internalName.getPrice(config.priceSource)

    private fun createCroesusSingleChestDisplay(
        chestType: CroesusChestType,
        totalValue: Double,
        contents: MutableList<String>,
        favoriteText: String,
    ): Renderable = Renderable.hoverTips(
        "${chestType.stackChestName}: ${totalValue.formatCoin()} $favoriteText",
        contents,
    )

    private fun createCroesusDisplay() {
        val list = buildList {
            add(Renderable.text("§6§lCroesus Profit Overlay"))
            addAll(croesusDisplayList)
        }
        croesusDisplay = Renderable.vertical(list, spacing = 1)
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

    private fun createDisplay(inventoryName: String, items: Map<Int, ItemStack>) {
        /**
         * Kuudra chests say "Free Chest Chest" and "Paid Chest Chest" due to Hypixel issue
         */
        val fixedInventoryName = inventoryName.replace("Chest Chest", "Chest")

        val itemsWithCost: MutableMap<String, Double> = mutableMapOf()
        items.forEach {
            if (fakeItemNamePattern.matches(it.value.hoverName.formattedTextCompatLeadingWhiteLessResets())) return@forEach
            if (it.value.getInternalNameOrNull() != null) {
                val cost = EstimatedItemValueCalculator.getTotalPrice(it.value)
                if (cost != null) itemsWithCost.addOrPut(it.value.getInternalName().repoItemName, cost)
            }
            val name = it.value.hoverName.formattedTextCompatLeadingWhiteLessResets()
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

        chestDisplay = Renderable.vertical(buildDisplay(fixedInventoryName, itemsWithCost), spacing = 1)
    }

    private fun buildDisplay(fixedInventoryName: String, itemsWithCost: Map<String, Double>) = buildList {
        add(Renderable.text("§d§l$fixedInventoryName Profit"))
        add(Renderable.emptyText())

        var total = 0.0
        var displayedCost = false

        val revenue = itemsWithCost.values.filter { it > 0 }.sum()
        add(Renderable.text("§a§lTotal Revenue §a${revenue.formatCoin()}"))

        itemsWithCost.forEach { itemWithCost ->
            var favourited = ""
            val coinsColor = if (itemWithCost.value < 0) "§c"
            else "§a"

            if (profileStorage?.instanceChestFavoriteItems?.any { it.repoItemName == itemWithCost.key } == true) {
                favourited = " §6✯ Favourited Item"
            }

            if (!displayedCost && itemWithCost.value < 0) {
                val cost = itemsWithCost.values.filter { cost -> cost < 0 }.sum()
                add(Renderable.text(" "))
                add(Renderable.text("§c§lTotal Cost §c${cost.formatCoin()}"))
                displayedCost = true
            }

            val coins = "$coinsColor${itemWithCost.value.formatCoin()}"

            total += itemWithCost.value
            add(Renderable.text("${itemWithCost.key} $coins$favourited"))
        }

        chestProfits[fixedInventoryName] = total

        var color = if (total < 0) "§c"
        else "§a"

        add(Renderable.emptyText())
        add(Renderable.text("$color§lProfit $color${total.formatCoin()}"))

        if (!IslandType.CATACOMBS.isCurrent() && !IslandType.KUUDRA_ARENA.isCurrent()) return@buildList

        add(Renderable.emptyText())
        add(Renderable.text("§d§lAll Chest Profits"))

        for (it in chestProfits.entries.sortedByDescending { it.value }) {
            color = if (it.value < 0) "§c"
            else "§a"

            add(Renderable.text("${it.key} $color${it.value.formatCoin()}"))
        }
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
        if (config.enabled && InventoryUtils.inInventory())
            if (isInstanceChestGUI()) {
                config.position.renderRenderable(
                    chestDisplay,
                    posLabel = "Instance Chest Profit",
                )
            }
        if (config.croesusAllChestsOverlay && InventoryUtils.inInventory())
            if (isInCroesusMenu()) {
                config.croesusPosition.renderRenderable(
                    croesusDisplay,
                    posLabel = "Croesus Chest Profit",
                )
            }
    }

    @HandleEvent(DungeonEnterEvent::class)
    fun onDungeonEnter() {
        chestProfits.clear()
    }

    @HandleEvent(KuudraEnterEvent::class)
    fun onKuudraEnter() {
        chestProfits.clear()
    }
}
