package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonProfitTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import java.util.EnumMap
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonProfitTracker : SkyHanniBucketedItemTracker<DungeonFloor, DungeonProfitTracker.BucketData>(
    "Dungeon Profit Tracker",
    ::BucketData,
    { it.dungeons.profitTracker },
    { drawDisplay(it) },
    trackerConfig = { SkyHanniMod.feature.dungeon.profitTracker.perTrackerConfig },
    customUptimeControl = true,
) {
    val config get() = SkyHanniMod.feature.dungeon.profitTracker

    // this only exists to ignore croesus openings for floors the player did prior to this feature being implemented.
    val availableCroesus: MutableMap<DungeonFloor, CroesusStorage>? get() = ProfileStorageData.profileSpecific?.dungeons?.availableCroesus

    private val KISMET_FEATHER = "KISMET_FEATHER".toInternalName()

    var hasOpened = false
    var hasUsedKey = false

    var lastChangeTime = SimpleTimeMark.farPast()

    init {
        initRenderer({ config.position }) { shouldShowDisplay() }
    }

    @HandleEvent
    fun onIslandJoin(event: IslandJoinEvent) {
        if (event.island == IslandType.DUNGEON_HUB) {
            firstUpdate()
        }
    }

    // TODO show while holding kismet or dungeon key in hand always
    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled) return false
        if (!IslandTypeTag.DUNGEON_ISLANDS.isInIsland()) return false
        if (DungeonApi.dungeonFloorEnum == DungeonFloor.E) return false
        if (lastChangeTime.passedSince() < 30.seconds) return true

        if (config.showAlways && IslandType.CATACOMBS.isInIsland()) {
            return true
        }

        if (IslandType.DUNGEON_HUB.isInIsland()) {
            IslandGraphs.nodeOrNull("Croesus", GraphNodeTag.NPC)?.let {
                if (it.position.distanceSqToPlayer() < 100) {
                    return true
                }
            }
        }


        return false
    }

    data class BucketData(
        @Expose var runsParticipated: MutableMap<DungeonFloor, Long> = EnumMap(DungeonFloor::class.java),
        @Expose var coinsSpent: MutableMap<DungeonFloor, Long> = EnumMap(DungeonFloor::class.java),
        @Expose var chestsOpened: MutableMap<DungeonFloor, Long> = EnumMap(DungeonFloor::class.java),
        @Expose var kismetsUsed: MutableMap<DungeonFloor, Long> = EnumMap(DungeonFloor::class.java),
        @Expose var keysUsed: MutableMap<DungeonFloor, Long> = EnumMap(DungeonFloor::class.java),
    ) : BucketedItemTrackerData<DungeonFloor, SessionUptime.Normal>(DungeonFloor::class, SessionUptime.Normal::class) {
        override fun getDescription(bucket: DungeonFloor?, timesGained: Long): List<String> {
            val participated = runsParticipated[bucket]
            val percentage = participated?.let { timesGained.toDouble() / it } ?: 1.0
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        val totalFloorParticipated get() = runsParticipated.sumAllValues().toLong()
        val totalKismetsUsed get() = kismetsUsed.sumAllValues().toLong()

        override fun getCoinName(bucket: DungeonFloor?, item: TrackedItem) = "§6Dungeon Chest Coins"

        override fun getCoinDescription(bucket: DungeonFloor?, item: TrackedItem): List<String> {
            val pestsCoinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7Dungeon chests never give you coins.",
                "§7You got §6$pestsCoinsFormat coins §7that way.",
            )
        }

        override fun DungeonFloor.isBucketSelectable() = this != DungeonFloor.E

        override fun bucketName(): String {
            return "Floor"
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (event.cleanMessage == "You used a Kismet Feather!") {
            modify {
                val floor = DungeonApi.dungeonFloorEnum
                if (floor != null) {
                    it.kismetsUsed.addOrPut(floor, 1)
                }

            }
        }
    }

    @HandleEvent
    fun onDungeonComplete(event: DungeonCompleteEvent) {
        if (event.dungeonFloor == DungeonFloor.E) return
        val dungeonFloor = event.dungeonFloor
        availableCroesus?.let {
            val floor = it[dungeonFloor] ?: run {
                val croesusStorage = CroesusStorage()
                it[dungeonFloor] = croesusStorage
                croesusStorage
            }
            floor.keys++
            floor.chests++
            modify {
                it.runsParticipated.addOrPut(event.dungeonFloor, 1)
            }
        }
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (shouldShowDisplay() && event.source == ItemAddManager.Source.COMMAND) {
            event.addItemFromEvent()
        }
    }

    private fun drawDisplay(bucketData: BucketData): List<Searchable> = buildList {
        addSearchString("§e§lDungeon Profit Tracker")
        addBucketSelector(this, bucketData, "Floor Type")

        var profit = drawItems(bucketData, { true }, this)

        val selectedBucket = bucketData.selectedBucket
        val pestCount = selectedBucket?.let { bucketData.runsParticipated[it] ?: 0 } ?: bucketData.totalFloorParticipated


        val pestCountFormat = "§7${selectedBucket ?: "Floors"} done: §e${pestCount.addSeparators()}"

        add(
            when {
                selectedBucket != null -> Renderable.text(pestCountFormat).toSearchable()
                else -> Renderable.hoverTips(
                    pestCountFormat,
                    buildList {
                        // Sort by A-Z in displaying real types
                        bucketData.runsParticipated.toList()
                            .forEach { (type, count) ->
                                add("§7$type: §e${count.addSeparators()}")
                            }
                    },
                ).toSearchable()
            },
        )

        profit -= calculateKismet(selectedBucket, bucketData)

        val duration = bucketData.getTotalUptime()
        addAll(addTotalProfit(profit, bucketData.totalFloorParticipated, "run", duration, "runs"))

        addPriceFromButton(this)
    }

    private fun MutableList<Searchable>.calculateKismet(
        selectedBucket: DungeonFloor?,
        bucketData: BucketData,
    ): Double {
        val amount = selectedBucket?.let { bucketData.kismetsUsed[it] ?: 0 } ?: bucketData.totalKismetsUsed
        val price = KISMET_FEATHER.getPrice() * amount
        val priceDisplay = if (amount != 0L) " §7(§c-${price.shortFormat()}§7)" else ""
        add(Renderable.text("§7Kismets used: §e$amount$priceDisplay").toSearchable("kismet"))
        return price
    }
}


class CroesusStorage {
    @Expose
    var chests = 0

    @Expose
    var keys = 0
}
