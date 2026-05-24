package at.hannibal2.skyhanni.features.mining.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import com.google.gson.annotations.Expose
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.mining.MiningProfitTrackerConfig.GemstoneType
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.MiningJson
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.features.mining.OreCategory
import at.hannibal2.skyhanni.features.mining.isTitanium
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addButton
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import net.minecraft.world.level.block.Blocks
import java.util.EnumMap
import kotlin.math.pow

val OreCategory.miningCategory: MiningCategory
    get() = when (this) {
        OreCategory.GEMSTONE -> MiningCategory.GEMSTONES
        OreCategory.DWARVEN_METAL -> MiningCategory.DWARVEN_METALS
        OreCategory.ORE -> MiningCategory.ORES
        OreCategory.BLOCK -> MiningCategory.BLOCKS
    }


enum class MiningCategory(val jsonKey: String) {
    GEMSTONES("Gemstones"),
    DWARVEN_METALS("Dwarven Metals"),
    BLOCKS("Blocks/Stones"),
    ORES("Ores"),
    MINING_FIESTA("Mining Fiesta Drops"),
    MISC("Misc");

    override fun toString(): String = jsonKey

    companion object {
        fun fromJsonKey(jsonKey: String): MiningCategory = entries.find { it.jsonKey == jsonKey } ?: MISC
    }
}

@SkyHanniModule
object MiningTracker : SkyHanniBucketedItemTracker<MiningCategory, MiningTracker.BucketData>(
    "Mining Profit Tracker",
    ::BucketData,
    { it.mining.miningTracker },
    MiningTracker::drawDisplay,
    trackerConfig = { SkyHanniMod.feature.mining.miningTracker.perTrackerConfig },
) {
    // region Tracker init and data population

    // Fetching config values. See MiningProfitTrackerConfig.kt
    val config get() = SkyHanniMod.feature.mining.miningTracker

    // Block Types; see [https://github.com/hannibal002/SkyHanni-REPO/blob/main/constants/Mining.json]
    private var blockTypes: Map<String, List<NeuInternalName>> = emptyMap()

    private val group = RepoPattern.group("data.miningtracker")

    // Used to update number of blocks mined one at a time rather than many.
    private var blockUpdateControl = false


    // (Category tracking is now handled by the selectedBucket property of BucketData)
    /**
     * REGEX-TEST: ROUGH_RUBY_GEM
     */
    private val gemstonePattern by group.pattern(
        "gemstoneidregex",
        "^(ROUGH|FLAWED|FINE|FLAWLESS)_(.+)_GEM$",
    )
    private var lastClickedPos: LorenzVec? = null

    // Should the tracker show? Tracker display conditions
    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { config.enabled && IslandTypeTag.MINING.isInIsland() && MiningApi.isHoldingMiningTool() },
            onRender = {
                renderDisplay(config.position)
            },
        )
    }

    data class BucketData(
        @Expose var blocksMined: MutableMap<MiningCategory, Long> = EnumMap(MiningCategory::class.java),
    ) : BucketedItemTrackerData<MiningCategory, SessionUptime.Normal>(MiningCategory::class, SessionUptime.Normal::class) {

        val safeBlocksMined: MutableMap<MiningCategory, Long>
            get() {
                return blocksMined
            }

        fun getTotalBlocksMined(): Long {
            return if (selectedBucket == null) {
                safeBlocksMined.values.sum()
            } else {
                safeBlocksMined[selectedBucket] ?: 0L
            }
        }

        override fun MiningCategory.isBucketSelectable(): Boolean = true
        override fun bucketName(): String = "Mining Category"

        // Description when hovering over a tracker code line
        override fun getDescription(bucket: MiningCategory?, timesGained: Long): List<String> {
            /* times gained here is only every time the sack updates, so the best calculable
                quantity is how often we gain it from sack- need to find a way to get individual item quantity */
            return listOf(
                "§7Gained §e${timesGained.addSeparators()} §7times from sack/inventory updates.",
            )
        }

        // Add tracker line if coins are picked up. Maybe glacite creature mob kill coins?
        override fun getCoinName(bucket: MiningCategory?, item: TrackedItem) = "§6Coins gained while mining"

        // Description when hovering over the above line
        override fun getCoinDescription(bucket: MiningCategory?, item: TrackedItem): List<String> {
            val miningCoinsGained = item.totalAmount.shortFormat()
            return listOf(
                "§7You have gained §6$miningCoinsGained coins §7from mining associated tasks.",
            )
        }

        // Custom price for gemstones in case of highly manipulated bazaar gemstone prices for rough/flawed
        // TODO: Implement this for enchanted versions of other mining blocks
        override fun getCustomPricePer(internalName: NeuInternalName, tracker: SkyHanniTracker<*, *>): Double {
            if (internalName.getItemCategoryOrNull() != ItemCategory.GEMSTONE ||
                config.gemstoneType == GemstoneType.DEFAULT
            ) {
                return super.getCustomPricePer(internalName, tracker)
            }

            val matcher = gemstonePattern.matcher(internalName.asString())
            if (!matcher.find()) return super.getCustomPricePer(internalName, tracker)

            val currentTierName = matcher.group(1)
            val gemstoneName = matcher.group(2)
            val targetType = config.gemstoneType

            val tiers = listOf("ROUGH", "FLAWED", "FINE", "FLAWLESS")
            val currentPower = tiers.indexOf(currentTierName)
            val targetPower = tiers.indexOf(targetType.name)

            // Safe case if indexOf breaks in any way
            if (currentPower == -1 || targetPower == -1) return super.getCustomPricePer(internalName, tracker)

            val targetPrice = tracker.getPricePer(("${targetType.name}_${gemstoneName}_GEM").toInternalName())
            // It takes 80 gemstones of the lower type to craft the higher
            return targetPrice * 80.0.pow(currentPower.toDouble() - targetPower.toDouble())
        }
    }

    // What should be displayed on the tracker
    fun drawDisplay(data: BucketData): List<Searchable> = buildList {
        addSearchString("§e§lMining Profit Tracker")

        addBucketSelector(this, data, "Category")

        // Populates values into the tracker
        val profit = drawItems(data, { true }, lists = this)

        // Rendering the total blocks mined field.
        val totalBlocksMined = data.getTotalBlocksMined()
        add(
            Renderable.hoverTips(
                content = "§7Times mined: §e${totalBlocksMined.addSeparators()}",
                tips = listOf("§7You've mined §e${totalBlocksMined.addSeparators()} §7blocks not including spread."),
            ).toSearchable(),
        )
        val duration = data.getTotalUptime()
        addAll(
            addTotalProfit(
                profit,
                totalBlocksMined,
                "block",
                duration,
                "Blocks",
            ),
        )
        if (isInventoryOpen()) {
            addButton(
                label = "Gemstone Type",
                current = config.gemstoneType,
                getName = { it.displayName },
                onChange = {
                    config.gemstoneType = it
                    update()
                },
                universe = GemstoneType.entries,
            )
        }
        addPriceFromButton(this)
    }

    //endregion

    // TODO: Fix OreMinedEvent!!
    // BlockType category control. All is for any minable quantity.
    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!config.enabled || !IslandTypeTag.MINING.isInIsland() || !MiningApi.isHoldingMiningTool()) return
        update()
        if (event.clickType != ClickType.LEFT_CLICK) return

        val ore = OreBlock.getByStateOrNull(event.blockState)
        if (ore != null) {
            lastClickedPos = event.position
            blockUpdateControl = true
        }
    }

    //Adding to the number of blocks mined
    @HandleEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!config.enabled || !IslandTypeTag.MINING.isInIsland()) return
        firstUpdate()
        val oldState = event.oldState
        val newState = event.newState
        val oldBlock = oldState.block
        val newBlock = newState.block

        // So respawning blocks dont influence tracker
        if (oldState == newState ||
            oldBlock == newBlock ||
            oldBlock == Blocks.AIR ||
            oldBlock == Blocks.BEDROCK
        ) return

        if (newBlock == Blocks.AIR ||
            newBlock == Blocks.BEDROCK ||
            isTitanium(newState)
        ) {
            val ore = OreBlock.getByStateOrNull(oldState)
            if (ore != null &&
                blockUpdateControl &&
                event.location == lastClickedPos
            ) {
                val category = ore.category.miningCategory
                // Does NOT count spread
                modify {
                    it.safeBlocksMined.addOrPut(category, 1L)
                }
                // necessary because it updates twice randomly
                // TODO: Make an event handler to investigate and get rid of this switch condition
                blockUpdateControl = false
            }
        }
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!config.enabled || !IslandTypeTag.MINING.isInIsland()) return

        val bucket = categoryOf(event.internalName)
        if (bucket == null) {
            ChatUtils.debug("Ignored non-mining item pickup: $event.internalName'")
            return
        }
        addItem(bucket, event.internalName, event.amount, command = (event.source == ItemAddManager.Source.COMMAND))
    }

    private fun categoryOf(internalName: NeuInternalName): MiningCategory? {
        val entry = blockTypes.entries.find { internalName in it.value } ?: return null
        return MiningCategory.fromJsonKey(entry.key)
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        blockTypes = event.getConstant<MiningJson>("Mining").categories
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetminingtracker") {
            description = "Resets the TOTAL Mining profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }
}
