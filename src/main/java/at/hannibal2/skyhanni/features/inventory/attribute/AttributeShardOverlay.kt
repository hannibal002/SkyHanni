package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getOpenBuyOrderAmount
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchableScrollable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.toSearchable

@SkyHanniModule
object AttributeShardOverlay {

    private val config get() = AttributeShardsData.config
    private val storage get() = ProfileStorageData.profileSpecific?.attributeShards

    private var display = emptyList<Renderable>()
    private val textInput = SearchTextInput()

    private var unlockedShards = 0
    private var maxedShards = 0
    private var totalShardLevels = 0
    private var priceToMax = 0.0

    private var lastShardsData: Map<String, ProfileSpecificStorage.AttributeShardData> = emptyMap()
    private var lastItemIdsInInventory: Set<NeuInternalName> = setOf()
    private var lastTotalShardsCollected = 0
    private var lastTotalInBazaarOrders = 0

    fun updateDisplay() {
        if (!config.enabled) return
        val newData = storage?.toMap().orEmpty().filter { it.key !in AttributeShardsData.unconsumableAttributes }
        val newTotalShardsCollected = newData.values.sumOf { it.amountSyphoned + it.amountInBox }
        val newTotalInBazaarOrders = if (config.includeBazaarOrders) {
            newData.keys.sumOf { openBuyOrderAmount(it) }
        } else 0

        if (lastShardsData == newData &&
            newTotalShardsCollected == lastTotalShardsCollected &&
            newTotalInBazaarOrders == lastTotalInBazaarOrders
        ) return
        lastShardsData = newData
        lastTotalShardsCollected = newTotalShardsCollected
        lastTotalInBazaarOrders = newTotalInBazaarOrders

        reconstructDisplay()
    }

    private fun openBuyOrderAmount(shardName: String): Int =
        AttributeShardsData.shardNameToInternalName(shardName)?.getOpenBuyOrderAmount() ?: 0

    enum class AttributeShardSorting(val displayName: String) {
        PRICE_TO_NEXT_TIER("Price to Next Tier"),
        PRICE_TO_MAXED("Price to Maxed"),
        ;

        override fun toString(): String = displayName
    }

    enum class AttributeShardPriceSource(val displayName: String, val priceSource: ItemPriceSource) {
        INSTANT_BUY("BZ Instant Buy", ItemPriceSource.BAZAAR_INSTANT_BUY),
        SELL_ORDER("BZ Buy Order", ItemPriceSource.BAZAAR_INSTANT_SELL),
        ;

        override fun toString(): String = displayName
    }

    private data class AttributeShardDisplayLine(
        val displayName: String,
        val currentTier: Int,
        val priceToNextTier: Double,
        val priceUntilMaxed: Double,
        val renderLine: Searchable,
    )

    private fun reconstructDisplay() {
        val shardsWithData = lastShardsData.size
        unlockedShards = 0
        maxedShards = 0
        totalShardLevels = 0
        priceToMax = 0.0

        val lines = mutableListOf<AttributeShardDisplayLine>()

        lastItemIdsInInventory = InventoryUtils.getItemIdsInOpenChest()
        val filteredShards = lastShardsData.filter { shardData ->
            !config.onlyCurrentInventory || AttributeShardsData.shardNameToInternalName(shardData.key) in lastItemIdsInInventory
        }

        for ((shardName, shardData) in filteredShards) {
            val shardInternalName = AttributeShardsData.shardNameToInternalName(shardName) ?: continue

            val amountSyphoned = shardData.amountSyphoned
            val (tier, toNextTier, toMax) = AttributeShardsData.findTierAndAmountUntilNext(shardName, amountSyphoned)
            if (tier == 10) {
                maxedShards++
            }
            if (tier > 0) {
                unlockedShards++
                totalShardLevels += tier
            }
            lines.add(
                createShardRenderable(
                    internalName = shardInternalName,
                    currentTier = tier,
                    amountToNextTier = toNextTier,
                    amountUntilMaxed = toMax,
                    amountInHuntingBox = if (config.includeHuntingBox) shardData.amountInBox else 0,
                    amountInBazaarOrders = if (config.includeBazaarOrders) shardInternalName.getOpenBuyOrderAmount() else 0,
                ),
            )
        }

        val sorted = when (config.displaySortingMethod) {
            AttributeShardSorting.PRICE_TO_NEXT_TIER -> lines.sortedBy { it.priceToNextTier }
            AttributeShardSorting.PRICE_TO_MAXED -> lines.sortedBy { it.priceUntilMaxed }
        }
        val filtered = sorted.filter { it.isVisible() }

        val adjustedMaxShards = if (config.onlyCurrentInventory) lastItemIdsInInventory.size else AttributeShardsData.maxShards

        display = buildDisplay(adjustedMaxShards, shardsWithData, filtered)
    }

    private fun AttributeShardDisplayLine.isVisible(): Boolean {
        if (config.hideMaxed && currentTier == 10) return false
        if (config.onlyNotUnlocked && currentTier > 0) return false
        return true
    }

    private fun buildDisplay(
        adjustedMaxShards: Int,
        shardsWithData: Int,
        shardLines: List<AttributeShardDisplayLine>,
    ): List<Renderable> = buildList {
        addString("§eAttribute Shard Overlay")
        addString("§7Found Shards: §a$unlockedShards/$adjustedMaxShards")
        addString("§7Maxed Shards: §a$maxedShards/$adjustedMaxShards")
        addString("§7Total Shard Levels: §a$totalShardLevels/${adjustedMaxShards * 10}")
        if (shardsWithData != AttributeShardsData.maxShards) {
            val missingAmount = AttributeShardsData.maxShards - shardsWithData
            val plural = StringUtils.pluralize(missingAmount, "shard")
            addString("§cMissing shard data for $missingAmount $plural")
            addString("§cPlease open /am and turn on advanced mode.")
        }
        if (shardLines.isEmpty()) {
            addString("§cNo Shards Found")
            addString("§cTry changing your settings below.")
        } else {
            add(shardLines.map { it.renderLine }.buildSearchableScrollable(height = 225, textInput, velocity = 25.0))
        }
        if (priceToMax > 0) {
            val description = if (config.onlyCurrentInventory) "Shown" else "All"
            addString("§7Total Price to Max $description Shards: §6${priceToMax.shortFormat()}")
        }
        addButtons()
    }

    @Suppress("LongMethod")
    private fun MutableList<Renderable>.addButtons() {
        addRenderableButton<AttributeShardSorting>(
            label = "Sorted By",
            current = config.displaySortingMethod,
            getName = { it.displayName },
            onChange = {
                config.displaySortingMethod = it
                reconstructDisplay()
            },
        )

        addRenderableButton<AttributeShardPriceSource>(
            label = "Price Source",
            current = config.overlayPriceSource,
            getName = { it.displayName },
            onChange = {
                config.overlayPriceSource = it
                reconstructDisplay()
            },
        )

        addRenderableButton(
            label = "Hide Maxed Shards",
            config = config::hideMaxed,
            enabled = "Hide Maxed",
            disabled = "Show Maxed",
            onChange = {
                reconstructDisplay()
            },
        )

        addRenderableButton(
            label = "Only Not Unlocked",
            config = config::onlyNotUnlocked,
            enabled = "Only Not Unlocked",
            disabled = "Show All",
            onChange = {
                reconstructDisplay()
            },
        )

        addRenderableButton(
            label = "Include Hunting Box",
            config = config::includeHuntingBox,
            enabled = "Include Hunting Box",
            disabled = "Exclude Hunting Box",
            onChange = {
                reconstructDisplay()
            },
        )

        addRenderableButton(
            label = "Include Bazaar Orders",
            config = config::includeBazaarOrders,
            enabled = "Include Bazaar Orders",
            disabled = "Exclude Bazaar Orders",
            onChange = {
                reconstructDisplay()
            },
        )

        addRenderableButton(
            label = "Only Current Inventory",
            config = config::onlyCurrentInventory,
            enabled = "Only in Current Inventory",
            disabled = "Show All Shards",
            onChange = {
                reconstructDisplay()
            },
        )

        addResetHuntingBoxDataButton()
    }

    private fun MutableList<Renderable>.addResetHuntingBoxDataButton() {
        if (!config.includeHuntingBox) return

        val clickable = Renderable.clickable(
            "§7Reset hunting box shards",
            tips = listOf(
                "§cThis will reset your",
                "§ctracked hunting box shards",
                "§cif there is an error with the data",
            ),
            onLeftClick = {
                AttributeShardsData.resetHuntingBoxShards()
                reconstructDisplay()
            },
        )
        add(clickable)
    }

    private fun createShardRenderable(
        internalName: NeuInternalName,
        currentTier: Int,
        amountToNextTier: Int,
        amountUntilMaxed: Int,
        amountInHuntingBox: Int,
        amountInBazaarOrders: Int,
    ): AttributeShardDisplayLine {
        val individualPrice = internalName.getPrice(config.overlayPriceSource.priceSource)

        val alreadyCovered = amountInHuntingBox + amountInBazaarOrders
        val actualAmountToNextTier = (amountToNextTier - alreadyCovered).coerceAtLeast(0)
        val actualAmountUntilMaxed = (amountUntilMaxed - alreadyCovered).coerceAtLeast(0)

        val priceUntilNextTier = individualPrice * actualAmountToNextTier
        val priceUntilMaxed = individualPrice * actualAmountUntilMaxed
        val shardItemName = internalName.repoItemName

        val coveredString = when {
            amountInBazaarOrders != 0 -> "§eOn order"
            amountInHuntingBox != 0 -> "§aEnough in Hunting Box"
            else -> "§aNothing needed"
        }
        val priceColor = if (amountInBazaarOrders != 0) "§b" else "§6"
        val priceToNextTierString =
            if (actualAmountToNextTier == 0) coveredString else "$priceColor${priceUntilNextTier.shortFormat()}"
        val priceUntilMaxedString =
            if (actualAmountUntilMaxed == 0) coveredString else "$priceColor${priceUntilMaxed.shortFormat()}"

        priceToMax += priceUntilMaxed

        val priceString = when {
            currentTier == 10 -> "§a§lMaxed"
            config.displaySortingMethod == AttributeShardSorting.PRICE_TO_MAXED -> "§6$priceUntilMaxedString"
            else -> "§6$priceToNextTierString"
        }

        val bazaarAmount = when (config.displaySortingMethod) {
            AttributeShardSorting.PRICE_TO_MAXED -> actualAmountUntilMaxed
            AttributeShardSorting.PRICE_TO_NEXT_TIER -> actualAmountToNextTier
        }

        val tooltip = buildList {
            add(shardItemName)
            add("§7Current Tier: §e$currentTier")
            add("§7Price per Shard: §6${individualPrice.shortFormat()}")
            add("§7Amount in Hunting Box: §a${amountInHuntingBox.addSeparators()}")
            if (currentTier < 10) {
                if (currentTier != 9 && actualAmountToNextTier != 0) {
                    add("")
                    add("§7Amount to Next Tier: §a${actualAmountToNextTier.addSeparators()}")
                    add("§7Price to Next Tier: §6${priceUntilNextTier.shortFormat()}")
                }
                if (actualAmountUntilMaxed != 0) {
                    add("")
                    add("§7Amount Until Maxed: §a${actualAmountUntilMaxed.addSeparators()}")
                    add("§7Price Until Maxed: §6${priceUntilMaxed.shortFormat()}")
                }
            }
            if (amountInBazaarOrders != 0) {
                add("")
                add("§e${amountInBazaarOrders.addSeparators()}x §7in bazaar right now")
            }
            add("")
            if (bazaarAmount > 0) {
                add("§eClick to buy ${bazaarAmount.addSeparators()}x on bazaar!")
            } else {
                add("§7Nothing to buy right now")
            }
        }

        val text = " §7- $shardItemName §e$currentTier $priceString"
        val renderable = if (bazaarAmount > 0) {
            Renderable.clickable(
                text,
                tips = tooltip,
                onLeftClick = {
                    BazaarApi.searchForBazaarItem(shardItemName, bazaarAmount)
                },
            )
        } else {
            Renderable.hoverTips(text, tips = tooltip)
        }

        val stack = Renderable.item(internalName.getItemStack())
        val searchable = Renderable.horizontal(stack, renderable).toSearchable(shardItemName)

        return AttributeShardDisplayLine(
            shardItemName.removeColor(), currentTier, priceUntilNextTier, priceUntilMaxed, searchable,
        )
    }

    @HandleEvent(InventoryUpdatedEvent::class, onlyOnSkyblock = true)
    private fun onInventoryUpdated() {
        if (!AttributeShardsData.attributeMenuInventory.isInside()) return
        if (!config.onlyCurrentInventory) return

        DelayedRun.runNextTick {
            val newItemIds = InventoryUtils.getItemIdsInOpenChest()
            if (lastItemIdsInInventory != newItemIds) {
                reconstructDisplay()
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChestGuiRender() {
        if (!config.enabled) return
        if (!AttributeShardsData.attributeMenuInventory.isInside() && !AttributeShardsData.bazaarShardsInventory.isInside()) return

        if (display.isEmpty()) return
        config.displayPosition.renderRenderables(display, posLabel = "Attribute Shard Overlay")
    }

}
