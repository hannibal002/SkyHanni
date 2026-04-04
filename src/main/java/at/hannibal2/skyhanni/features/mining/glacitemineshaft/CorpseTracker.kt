package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.mining.CorpseLootedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.data.BucketedItemTrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object CorpseTracker : SkyHanniBucketedItemTracker<CorpseType, CorpseTracker.BucketData>(
    "Corpse Tracker",
) {
    override val storageAccessor: (ProfileSpecificStorage) -> BucketData = { it.mining.mineshaft.corpseProfitTracker }
    override val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.corpseTracker
    override val renderConfig = RenderDisplayConfig(
        condition = { isEnabled() },
    )

    data class BucketData(
        @Expose var corpsesLooted: MutableMap<CorpseType, Long> = enumMapOf()
    ) : BucketedItemTrackerData<CorpseType, SessionUptime.Normal>() {
        override fun getDescription(bucket: CorpseType?, timesGained: Long): List<String> =
            super.getDropRate(corpsesLooted, bucket, timesGained)

        override fun getCoinName(bucket: CorpseType?, item: TrackedItem) = "<no coins>"
        override fun getCoinDescription(bucket: CorpseType?, item: TrackedItem): List<String> = listOf("<no coins>")
        override fun CorpseType.isBucketSelectable() = true
        override fun bucketName(): String = "Corpse"

        fun getCorpseCount(): Long = selectedBucket?.let { corpsesLooted[it] } ?: corpsesLooted.values.sum()
    }

    private fun addLootedCorpse(type: CorpseType) = modify { it.corpsesLooted.addOrPut(type, 1) }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (isEnabled() && event.source == ItemAddManager.Source.COMMAND) {
            event.addItemFromEvent()
        }
    }

    @HandleEvent
    fun onCorpseLooted(event: CorpseLootedEvent) {
        addLootedCorpse(event.corpseType)
        for ((itemName, amount) in event.loot) {
            if (itemName.removeColor().trim() == "Glacite Powder") continue
            NeuInternalName.fromItemNameOrNull(itemName)?.let { item ->
                addItem(event.corpseType, item, amount, command = false, message = false)
            }
        }
    }

    override fun drawDisplayF(data: BucketData): List<Searchable> = buildList {
        addSearchString("§b§lMineshaft Corpse Profit Tracker")
        addBucketSelector(this, data, "Corpse Type")

        if (data.getCorpseCount() == 0L) return@buildList

        var profit = drawItems(data, { true }, this)
        val applicableKeys: List<CorpseType> = data.selectedBucket?.let {
            listOf(it)
        } ?: enumValues<CorpseType>().toList()
            .filter { data.corpsesLooted[it] != null }
        var totalKeyCost = 0.0
        var totalKeyCount = 0
        val keyCostStrings = buildList {
            applicableKeys.forEach { keyData ->
                keyData.key?.let { key ->
                    val keyName = key.repoItemName
                    val price = getPricePer(key)
                    val count = data.corpsesLooted[keyData] ?: 0
                    val totalPrice = price * count
                    if (totalPrice > 0) {
                        profit -= totalPrice
                        totalKeyCost += totalPrice
                        totalKeyCount += count.toInt()
                        add("§7${count}x $keyName§7: §c-${totalPrice.shortFormat()}")
                    }
                }
            }
        }

        if (totalKeyCount > 0) {
            val specificKeyFormat = if (applicableKeys.count() == 1) applicableKeys.first().key!!.repoItemName else "§eCorpse Keys"
            val keyFormat = "§7${totalKeyCount}x $specificKeyFormat§7: §c-${totalKeyCost.shortFormat()}"
            add(
                if (applicableKeys.count() == 1) Renderable.text(keyFormat).toSearchable()
                else Renderable.hoverTips(
                    keyFormat,
                    keyCostStrings,
                ).toSearchable(),
            )
        }

        val duration = data.getTotalUptime()
        addAll(addTotalProfit(profit, data.getCorpseCount(), "corpse", duration, "Corpses"))

        addPriceFromButton(this)
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.MINESHAFT || event.newIsland == IslandType.DWARVEN_MINES) {
            firstUpdate()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetcorpsetracker") {
            description = "Resets the Glacite Mineshaft Corpse Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }

    private fun isEnabled() =
        SkyBlockUtils.inSkyBlock && config.enabled && (
            IslandType.MINESHAFT.isInIsland() ||
                (!config.onlyInMineshaft && MiningApi.inGlacialTunnels())
            )
}
