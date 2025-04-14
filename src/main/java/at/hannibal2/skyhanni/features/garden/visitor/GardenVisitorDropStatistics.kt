package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.visitor.DropsStatisticsConfig.DropsStatisticsTextEntry
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.add
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.addLine
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.seconds

private typealias VisitorDrops = ProfileSpecificStorage.GardenStorage.VisitorDrops

@SkyHanniModule
object GardenVisitorDropStatistics {

    private val patternGroup = RepoPattern.group("garden.visitor.droptracker")
    private val config get() = VisitorApi.config.dropsStatistics
    private val visitorRarityEntries: List<LorenzRarity> = listOf(
        LorenzRarity.UNCOMMON,
        LorenzRarity.RARE,
        LorenzRarity.LEGENDARY,
        LorenzRarity.MYTHIC,
        LorenzRarity.SPECIAL,
    )
    private var display = emptyList<Renderable>()
    private var lastAccept = SimpleTimeMark.farPast()

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: OFFER ACCEPTED with Duke (UNCOMMON)
     */
    private val acceptPattern by patternGroup.pattern(
        "accept",
        "OFFER ACCEPTED with (?<visitor>.*) \\((?<rarity>.*)\\)",
    )

    /**
     * REGEX-TEST: +20 Copper
     */
    private val copperPattern by patternGroup.pattern(
        "copper",
        "[+](?<amount>.*) Copper",
    )

    /**
     * REGEX-TEST: +20 Garden Experience
     */
    private val gardenExpPattern by patternGroup.pattern(
        "gardenexp",
        "[+](?<amount>.*) Garden Experience",
    )

    /**
     * REGEX-TEST: +18.2k Farming XP
     */
    private val farmingExpPattern by patternGroup.pattern(
        "farmingexp",
        "[+](?<amount>.*) Farming XP",
    )

    /**
     * REGEX-TEST: +12 Bits
     */
    private val bitsPattern by patternGroup.pattern(
        "bits",
        "[+](?<amount>.*) Bits",
    )

    /**
     * REGEX-TEST: +968 Mithril Powder
     */
    private val mithrilPowderPattern by patternGroup.pattern(
        "powder.mithril",
        "[+](?<amount>.*) Mithril Powder",
    )

    /**
     * REGEX-TEST: +754 Gemstone Powder
     */
    private val gemstonePowderPattern by patternGroup.pattern(
        "powder.gemstone",
        "[+](?<amount>.*) Gemstone Powder",
    )
    // </editor-fold>

    private val patternStorageAccessorMap: Map<Pattern, (VisitorDrops, Int) -> Unit> = mapOf(
        copperPattern to { storage, amount -> storage.copper += amount },
        farmingExpPattern to { storage, amount -> storage.farmingExp += amount },
        gardenExpPattern to { storage, amount -> storage.gardenExp += amount },
        bitsPattern to { storage, amount -> storage.bits += amount },
        mithrilPowderPattern to { storage, amount -> storage.mithrilPowder += amount },
        gemstonePowderPattern to { storage, amount -> storage.gemstonePowder += amount },
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onVisitorAccepted(event: VisitorAcceptedEvent) {
        lastAccept = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
    }

    @HandleEvent
    fun onVisitorAccept(event: VisitorAcceptEvent) {
        if (!GardenApi.onBarnPlot) return
        if (!ProfileStorageData.loaded) return
        val storage = GardenApi.storage?.visitorDrops ?: return

        for (internalName in event.visitor.allRewards) {
            val reward = VisitorReward.getByInternalName(internalName) ?: continue
            storage.rewardsCount.addOrPut(reward, 1)
            saveAndUpdate()
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!GardenApi.onBarnPlot) return
        if (!ProfileStorageData.loaded) return
        if (lastAccept.passedSince() > 1.seconds) return

        val message = event.message.removeColor().trim()
        val storage = GardenApi.storage?.visitorDrops ?: return

        patternStorageAccessorMap.forEach { (pattern, accessor) ->
            pattern.matchMatcher(message) {
                val amount = group("amount").formatInt()
                accessor.invoke(storage, amount)
            }
        }

        acceptPattern.matchMatcher(message) {
            storage.acceptedVisitors += 1
            val rarity = LorenzRarity.getByName(group("rarity")) ?: return@matchMatcher
            storage.acceptedRarities.addOrPut(rarity, 1)
            saveAndUpdate()
        }
    }

    private val transformMap: Map<DropsStatisticsTextEntry, (VisitorDrops, MutableList<Renderable>) -> Unit> = buildMap {
        VisitorReward.entries.forEach { reward ->
            val textEntryOption = reward.toStatsTextEntryOrNull() ?: return@forEach
            val transformer: (VisitorDrops, MutableList<Renderable>) -> Unit = { storage, list ->
                val count = storage.rewardsCount[reward] ?: 0
                when (config.displayIcons.get()) {
                    true -> {
                        val stack = reward.itemStack
                        val countFormat = "§b${count.addSeparators()}"
                        if (config.displayNumbersFirst.get()) list.addLine {
                            addString(countFormat)
                            addItemStack(stack)
                        } else list.addLine {
                            addItemStack(stack)
                            addString(countFormat)
                        }
                    }
                    false -> list.addString(format(count, reward.displayName, "§b"))
                }
            }
            add(textEntryOption to transformer)
        }

        addAll(
            DropsStatisticsTextEntry.TITLE to { _, list -> list.addString("§e§lVisitor Statistics") },
            DropsStatisticsTextEntry.SPACER_1 to { _, list -> list.addString("") },
            DropsStatisticsTextEntry.SPACER_2 to { _, list -> list.addString("") },

            DropsStatisticsTextEntry.TOTAL_VISITORS to { storage, list ->
                list.addString(format(storage.getTotalVisitors(), "Total", "§e", ""))
            },
            DropsStatisticsTextEntry.ACCEPTED to { storage, list ->
                list.addString(format(storage.acceptedVisitors, "Accepted", "§2", ""))
            },
            DropsStatisticsTextEntry.DENIED to { storage, list ->
                list.addString(format(storage.deniedVisitors, "Denied", "§c", ""))
            },
            DropsStatisticsTextEntry.COPPER to { storage, list ->
                list.addString(format(storage.copper, "Copper", "§c", ""))
            },
            DropsStatisticsTextEntry.FARMING_EXP to { storage, list ->
                list.addString(format(storage.farmingExp, "Farming EXP", "§3", "§7"))
            },
            DropsStatisticsTextEntry.GARDEN_EXP to { storage, list ->
                list.addString(format(storage.gardenExp, "Garden EXP", "§2", "§7"))
            },
            DropsStatisticsTextEntry.COINS_SPENT to { storage, list ->
                list.addString(format(storage.coinsSpent, "Coins Spent", "§6", ""))
            },
            DropsStatisticsTextEntry.BITS to { storage, list ->
                list.addString(format(storage.bits, "Bits", "§b", "§b"))
            },
            DropsStatisticsTextEntry.MITHRIL_POWDER to { storage, list ->
                list.addString(format(storage.mithrilPowder, "Mithril Powder", "§2", "§2"))
            },
            DropsStatisticsTextEntry.GEMSTONE_POWDER to { storage, list ->
                list.addString(format(storage.gemstonePowder, "Gemstone Powder", "§d", "§d"))
            },
            DropsStatisticsTextEntry.VISITORS_BY_RARITY to { storage, list ->
                val visitorRarityLine = visitorRarityEntries.joinToString("§f-") { rarity ->
                    val count = storage.acceptedRarities[rarity] ?: 0
                    "${rarity.chatColorCode}${count.addSeparators()}"
                }
                list.addString(visitorRarityLine)
            },
        )
    }

    private fun drawDisplay(storage: ProfileSpecificStorage.GardenStorage.VisitorDrops) = buildList {
        for (enabledOption in config.textFormat.get()) {
            val transformer = transformMap[enabledOption] ?: continue
            transformer(storage, this)
        }
    }

    fun format(amount: Number, name: String, color: String, amountColor: String = color) =
        if (config.displayNumbersFirst.get())
            "$color${format(amount)} $name"
        else
            "$color$name: $amountColor${format(amount)}"

    fun format(amount: Number): String {
        if (amount is Int) return amount.addSeparators()
        if (amount is Long) return amount.shortFormat()
        return "$amount"
    }

    fun saveAndUpdate() {
        if (!GardenApi.inGarden()) return
        val storage = GardenApi.storage?.visitorDrops ?: return
        display = drawDisplay(storage)
    }

    fun resetCommand() {
        val storage = GardenApi.storage?.visitorDrops ?: return
        ChatUtils.clickableChat(
            "Click here to reset Visitor Drops Statistics.",
            // Todo: Make the storage class extend `ResettableStorageSet`, so this can just be a .reset() call
            //  This should happen at the same time as the tracker migration - see #profile.garden.visitorDrops
            onClick = {
                storage.copper = 0
                storage.bits = 0
                storage.farmingExp = 0
                storage.gardenExp = 0
                storage.gemstonePowder = 0
                storage.mithrilPowder = 0
                storage.acceptedRarities = mutableMapOf()
                storage.rewardsCount = mutableMapOf()
                ChatUtils.chat("Visitor Drop Statistics reset!")
                saveAndUpdate()
            },
            "§eClick to reset!",
        )
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        saveAndUpdate()
        ConditionalUtils.onToggle(
            config.enabled,
            config.textFormat,
            config.displayNumbersFirst,
            config.displayIcons
        ) {
            saveAndUpdate()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled.get()) return
        if (GardenApi.hideExtraGuis()) return
        if (config.onlyOnBarn.get() && !GardenApi.onBarnPlot) return
        config.pos.renderRenderables(display, posLabel = "Visitor Stats")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val originalPrefix = "garden.visitorDropsStatistics."
        val newPrefix = "garden.visitors.dropsStatistics."
        event.move(3, "${originalPrefix}enabled", "${newPrefix}enabled")
        event.move(3, "${originalPrefix}textFormat", "${newPrefix}textFormat")
        event.move(3, "${originalPrefix}displayNumbersFirst", "${newPrefix}displayNumbersFirst")
        event.move(3, "${originalPrefix}displayIcons", "${newPrefix}displayIcons")
        event.move(3, "${originalPrefix}onlyOnBarn", "${newPrefix}onlyOnBarn")
        event.move(3, "${originalPrefix}visitorDropPos", "${newPrefix}pos")

        event.transform(11, "${newPrefix}textFormat") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, DropsStatisticsTextEntry::class.java)
        }

        // Was a list of longs, now a map of rarity to count
        event.move(
            85,
            "#profile.garden.visitorDrops.visitorRarities",
            "#profile.garden.visitorDrops.acceptedRarities",
        ) { element ->
            val list = element.asJsonArray.map { it.asLong }.toMutableList()

            // Adding the mythic rarity between legendary and special, if missing
            if (list.size == 4) {
                val special = list.last()
                list[3] = 0L
                list.add(special)
            }

            val map = mutableMapOf<LorenzRarity, Long>()
            for ((index, rarity) in visitorRarityEntries.withIndex()) {
                map[rarity] = list[index]
            }

            ConfigManager.gson.toJsonTree(map, MutableMap::class.java)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetvisitordrops") {
            description = "Resets the Visitors Drop Statistics"
            category = CommandCategory.USERS_RESET
            callback { resetCommand() }
        }
    }
}
