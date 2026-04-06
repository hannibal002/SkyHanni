package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGH
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.BAL_SHARD_ITEM
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.EPIC_BAL_ITEM
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.JUNGLE_KEY_ITEM
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.LEGENDARY_BAL_ITEM
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.data.ItemTrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object CrystalNucleusTracker : SkyHanniItemTracker<CrystalNucleusTracker.Data>("Crystal Nucleus Tracker") {
    override val config get() = SkyHanniMod.feature.mining.crystalNucleusTracker
    override val storageAccessor: (ProfileSpecificStorage) -> Data = { it.mining.crystalNucleusTracker }
    override val renderConfig = RenderDisplayConfig(
        outsideInventory = true,
        inOwnInventory = true,
        condition = { isEnabled() },
        onlyOnIsland = IslandType.CRYSTAL_HOLLOWS,
    )
    private val patternGroup = RepoPattern.group("mining.crystalnucleus.tracker")

    /**
     * REGEX-TEST: [MVP+] oBlazin has obtained [Lvl 1] Bal!
     * REGEX-TEST: [MVP++] oBlazin has obtained [Lvl 1] Bal!
     * REGEX-TEST: oBlazin has obtained [Lvl 1] Bal!
     * REGEX-TEST: [YOUTUBE] oBlazin has obtained [Lvl 1] Bal!
     */
    private val balObtainedPattern by patternGroup.pattern(
        "bal.obtained.colorless",
        "(?:\\[.*\\+*\\] )?(?<player>.*) has obtained \\[Lvl 1\\] Bal!",
    )

    /**
     * REGEX-TEST: §6Bal
     * REGEX-TEST: §5Bal
     */
    private val balRarityPattern by patternGroup.pattern(
        "bal.rarity",
        ".*§(?<raritycolor>[65])Bal.*"
    )

    data class Data(
        @Expose var runsCompleted: Long = 0L,
    ) : ItemTrackerData<SessionUptime.Normal>() {
        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / runsCompleted
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        // No direct coin drops from nuc runs
        override fun getCoinName(item: TrackedItem) = ""
        override fun getCoinDescription(item: TrackedItem) = mutableListOf<String>()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        balObtainedPattern.matchMatcher(event.chatComponent) {
            if (!group("player").equals(PlayerUtils.getName(), ignoreCase = true)) return@matchMatcher
            balRarityPattern.matchMatcher(event.chatComponent.formattedTextCompat()) {
                val item = when (group("raritycolor")) {
                    "6" -> LEGENDARY_BAL_ITEM
                    "5" -> EPIC_BAL_ITEM
                    else -> return@matchMatcher
                }
                modify {
                    it.addItem(item, amount = 1, command = false)
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onShardGain(event: ShardGainEvent) {
        if (event.shardInternalName != BAL_SHARD_ITEM) return

        modify {
            it.addItem(BAL_SHARD_ITEM, event.amount, command = false)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetcrystalnucleustracker") {
            description = "Resets the Crystal Nucleus Tracker."
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }

    @HandleEvent(priority = HIGH)
    fun onCrystalNucleusLoot(event: CrystalNucleusLootEvent) {
        modify {
            it.runsCompleted++
        }
        for ((internalName, amount) in event.loot) {
            addItem(internalName, amount, false)
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.professorUsage.onToggle(::update)
    }

    override fun drawDisplayF(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lCrystal Nucleus Profit Tracker")

        val runsCompleted = data.runsCompleted
        if (runsCompleted > 0) {
            var profit = drawItems(data, { true }, this)
            val jungleKeyCost: Double = getPricePer(JUNGLE_KEY_ITEM) * runsCompleted
            profit -= jungleKeyCost
            val jungleKeyCostFormat = jungleKeyCost.shortFormat()
            add(
                Renderable.hoverTips(
                    " §7${runsCompleted}x §5Jungle Key§7: §c-$jungleKeyCostFormat",
                    tips = listOf(
                        "§7You lost §c$jungleKeyCostFormat §7of total profit",
                        "§7due to §5Jungle Keys§7.",
                    ),
                ).toSearchable("Jungle Key"),
            )

            val usesApparatus = CrystalNucleusApi.usesApparatus()
            val partsCost = CrystalNucleusApi.getPrecursorRunPrice { getPricePer(it) }
            val totalSapphireCost: Double = partsCost * runsCompleted
            val rawConfigString = config.professorUsage.get().toString()
            val usageString = if (usesApparatus) StringUtils.pluralize(
                runsCompleted.toInt(),
                rawConfigString,
                "§5Precursor Apparatuses",
            )
            else rawConfigString
            val usageTotal = if (usesApparatus) runsCompleted else runsCompleted * 6

            profit -= totalSapphireCost
            val totalSapphireCostFormat = totalSapphireCost.shortFormat()
            add(
                Renderable.hoverTips(
                    " §7${usageTotal}x $usageString§7: §c-$totalSapphireCostFormat",
                    tips = listOf(
                        "§7You lost §c$totalSapphireCostFormat §7of total profit",
                        "§7due to $usageString§7.",
                    ),
                ).toSearchable(usageString.removeColor()),
            )

            add(
                Renderable.hoverTips(
                    "§7Runs completed: §e${runsCompleted.addSeparators()}",
                    tips = listOf("§7You completed §e${runsCompleted.addSeparators()} §7Crystal Nucleus Runs."),
                ).toSearchable(),
            )

            val duration = data.getTotalUptime()
            addAll(addTotalProfit(profit, data.runsCompleted, "run", duration, "Runs"))
        } else {
            addSearchString("§7Do a run to start tracking!")
        }

        addPriceFromButton(this)
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled()) return

        if (event.source == ItemAddManager.Source.COMMAND) {
            addItem(event.internalName, event.amount, command = true)
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.CRYSTAL_HOLLOWS) {
            firstUpdate()
        }
    }

    private fun isAreaEnabled() = config.showOutsideNucleus || SkyBlockUtils.graphArea == "Crystal Nucleus"
    private fun isEnabled() = config.enabled && IslandType.CRYSTAL_HOLLOWS.isInIsland() && isAreaEnabled()
}
