package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.core.config.PositionList
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardRankConfig
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearCategories
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearEntries
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.json.fromJson
import kotlin.reflect.KClass

enum class EliteLeaderboards(
    private val displayName: String,
    val display: EliteLeaderboardDisplayBase<*, *>,
    val leaderboardType: KClass<out EliteLeaderboardType>
) {
    WEIGHT("Farming Weight", WeightDisplay(), EliteLeaderboardType.Weight::class),
    CROP("Crop Collection", CropDisplay(), EliteLeaderboardType.Crop::class),
    PEST("Pest Kills", PestDisplay(), EliteLeaderboardType.Pest::class)
    ;

    val isEnabled get() = config.enabled && this in config.display.get()
    val position get() = config.displayPositions[ordinal]
    override fun toString() = displayName

    @SkyHanniModule
    companion object EliteLeaderboardDisplayManager {
        val config get() = GardenApi.config.eliteFarmersLeaderboards
        private val cropConfig get() = config.cropCollectionLeaderboard
        private val pestConfig get() = config.pestKillsLeaderboard
        private val weightConfig get() = config.farmingWeightLeaderboard

        fun getFromTypeOrNull(type: KClass<out EliteLeaderboardType>) = entries.firstOrNull {
            it.leaderboardType == type
        }

        fun updateDisplays() {
            config.display.get().forEach { leaderboard ->
                leaderboard.display.lastUpdate = SimpleTimeMark.farPast()
            }
        }

        @HandleEvent
        fun onRenderOverlay(event: GuiRenderEvent) {
            if (config.displayPositions.isEmpty()) return
            if (!config.enabled) return
            config.display.get().forEach { leaderboard ->
                leaderboard.display.renderDisplay(leaderboard.position)
            }
        }

        fun resetDisplays() {
            EliteLeaderboards.entries.forEach { leaderboard ->
                leaderboard.display.reset()
            }
        }

        @HandleEvent
        fun onConfigLoad(event: ConfigLoadEvent) {
            val weightConfigs = listOf(
                weightConfig.rankGoals.useRankGoal,
                weightConfig.rankGoals.monthlyRankGoal,
                weightConfig.rankGoals.rankGoal,
                weightConfig.gamemode,
            )

            val cropConfigs = listOf(
                cropConfig.rankGoals.useRankGoal,
                cropConfig.rankGoals.rankGoalTypes,
                cropConfig.gamemode,
            )

            val pestConfigs = listOf(
                pestConfig.rankGoals.useRankGoal,
                pestConfig.rankGoals.rankGoalTypes,
                pestConfig.gamemode
            )

            weightConfigs.forEach {
                it.afterChange {
                    clearCategories(EliteLeaderboardType.Weight::class)
                }
            }

            cropConfigs.forEach {
                it.afterChange {
                    clearCategories(EliteLeaderboardType.Crop::class)
                }
            }

            pestConfigs.forEach {
                it.afterChange {
                    clearCategories(EliteLeaderboardType.Pest::class)
                }
            }

            for (crop in CropType.entries) {
                for (mode in EliteLeaderboardMode.entries) {
                    val leaderboardType = EliteLeaderboardType.Crop(crop, mode)
                    ConditionalUtils.onToggle(getLeaderboardRankConfig(leaderboardType)?.get() ?: continue) {
                        clearEntries(leaderboardType)
                    }
                }
            }

            for (pest in (PestType.entries + null)) {
                for (mode in EliteLeaderboardMode.entries) {
                    val leaderboardType = EliteLeaderboardType.Pest(pest, mode)
                    ConditionalUtils.onToggle(getLeaderboardRankConfig(leaderboardType)?.get() ?: continue) {
                        clearEntries(leaderboardType)
                    }
                }
            }
        }

        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.transform(1, "garden.eliteFarmingWeightoffScreenDropMessage")
            event.move(3, "garden.eliteFarmingWeightDisplay", "garden.eliteFarmingWeights.display")
            event.move(3, "garden.eliteFarmingWeightPos", "garden.eliteFarmingWeights.pos")
            event.move(3, "garden.eliteFarmingWeightLeaderboard", "garden.eliteFarmingWeights.leaderboard")
            event.move(3, "garden.eliteFarmingWeightOvertakeETA", "garden.eliteFarmingWeights.overtakeETA")
            event.move(3, "garden.eliteFarmingWeightOffScreenDropMessage", "garden.eliteFarmingWeights.offScreenDropMessage")
            event.move(3, "garden.eliteFarmingWeightOvertakeETAAlways", "garden.eliteFarmingWeights.overtakeETAAlways")
            event.move(3, "garden.eliteFarmingWeightETAGoalRank", "garden.eliteFarmingWeights.ETAGoalRank")
            event.move(3, "garden.eliteFarmingWeightIgnoreLow", "garden.eliteFarmingWeights.ignoreLow")
            event.move(14, "garden.eliteFarmingWeight.offScreenDropMessage", "garden.eliteFarmingWeights.showLbChange")
            event.move(34, "garden.eliteFarmingWeights.ETAGoalRank", "garden.eliteFarmingWeights.etaGoalRank")

            val base = "#garden.farmingWeight"
            event.move(101, "$base.lastFarmingWeightLeaderboard", "$base.lastLeaderboard")

            val displayList: List<FarmingWeightTextEntry> = buildList {
                add(FarmingWeightTextEntry.WEIGHT_POSITION)
                event.transform(117, "garden.eliteFarmingWeights.overtakeETA") { entry ->
                    if (entry.asBoolean) add(FarmingWeightTextEntry.OVERTAKE)
                    entry
                }

            }

            event.add(120, "garden.eliteFarmingWeights.text") {
                ConfigManager.gson.toJsonTree(displayList)
            }

            val oldConfig = "garden.eliteFarmingWeights"
            val newConfig = "garden.eliteFarmersLeaderboards.farmingWeightLeaderboard"
            val display = "$newConfig.display"
            val rankGoal = "$newConfig.rankGoals"

            // While pest and crop leaderboards are new, we'll guess if players want them on or off based on their other preferences
            val leaderboardDisplayList: List<EliteLeaderboards> = buildList {
                event.transform(120, "$oldConfig.display") { entry ->
                    if (entry.asBoolean) add(WEIGHT)
                    entry
                }
                event.transform(120, "garden.cropMilestones.progress") { entry ->
                    if (entry.asBoolean) add(CROP)
                    entry
                }
                event.transform(120, "garden.pests.pestProfitTracker.enabled") { entry ->
                    if (entry.asBoolean) add(PEST)
                    entry
                }
            }
            event.add(120, "garden.eliteFarmersLeaderboards.display") {
                ConfigManager.gson.toJsonTree(leaderboardDisplayList)
            }
            event.move(120, "$oldConfig.pos", "garden.eliteFarmersLeaderboards.displayPositions") { entry ->
                val positionList = PositionList(EliteLeaderboards.entries.size)
                positionList[WEIGHT.ordinal] = ConfigManager.gson.fromJson<Position>(entry)
                ConfigManager.gson.toJsonTree(positionList)
            }

            event.move(120, "$oldConfig.showOutsideGarden", "$display.showOutsideGarden")
            event.move(120, "$oldConfig.text", "$display.text")
            event.move(120, "$oldConfig.leaderboard", "$display.leaderboard")
            event.move(120, "$oldConfig.overtakeETA", "$display.overtakeETA")
            event.move(120, "$oldConfig.overtakeETAAlways", "$display.overtakeETAAlways")
            event.move(120, "$oldConfig.showLbChange", "$newConfig.offlineLbChange")
            event.move(120, "$oldConfig.etaGoalRank", "$rankGoal.rankGoal")
            event.move(120, "$oldConfig.etaGoalRank", "$rankGoal.monthlyRankGoal")
            event.move(120, "$oldConfig.etaGoalRank", "$rankGoal.useRankGoal") { entry ->
                ConfigManager.gson.toJsonTree(entry.asString != "10000")
            }
            event.move(120, "$oldConfig.ignoreLow", "$display.ignoreLow")
        }


        enum class FarmingWeightTextEntry(private val displayName: String) {
            WEIGHT_POSITION("§6Farming Weight: §e104,481.49 §7[§b#5§7]"),
            OVERTAKE("§e170.21 §7(§b12h 32m 15s§7) §7behind §bChissl")
            ;

            override fun toString() = displayName
        }
    }
}
