package at.hannibal2.skyhanni.features.combat.end.endernodetracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.combat.end.EnderNodeConfig.EnderNodeDisplayEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemCategory.Companion.containsItem
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.add
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object EnderNodeTracker {

    private val config get() = SkyHanniMod.feature.combat.endIsland.enderNodeTracker

    private var miteGelInInventory = 0

    private val patternGroup = RepoPattern.group("combat.endernodetracker.chat")

    /**
     * REGEX-TEST: §5§lENDER NODE! §r§fYou found §r§8§r§aEnchanted Obsidian§r§f!
     */
    private val patternOne by patternGroup.pattern(
        "one",
        "§5§lENDER NODE! §r§fYou found §r(?:§8§r)?(?<name>.*)§r§f!",
    )

    /**
     * REGEX-TEST: §5§lENDER NODE! §r§fYou found §r§85x §r§aEnchanted Ender Pearl§r§f!
     */
    private val patternMulti by patternGroup.pattern(
        "multi",
        "§5§lENDER NODE! §r§fYou found §r§8(?<amount>\\d+)x §r(?<name>.*)§r§f!",
    )

    // TODO use repo patterns
    // TODO add abstract logic with ohter pet drop chat messages
    private val endermanRegex = Regex("""(RARE|PET) DROP! §r(.+) §r§b\(""")

    private val tracker = SkyHanniTracker("Ender Node Tracker", { Data() }, { it.enderNodeTracker }) {
        drawDisplay(it)
    }

    class Data : TrackerData() {

        override fun reset() {
            totalNodesMined = 0
            totalEndermiteNests = 0
            lootCount.clear()
        }

        @Expose
        var totalNodesMined = 0

        @Expose
        var totalEndermiteNests = 0

        @Expose
        var lootCount: MutableMap<EnderNode, Int> = mutableMapOf()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (!ProfileStorageData.loaded) return

        // don't call removeColor because we want to distinguish enderman pet rarity
        val message = event.message.trim()
        var item: String? = null
        var amount = 1

        patternMulti.matchMatcher(message) {
            item = group("name")
            amount = group("amount").toInt()
            addOneNodeMined()
        } ?: patternOne.matchMatcher(message) {
            item = group("name")
            amount = 1
            addOneNodeMined()
        }

        endermanRegex.find(message)?.let {
            amount = 1
            item = it.groups[2]?.value
        }

        when (item) {
            null -> return
            "§cEndermite Nest" -> {
                tracker.modify { storage ->
                    storage.totalEndermiteNests++
                }
            }
        }

        // increment the count of the specific item found
        EnderNode.entries.find { it.displayName == item }?.let {
            tracker.modify { storage ->
                storage.lootCount.addOrPut(it, amount)
            }
        }
    }

    private fun addOneNodeMined() {
        tracker.modify { storage ->
            storage.totalNodesMined++
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!isEnabled()) return
        miteGelInInventory = InventoryUtils.getItemsInOwnInventory().filter {
            it.getInternalNameOrNull() == EnderNode.MITE_GEL.internalName
        }.sumOf { it.stackSize }
    }

    @HandleEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled()) return
        if (!ProfileStorageData.loaded) return

        val change = event.sackChanges.firstOrNull {
            it.internalName == EnderNode.MITE_GEL.internalName && it.delta > 0
        } ?: return

        tracker.modify { storage ->
            storage.lootCount.addOrPut(EnderNode.MITE_GEL, change.delta)
        }
    }

    @HandleEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!isEnabled()) return
        if (!ProfileStorageData.loaded) return

        val newMiteGelInInventory = InventoryUtils.getItemsInOwnInventory().filter {
            it.getInternalNameOrNull() == EnderNode.MITE_GEL.internalName
        }.sumOf { it.stackSize }

        val change = newMiteGelInInventory - miteGelInInventory
        if (change > 0) {
            tracker.modify { storage ->
                storage.lootCount.addOrPut(EnderNode.MITE_GEL, change)
            }
        }
        miteGelInInventory = newMiteGelInInventory
    }

    init {
        tracker.initRenderer({ config.position }) { config.enabled && isEnabled() }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.textFormat.afterChange {
            tracker.update()
        }
        tracker.update()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.enderNodeTracker", "combat.enderNodeTracker")
        event.transform(11, "combat.enderNodeTracker.textFormat") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, EnderNodeDisplayEntry::class.java)
        }
        event.move(77, "combat.enderNodeTracker", "combat.endIsland.enderNodeTracker")
    }

    private fun getLootProfit(storage: Data): Map<EnderNode, Double> {
        if (!ProfileStorageData.loaded) return emptyMap()

        val newProfit = mutableMapOf<EnderNode, Double>()
        for ((item, amount) in storage.lootCount) {
            val altPrice = (if (!SkyBlockUtils.noTradeMode) SkyHanniTracker.getPricePer(item.internalName) else 0.0)
            val price = when (item.isEnderArmor()) {
                true -> 10_000.0
                false -> altPrice.coerceAtLeast(
                    item.internalName.getNpcPrice(),
                ).coerceAtLeast(item.getGeorgePrice())
            }
            newProfit[item] = price * amount
        }
        return newProfit
    }

    private fun isEnabled() = IslandType.THE_END.isCurrent() && (!config.onlyPickaxe || hasItemInHand())

    private fun hasItemInHand() = ItemCategory.miningTools.containsItem(InventoryUtils.getItemInHand())

    private fun EnderNode.isEnderArmor() = this in EnderNode.armorEntries

    private fun EnderNode.getGeorgePrice(): Double = when (this) {
        EnderNode.COMMON_ENDERMAN_PET -> 100.0
        EnderNode.UNCOMMON_ENDERMAN_PET -> 500.0
        EnderNode.RARE_ENDERMAN_PET -> 2_000.0
        EnderNode.EPIC_ENDERMAN_PET -> 10_000.0
        EnderNode.LEGENDARY_ENDERMAN_PET -> 1_000_000.0
        else -> 0.0
    }

    private val transformMap: Map<EnderNodeDisplayEntry, (Data, MutableList<Searchable>, EnderNode?) -> Unit> = buildMap {
        addAll(
            EnderNodeDisplayEntry.TITLE to { _, list, _ -> list.addSearchString("§5§lEnder Node Tracker") },
            EnderNodeDisplayEntry.NODES_MINED to { data, list, _ ->
                list.addSearchString("§d${data.totalNodesMined.addSeparators()} Ender Nodes mined")
            },
            EnderNodeDisplayEntry.COINS_MADE to { data, list, _ ->
                list.addSearchString("§6${getLootProfit(data).values.sum().shortFormat()} Coins made")
            },
            EnderNodeDisplayEntry.ENDERMITE_NEST to { data, list, _ ->
                list.addSearchString("§b${data.totalEndermiteNests.addSeparators()} §cEndermite Nest", "Endermite Nest")
            },
            EnderNodeDisplayEntry.ENDER_ARMOR to { data, list, _ ->
                val totalEnderArmor = data.lootCount.filterKeys { it.isEnderArmor() }.sumAllValues()
                list.addSearchString(
                    "§b${totalEnderArmor.addSeparators()} §5Ender Armor " + "§7(§6${(totalEnderArmor * 10_000).shortFormat()}§7)",
                )
            },
            EnderNodeDisplayEntry.ENDERMAN_PET to { data, list, _ ->
                val lootProfit = getLootProfit(data)
                val (c, u, r, e, l) = EnderNode.petEntries.map { (data.lootCount[it] ?: 0).addSeparators() }
                val profit = EnderNode.petEntries.sumOf { lootProfit[it] ?: 0.0 }.shortFormat()
                list.addSearchString("§f$c§7-§a$u§7-§9$r§7-§5$e§7-§6$l §fEnderman Pet §7(§6$profit§7)")
            },

            EnderNodeDisplayEntry.SPACER_1 to { _, list, _ -> list.addSearchString(" ") },
            EnderNodeDisplayEntry.SPACER_2 to { _, list, _ -> list.addSearchString(" ") },
        )

        addFromNodeEntries(EnderNode.miscEntries + EnderNode.armorEntries) { data, list, nodeItem ->
            if (nodeItem == null) return@addFromNodeEntries
            val lootProfit = getLootProfit(data)
            val count = (data.lootCount[nodeItem] ?: 0).addSeparators()
            val profit = (lootProfit[nodeItem] ?: 0.0).shortFormat()
            list.addSearchString("§b$count ${nodeItem.displayName} §7(§6$profit§7)")
        }
    }

    private fun MutableMap<EnderNodeDisplayEntry, (Data, MutableList<Searchable>, EnderNode?) -> Unit>.addFromNodeEntries(
        entries: List<EnderNode>,
        invoker: (Data, MutableList<Searchable>, EnderNode?) -> Unit,
    ) = entries.forEach { node ->
        val configItem = node.toEnderNodeDisplayEntryOrNull() ?: return@forEach
        add(configItem to invoker)
    }

    private fun drawDisplay(data: Data) = buildList {
        for (enabledOption in config.textFormat.get()) {
            val transformer = transformMap[enabledOption] ?: continue
            val nodeItem = enabledOption.toEnderNodeOrNull()
            transformer(data, this, nodeItem)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetendernodetracker") {
            description = "Resets the Ender Node Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }
}
