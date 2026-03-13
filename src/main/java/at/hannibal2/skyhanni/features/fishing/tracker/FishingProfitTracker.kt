package at.hannibal2.skyhanni.features.fishing.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.FishingProfitItemsJson
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.fishing.FishingCatchEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addButton
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.data.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

typealias CategoryName = String

@SkyHanniModule
object FishingProfitTracker : SkyHanniItemTracker<FishingProfitTracker.Data>("Fishing Profit Tracker") {
    override val config get() = SkyHanniMod.feature.fishing.fishingProfitTracker
    override val storageAccessor: (ProfileSpecificStorage) -> Data = { it.fishing.fishingProfitTracker }
    override val renderConfig = RenderDisplayConfig(
        outsideInventory = true,
        inOwnInventory = true,
        condition = { isEnabled() && config.enabled && shouldShow }
    )

    /**
     * REGEX-TEST: §5⛃ §r§5§lGOOD CATCH! §r§fYou caught §r§636,064 Coins§r§f!
     * REGEX-TEST: §6⛃ §r§6§lGREAT CATCH! §r§fYou caught §r§6133,431 Coins§r§f!
     */
    private val coinsChatPattern by RepoPattern.pattern(
        "fishing.tracker.chat.coins",
        "§(?<colorCode>.*)⛃ §r(?<catch>.*) CATCH! §r§fYou caught §r§6(?<coins>[\\d,]+) Coins§r§f!",
    )

    private var lastCatchTime = SimpleTimeMark.farPast()

    data class Data(
        @Expose var totalCatchAmount: Long = 0L
    ) : ItemTrackerData<SessionUptime.Normal>() {
        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / totalCatchAmount
            val catchRate = percentage.coerceAtMost(1.0).formatPercentage()

            return listOf(
                "§7Caught §e${timesGained.addSeparators()} §7times.",
                "§7Your catch rate: §c$catchRate",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Fished Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val mobKillCoinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7You fished up §6$mobKillCoinsFormat coins §7already.",
            )
        }

        override fun getCustomPricePer(internalName: NeuInternalName, tracker: SkyHanniTracker<*>): Double {
            return if (internalName.getItemCategoryOrNull() == ItemCategory.TROPHY_FISH) {
                tracker.getPricePer(MAGMA_FISH) * FishingApi.getFilletPerTrophy(internalName)
            } else super.getCustomPricePer(internalName, tracker)
        }
    }

    private val MAGMA_FISH = "MAGMA_FISH".toInternalName()

    private const val NAME_ALL: CategoryName = "All"
    private var currentCategory: CategoryName = NAME_ALL
    private var itemCategories = mapOf<String, List<NeuInternalName>>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        itemCategories = event.getConstant<FishingProfitItemsJson>("FishingProfitItems").categories
    }

    private fun getCurrentCategories(data: Data): Map<CategoryName, Int> {
        val map = mutableMapOf<CategoryName, Int>()
        map[NAME_ALL] = data.items.size
        for ((name, items) in itemCategories) {
            val amount = items.count { it in data.items }
            if (amount > 0) {
                map[name] = amount
            }
        }

        return map
    }

    override fun drawDisplayF(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lFishing Profit Tracker")
        val filter: (NeuInternalName) -> Boolean = addCategories(data)

        val profit = drawItems(data, filter, this)

        val fishedCount = data.totalCatchAmount
        add(
            Renderable.hoverTips(
                "§7Times fished: §e${fishedCount.addSeparators()}",
                listOf("§7You've reeled in §e${fishedCount.addSeparators()} §7catches."),
            ).toSearchable(),
        )

        val duration = data.getTotalUptime()
        addAll(addTotalProfit(profit, data.totalCatchAmount, "catch", duration, "Catches"))
        addPriceFromButton(this)
    }

    private fun MutableList<Searchable>.addCategories(data: Data): (NeuInternalName) -> Boolean {
        val amounts = getCurrentCategories(data)
        checkMissingItems(data)
        val list = amounts.keys.toList()
        if (currentCategory !in list) {
            currentCategory = NAME_ALL
        }

        if (isInventoryOpen()) {
            addButton(
                label = "Category",
                current = currentCategory,
                getName = { it + " §7(" + amounts[it] + ")" },
                onChange = {
                    currentCategory = it
                    update()
                },
                universe = list,
            )
        }

        val filter: (NeuInternalName) -> Boolean = if (currentCategory == NAME_ALL) {
            { true }
        } else {
            { it in (itemCategories[currentCategory].orEmpty()) }
        }
        return filter
    }

    private fun checkMissingItems(data: Data) {
        val missingItems = mutableListOf<NeuInternalName>()
        for (internalName in data.items.keys) {
            // TODO remove workaround to not warn about ATTRIBUTE_SHARD
            if (internalName == "ATTRIBUTE_SHARD".toInternalName()) continue
            if (itemCategories.none { internalName in it.value }) {
                missingItems.add(internalName)
            }
        }
        if (missingItems.isNotEmpty()) {
            val label = StringUtils.pluralize(missingItems.size, "item", withNumber = true)
            ErrorManager.logErrorStateWithData(
                "Loaded $label not in a fishing category",
                "Found items missing in itemCategories",
                "missingItems" to missingItems,
                noStackTrace = true,
            )
        }
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled()) return

        if (event.source == ItemAddManager.Source.COMMAND) {
            if (!config.enabled) return
            tryAddItem(event.internalName, event.amount, command = true)
            return
        }

        DelayedRun.runDelayed(500.milliseconds) {
            tryAddItem(event.internalName, event.amount, command = false)
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        coinsChatPattern.matchMatcher(event.message) {
            tryAddItem(NeuInternalName.SKYBLOCK_COIN, group("coins").formatInt(), command = false)
            addCatch()
        }
    }

    private fun addCatch() {
        modify {
            it.totalCatchAmount++
        }
        lastCatchTime = SimpleTimeMark.now()
    }

    private val isRecentPickup: Boolean
        get() = config.showWhenPickup && lastCatchTime.passedSince() < 3.seconds

    private val shouldShow: Boolean
        get() = isRecentPickup || FishingApi.isFishing(checkRodInHand = false)

    @HandleEvent
    fun onWorldChange() {
        lastCatchTime = SimpleTimeMark.farPast()
    }

    private fun tryAddItem(internalName: NeuInternalName, amount: Int, command: Boolean) {
        if (!FishingApi.isFishing(checkRodInHand = false)) return
        if (!isAllowedItem(internalName)) {
            ChatUtils.debug("Ignored non-fishing item pickup: $internalName'")
            return
        }

        addItem(internalName, amount, command)
    }

    private fun isAllowedItem(internalName: NeuInternalName) = itemCategories.any { internalName in it.value }

    @HandleEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        firstUpdate()
    }

    @HandleEvent
    fun onCatch(event: FishingCatchEvent) {
        addCatch()
    }

    @HandleEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        addCatch()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && !KuudraApi.inKuudra

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetfishingtracker") {
            description = "Resets the Fishing Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }
}
