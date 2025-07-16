package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.buildSearchableScrollable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
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
    private var lastTotalSyphoned = 0

    fun updateDisplay() {
        if (!config.enabled) return
        val newData = storage?.toMap().orEmpty().filter { it.key !in AttributeShardsData.unconsumableAttributes }
        val newTotalSyphoned = newData.values.sumOf { it.amountSyphoned }

        if (lastShardsData == newData && newTotalSyphoned == lastTotalSyphoned) return
        lastShardsData = newData
        lastTotalSyphoned = newTotalSyphoned

        reconstructDisplay()
    }

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

        for ((shardName, shardData) in lastShardsData) {
            val shardInternalName = AttributeShardsData.shardNameToInternalName(shardName)
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
                ),
            )
        }

        val sorted = when (config.displaySortingMethod) {
            AttributeShardSorting.PRICE_TO_NEXT_TIER -> lines.sortedBy { it.priceToNextTier }
            AttributeShardSorting.PRICE_TO_MAXED -> lines.sortedBy { it.priceUntilMaxed }
        }
        val filtered = sorted.filter { line ->
            if (config.hideMaxed && line.currentTier == 10) return@filter false
            if (config.onlyNotUnlocked && line.currentTier > 0) return@filter false
            true
        }

        display = buildList {
            add(StringRenderable("§eAttribute Shard Overlay"))
            add(StringRenderable("§7Found Shards: §a$unlockedShards/${AttributeShardsData.maxShards}"))
            add(StringRenderable("§7Maxed Shards: §a$maxedShards/${AttributeShardsData.maxShards}"))
            add(StringRenderable("§7Total Shard Levels: §a$totalShardLevels/${AttributeShardsData.maxShards * 10}"))
            if (shardsWithData != AttributeShardsData.maxShards) {
                val missingAmount = AttributeShardsData.maxShards - shardsWithData
                val plural = StringUtils.pluralize(missingAmount, "shard")
                add(StringRenderable("§cMissing shard data for $missingAmount $plural"))
                add(StringRenderable("§cPlease open /am and turn on advanced mode."))
            }
            if (filtered.isEmpty()) {
                add(StringRenderable("§cNo Shards Found"))
                add(StringRenderable("§cTry changing your settings below."))
            } else {
                add(filtered.map { it.renderLine }.buildSearchableScrollable(height = 225, textInput, velocity = 25.0))
            }
            if (priceToMax > 0) {
                add(StringRenderable("§7Total Price to Max All Shards: §6${priceToMax.shortFormat()}"))
            }
            addButtons()
        }
    }

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
    }

    private fun createShardRenderable(
        internalName: NeuInternalName,
        currentTier: Int,
        amountToNextTier: Int,
        amountUntilMaxed: Int,
        amountInHuntingBox: Int,
    ): AttributeShardDisplayLine {
        val individualPrice = internalName.getPrice(config.overlayPriceSource.priceSource)

        val actualAmountToNextTier = (amountToNextTier - amountInHuntingBox).coerceAtLeast(0)
        val actualAmountUntilMaxed = (amountUntilMaxed - amountInHuntingBox).coerceAtLeast(0)

        val priceUntilNextTier = individualPrice * actualAmountToNextTier
        val priceUntilMaxed = individualPrice * actualAmountUntilMaxed
        val shardItemName = internalName.repoItemName

        val priceToNextTierString = if (actualAmountToNextTier == 0) {
            "§aEnough in Hunting Box"
        } else {
            "§6${(individualPrice * actualAmountToNextTier).shortFormat()}"
        }
        val priceUntilMaxedString = if (actualAmountUntilMaxed == 0) {
            "§aEnough in Hunting Box"
        } else {
            "§6${(individualPrice * actualAmountUntilMaxed).shortFormat()}"
        }

        priceToMax += priceUntilMaxed

        val priceString = when {
            currentTier == 10 -> "§a§lMaxed"
            config.displaySortingMethod == AttributeShardSorting.PRICE_TO_MAXED -> "§6${priceUntilMaxed.shortFormat()}"
            else -> "§6$priceToNextTierString"
        }

        val bazaarAmount = when {
            actualAmountUntilMaxed == 0 -> 1
            config.displaySortingMethod == AttributeShardSorting.PRICE_TO_MAXED -> actualAmountUntilMaxed
            else -> actualAmountToNextTier
        }.coerceAtLeast(1)

        val tooltip = buildList {
            add(shardItemName)
            add("§7Current Tier: §e$currentTier")
            add("§7Price per Shard: §6${individualPrice.shortFormat()}")
            add("§7Amount in Hunting Box: §a${amountInHuntingBox.addSeparators()}")
            if (currentTier < 10) {
                if (currentTier != 9) {
                    add("")
                    if (actualAmountToNextTier != 0) add("§7Amount to Next Tier: §a$actualAmountToNextTier")
                    add("§7Price to Next Tier: §6$priceToNextTierString")
                }
                add("")
                if (actualAmountUntilMaxed != 0) add("§7Amount Until Maxed: §a$actualAmountUntilMaxed")
                add("§7Price Until Maxed: §6$priceUntilMaxedString")
            }
            add("")
            add("§eClick to open on bazaar!")
        }

        val stack = ItemStackRenderable(internalName.getItemStack())

        val clickable = Renderable.clickable(
            " §7- $shardItemName §e$currentTier $priceString",
            tips = tooltip,
            onLeftClick = {
                BazaarApi.searchForBazaarItem(shardItemName, bazaarAmount)
            },
        )
        val searchable = HorizontalContainerRenderable(
            listOf(stack, clickable),
        ).toSearchable(shardItemName)

        return AttributeShardDisplayLine(
            shardItemName.removeColor(), currentTier, priceUntilNextTier, priceUntilMaxed, searchable,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!AttributeShardsData.attributeMenuInventory.isInside() && !AttributeShardsData.bazaarShardsInventory.isInside()) return

        if (display.isEmpty()) return
        config.displayPosition.renderRenderables(display, posLabel = "Attribute Shard Overlay")
    }

}
