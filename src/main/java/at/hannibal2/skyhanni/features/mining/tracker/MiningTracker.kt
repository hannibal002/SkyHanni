@file:Suppress("DuplicatedCode")

package at.hannibal2.skyhanni.features.mining.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.mining.MiningProfitTrackerConfig.GemstoneType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.MiningJson
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.features.mining.isTitanium
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addButton
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import net.minecraft.world.level.block.Blocks
import kotlin.collections.orEmpty
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds


typealias CategoryName = String

@SkyHanniModule
object MiningTracker {
    // region Tracker init and data population

    // Fetching config values. See MiningProfitTrackerConfig.kt
    val config get() = SkyHanniMod.feature.mining.miningTracker

    // Block Types; see [https://github.com/hannibal002/SkyHanni-REPO/blob/main/constants/Mining.json]
    private var BlockTypes : Map<String, List<NeuInternalName>>

    private val group = RepoPattern.group("data.miningtracker")

    // Used to update number of blocks mined one at a time rather than many.
    private var blockUpdateControl = false


    // Constant for "all" categories
    private const val ALL: CategoryName = "All"
    private var currentCategory: CategoryName = ALL
    /**
     * REGEX-TEST: internalName:ROUGH_RUBY_GEM
     */
    private val gemstonePatternMeta = group.pattern(
        "gemstoneidregex",
        "^internalName:(ROUGH|FLAWED|FINE|FLAWLESS)_(.+)_GEM$"
    )
    private val gemstonePattern by gemstonePatternMeta
    private var lastClickedPos: LorenzVec? = null
    private var lastClickedTime = SimpleTimeMark.farPast()

    // Should the tracker show? Tracker display conditions
    init {
        BlockTypes = emptyMap()
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { config.enabled && SkyBlockUtils.inSkyBlock && onMiningIsland() },
            onRender = {
                tracker.renderDisplay(config.position)
            },
        )
    }

    fun onMiningIsland() : Boolean{
        return MiningApi.inDwarvenMines or MiningApi.inGlaciteArea() or
            MiningApi.inEnd or MiningApi.inCrimsonIsle or MiningApi.inCrystalHollows
    }

    // Associated data when hovering over tracker lines.
    data class Data(
        @Expose var totalBlocksMined: Long = 0L
    ): ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class){

        // Description when hovering over a tracker code line
        override fun getDescription(timesGained: Long): List<String> {
            /* times gained here is only every time the sack updates, so the best calculable
                quantity is how often we gain it from sack */
            val percentage = timesGained.toDouble() / totalBlocksMined
            // TODO: Find an actually useful metric to display, especially glossy/mineral drop rate
            return listOf(
                "§7Gained §e${timesGained.addSeparators()} §7times from sack/inventory updates."
            )
        }
        // Add tracker line if coins are picked up. Maybe glacite creature mob kill coins?
        override fun getCoinName(item: TrackedItem) = "§6Coins gained while mining"

        // Description when hovering over the above line
        override fun getCoinDescription(item: TrackedItem) : List<String>{
            val mobKillCoinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7You have gained §6$mobKillCoinsFormat coins §7from mining associated tasks."
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

            val matcher = gemstonePattern.matcher(internalName.toString())
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

    // Tracker initialization
    private val tracker = SkyHanniItemTracker(
        name = "Mining Profit Tracker",
        ::Data,
        getStorage = {it.mining.miningTracker},
        trackerConfig = {config.perTrackerConfig}
    ) {drawDisplay(data=it)}

    // What should be displayed on the tracker
    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lMining Profit Tracker")
        // Block Type blocks filter
        val filter: (NeuInternalName) -> Boolean = addCategories(data)

        // Populates values into the tracker
        val profit = tracker.drawItems(data, filter, lists=this)

        // Rendering the total blocks mined field.
        val totalBlocksMined = data.totalBlocksMined
        add(
            Renderable.hoverTips(
                content = "§7Times mined: §e${totalBlocksMined.addSeparators()}",
                tips = listOf("§7You've mined §e${totalBlocksMined.addSeparators()} §7blocks not including spread.")
            ).toSearchable(),
        )
        val duration = data.getTotalUptime()
        addAll(
            tracker.addTotalProfit(
                profit,
                data.totalBlocksMined,
                "block",
                duration,
                "Blocks"
            )
        )
        if (tracker.isInventoryOpen()) {
            addButton(
                label = "Gemstone Type",
                current = config.gemstoneType,
                getName = { it.displayName },
                onChange = {
                    config.gemstoneType = it
                    tracker.update()
                },
                universe = GemstoneType.entries,
            )
        }
        tracker.addPriceFromButton(this)
    }

    //endregion


    // BlockType category control. All is for any minable quantity.
    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!config.enabled || !onMiningIsland()) return
        tracker.update()
        if (event.clickType != at.hannibal2.skyhanni.data.ClickType.LEFT_CLICK) return

        val ore = OreBlock.getByStateOrNull(event.getBlockState)
        if (ore != null) {
            lastClickedPos = event.position
            lastClickedTime = SimpleTimeMark.now()
            blockUpdateControl = true
        }
    }
    //Adding to the number of blocks mined
    @HandleEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!config.enabled || !onMiningIsland()) return
        tracker.firstUpdate()
        val oldState = event.oldState
        val newState = event.newState
        val oldBlock = oldState.block
        val newBlock = newState.block

        // So respawning blocks dont influence tracker
        if (oldState == newState ||
            oldBlock == newBlock ||
            oldBlock == Blocks.AIR ||
            oldBlock == Blocks.BEDROCK) return

        if (newBlock == Blocks.AIR ||
            newBlock == Blocks.BEDROCK ||
            isTitanium(newState)
            ){
            if (OreBlock.getByStateOrNull(oldState) != null
                && blockUpdateControl
                && event.location == lastClickedPos
                ) {
                 // Does NOT count spread
                     tracker.modify {
                         it.totalBlocksMined += 1
                     }
                // necessary because it updates twice randomly
                // TODO: Make an event handler to investigate and get rid of this switch condition
                blockUpdateControl = false
            }
        }
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent){
        if (!config.enabled) return

        if (event.source == ItemAddManager.Source.COMMAND) {
            if (config.enabled) return
            tryAddItem(event.internalName, event.amount, command = true)
            return
        }

        DelayedRun.runDelayed(500.milliseconds) {
            tryAddItem(event.internalName, event.amount, command = false)
        }
    }

    private fun tryAddItem(internalName: NeuInternalName, amount: Int, command: Boolean) {
        if (!onMiningIsland()) return
        if (!isAllowedItem(internalName)) {
            ChatUtils.debug("Ignored non-mining item pickup: $internalName'")
            return
        }

        tracker.addItem(internalName, amount, command)
    }

    private fun isAllowedItem(internalName: NeuInternalName) = BlockTypes.any { internalName in it.value }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        BlockTypes = event.getConstant<MiningJson>("Mining").categories ?: emptyMap()
    }

    private fun MutableList<Searchable>.addCategories(data: Data): (NeuInternalName) -> Boolean {
        val map = mutableMapOf<CategoryName, Int>()
        // Max size will be the total number of items in data
        map[ALL] = data.items.size
        // To display count of items, from a specific block type
        for ((name, items) in BlockTypes) {
            val amt = items.count { it in data.items }
            if (amt > 0) {
                map[name] = amt
            }
        }
        val list = map.keys.toList()
        if (currentCategory !in list) {
            currentCategory = ALL
        }

        if (tracker.isInventoryOpen()) {
            addButton(
                label = "Category",
                current = currentCategory,
                getName = { it + " §7(" + map[it] + ")" },
                onChange = {
                    currentCategory = it
                    tracker.update()
                },
                universe = list,
            )
        }

        val filter: (NeuInternalName) -> Boolean =
            if (currentCategory == ALL) {
                { true }
            } else {
                { it in (BlockTypes[currentCategory].orEmpty()) }
            }

        return filter
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent){
        event.registerBrigadier("shresetminingtracker"){
            description = "Resets the TOTAL Mining profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
