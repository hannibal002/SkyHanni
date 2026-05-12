package at.hannibal2.skyhanni.features.fishing.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.fishing.SeaCreatureTrackerConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.ExcludedSeaCreatureAreasJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.combat.CocoonChatMessageEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.fishing.FishingApi.BABY_MAGMA_SLUG_NAME
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addButton
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object SeaCreatureTracker {
    private var needMigration = true
    private var needCountMigration: Boolean = ProfileStorageData.profileSpecific?.fishing?.seaCreatureTracker?.needCountMigration ?: true

    private val config get() = SkyHanniMod.feature.fishing.seaCreatureTracker

    private val tracker = SkyHanniTracker(
        "Sea Creature Tracker",
        ::Data,
        { it.fishing.seaCreatureTracker },
        trackerConfig = { config.perTrackerConfig }
    ) {
        drawDisplay(it)
    }

    data class Data(
        @Expose var amount: MutableMap<String, Int> = mutableMapOf(),
        @Expose var newData: MutableMap<String, Counts> = mutableMapOf(),
        @Expose var needCountMigration: Boolean = true
    ) : TrackerData<SessionUptime.Normal>(SessionUptime.Normal::class)

    data class Counts(
        @Expose var doubleHooks: Long = 0L,
        // This one is a bit weird, it includes true Hook Amount since it makes more sense to me?
        @Expose var cocooned: Long = 0L,
        // Should only ever increment for each cocoon.
        @Expose var trueHookAmount: Long = 0L,
        // should only ever increment by one at a time.
        @Expose var trueTotal: Long = 0L,
        )

    @HandleEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!isEnabled()) return
        val name = event.seaCreature.name

        tracker.modify {
            val amount = if (event.doubleHook) 2L else 1L
            val data = it.newData[name]
            val cocooned = (data?.cocooned ?: 0L)
            val trueHookAmount = ((data?.trueHookAmount ?: 0L) + 1L)
            val doubleHookInclusive = ((data?.doubleHooks ?: 0L) + amount)
            it.newData[name] = Counts(doubleHookInclusive, cocooned, trueHookAmount)
        }
    }

    @HandleEvent
    fun onCocoonChatMessage(event: CocoonChatMessageEvent) {
        val name = SeaCreatureManager.getRepoSeaCreatureByUserVisibleName(event.mobName)?.name ?: return
        if (name == BABY_MAGMA_SLUG_NAME) return
        tracker.modify {
            val data = it.newData[name]
            val cocooned = ((data?.cocooned ?: 0L) + 1L)
            val trueHookAmount = (data?.trueHookAmount ?: 0L)
            val doubleHookInclusive = (data?.doubleHooks ?: 0L)
            it.newData[name] = Counts(doubleHookInclusive, cocooned, trueHookAmount)
        }
    }

    private const val NAME_ALL: CategoryName = "All"
    private var currentCategory: CategoryName = NAME_ALL

    private fun getCurrentCategories(data: Data): Map<CategoryName, Int> {
        val map = mutableMapOf<CategoryName, Int>()
        map[NAME_ALL] = data.amount.size
        for ((category, names) in SeaCreatureManager.allVariants) {
            val amount = names.count { it in data.amount }
            if (amount > 0) {
                map[category] = amount
            }
        }

        return map
    }

    @HandleEvent
    fun onProfileJoin() {
        needMigration = true
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        tryToMigrate(data.amount)
        tryToMigrateCounts(data.amount)

        addSearchString("§7Sea Creature Tracker:")

        val filter: (String) -> Boolean = addCategories(data)
        val realAmount = data.newData.filter { filter(it.key)}

        var total = 0L
        var visibleTotal = 0L
        realAmount.values.forEach { total += it.trueHookAmount }
        for ((name, fishedCounts) in realAmount.entries.sortedByDescending { it.value.trueHookAmount }) {
            val displayName = SeaCreatureManager.allFishingMobs[name]?.displayName ?: run {
                ErrorManager.logErrorStateWithData(
                    "Sea Creature Tracker can not display a name correctly",
                    "Could not find sea creature by name",
                    "SeaCreatureManager.allFishingMobs.keys" to SeaCreatureManager.allFishingMobs.keys,
                    "name" to name,
                )
                name
            }

            val percentageSuffix = if (config.showPercentage.get()) {
                val percentage = (fishedCounts.trueHookAmount.toDouble() / total).formatPercentage()
                " §7$percentage"
            } else ""

            val configAccountedCounts = getDisplayStrings(fishedCounts)

            val doubleHookText = if (configAccountedCounts.doubleHooks == 0L) "" else " §e${configAccountedCounts.doubleHooks.addSeparators()} "
            val cocoonText = if (configAccountedCounts.cocooned == 0L) "" else " §f${configAccountedCounts.cocooned.addSeparators()} "

            visibleTotal += (fishedCounts.cocooned + fishedCounts.doubleHooks/2 + fishedCounts.trueHookAmount)
            addSearchString(" §7- §e${configAccountedCounts.trueTotal.addSeparators()} $doubleHookText$cocoonText$displayName$percentageSuffix", displayName)
        }

        addSearchString(" §7- §e${total.addSeparators()} §7Total Sea Creatures")
    }

    private fun getDisplayStrings(fishedCounts: Counts): Counts {
        val tempHolder = Counts(0L, 0L, 0L, trueTotal = fishedCounts.trueHookAmount)
        when (config.cocoonDisplayType) {
            SeaCreatureTrackerConfig.CreatureCountDisplayType.MAIN_COUNT -> tempHolder.trueTotal += fishedCounts.cocooned
            SeaCreatureTrackerConfig.CreatureCountDisplayType.OWN_COUNT -> tempHolder.cocooned = fishedCounts.cocooned
            else -> tempHolder.cocooned = 0L
        }
        when (config.doubleHookDisplayType) {
            SeaCreatureTrackerConfig.CreatureCountDisplayType.MAIN_COUNT -> tempHolder.trueTotal += fishedCounts.doubleHooks / 2
            SeaCreatureTrackerConfig.CreatureCountDisplayType.OWN_COUNT -> tempHolder.doubleHooks += fishedCounts.doubleHooks
            else -> tempHolder.doubleHooks = 0L
        }
        return tempHolder
    }

    private fun tryToMigrateCounts(data: MutableMap<String, Int>) {
        if (!needCountMigration) return
        for (entry in data) {
            val count = entry.value
            tracker.modify {
                it.newData[entry.key] = Counts(0L, 0, count.toLong())
                // Ok so,
            }
        }
        tracker.modify {
            it.needCountMigration = false
        }
    }

    // Hypixel renames sea creatures from time to time. This migration process fixes the invalid config entries.
    private fun tryToMigrate(data: MutableMap<String, Int>) {
        if (!needMigration) return
        needMigration = false

        val map = mutableMapOf(
            "Phlhlegblast" to "Plhlegblast",
            "Sea Emperor" to "The Sea Emperor",
            "The Sea Emperor" to "The Loch Emperor",
        )

        for ((oldName, newName) in map) {
            // only migrate once the repo contains the new name
            if (SeaCreatureManager.allFishingMobs.containsKey(newName)) {
                data[oldName]?.let {
                    ChatUtils.debug("Sea Creature Tracker migrated $it $oldName to $newName")
                    data[newName] = it + (data[newName] ?: 0)
                    data.remove(oldName)
                }
            }
        }
    }

    private fun MutableList<Searchable>.addCategories(data: Data): (String) -> Boolean {
        val amounts = getCurrentCategories(data)
        val list = amounts.keys.toList()
        if (currentCategory !in list) {
            currentCategory = NAME_ALL
        }

        if (tracker.isInventoryOpen()) {
            addButton(
                label = "Category",
                current = currentCategory,
                getName = { it.allLettersFirstUppercase() + " §7(" + amounts[it] + ")" },
                onChange = {
                    currentCategory = it
                    tracker.update()
                },
                universe = list,
            )
        }

        return if (currentCategory == NAME_ALL) {
            { true }
        } else filterCurrentCategory()
    }

    private fun filterCurrentCategory(): (String) -> Boolean {
        val items = SeaCreatureManager.allVariants[currentCategory] ?: run {
            ErrorManager.logErrorStateWithData(
                "Sea Creature Tracker can not find all sea creature variants",
                "Sea creature variant is not found",
                "SeaCreatureManager.allVariants.keys" to SeaCreatureManager.allVariants.keys,
                "currentCategory" to currentCategory,
            )
            return { true }
        }
        return { it in items }
    }

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.showPercentage) {
            tracker.update()
        }
    }

    @HandleEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        tracker.firstUpdate()
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled) return false
        if (!isEnabled()) return false
        if (inDisabledArea()) return false
        if (!FishingApi.isFishing(checkRodInHand = false)) return false

        return true
    }

    private var excludedIslands = emptySet<IslandType>()
    private var excludedGraphAreas = emptySet<String>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ExcludedSeaCreatureAreasJson>("fishing/ExcludedSeaCreatureAreas")
        excludedIslands = data.excludedIslands?.toSet().orEmpty()
        excludedGraphAreas = data.excludedGraphAreas?.toSet().orEmpty()
    }

    private fun inDisabledArea() = when {
        SkyBlockUtils.currentIsland in excludedIslands -> true
        SkyBlockUtils.graphArea in excludedGraphAreas -> true
        else -> false
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetseacreaturetracker") {
            description = "Resets the Sea Creature Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock &&
        !FishingApi.hasTreasureHook &&
        !FishingApi.wearingTrophyArmor
}
