package at.hannibal2.skyhanni.features.combat.carrytracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.combat.CrimsonMiniBossEvent
import at.hannibal2.skyhanni.events.combat.OtherPlayersSlayerEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.kuudra.KuudraCompleteEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraTier
import at.hannibal2.skyhanni.features.nether.miniboss.CrimsonMiniBoss
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrUserError
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.width
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.onHover
import at.hannibal2.skyhanni.utils.chat.TextHelper.width
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.suggest
import at.hannibal2.skyhanni.utils.inPartialHours
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CarryTracker {

    private val PRICE_LIST_MESSAGE_ID = ChatUtils.getUniqueMessageId()

    private val config get() = SkyHanniMod.feature.combat.carryTracker
    private val priceStorage get() = SkyHanniMod.feature.storage.carryTrackerPrices
    private var display: List<Renderable> = emptyList()

    private val customers: MutableList<Customer> = mutableListOf()

    // TODO implement full trade detection; for now this will do
    private var lastTradedPlayer: String? = null
    private val recentTrades: MutableMap<String, Double> = mutableMapOf()

    private val patternGroup = RepoPattern.group("carrytracker")

    /**
     * REGEX-TEST: Trade completed with [MVP+] zumbiepig!
     */
    private val tradeCompletedPattern by patternGroup.pattern(
        "trade.completed",
        "Trade completed with (?<name>.+)!",
    )

    /**
     * WRAPPED-REGEX-TEST: " + 500k coins"
     */
    private val tradeCoinsGainedPattern by patternGroup.pattern(
        "trade.coins.gained",
        " \\+ (?<coins>.+) coins",
    )

    /**
     * WRAPPED-REGEX-TEST: " - 500k coins"
     */
    private val tradeCoinsLostPattern by patternGroup.pattern(
        "trade.coins.lost",
        " - (?<coins>.+) coins",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        tradeCompletedPattern.matchMatcher(event.cleanMessage) {
            val name = group("name").cleanPlayerName()

            recentTrades.remove(name) // clear old trades with player
            lastTradedPlayer = name

            DelayedRun.runNextTick {
                lastTradedPlayer = null

                val coins = recentTrades[name] ?: return@runNextTick
                if (coins <= 0.0) return@runNextTick

                val customer = findCustomer(name)
                if (customer != null) {
                    customer.coinsPaid += coins
                    updateDisplay()
                }

                if (!config.suggestCarriesFromTrades) return@runNextTick
                val types = carryTypes.filter { it.pricePer != 0.0 && (coins % it.pricePer) == 0.0 }

                if (types.isNotEmpty()) ChatUtils.chat {
                    append("Click to add carries for:")

                    val chatWidth = Minecraft.getInstance().gui.chat.width
                    val prefixWidth = "[SkyHanni] ".width()
                    val spaceWidth = " ".width()

                    for (type in types) {
                        val amount = (coins / type.pricePer).toInt()
                        val component = componentBuilder {
                            append(" ")
                            append("§b[${amount}x ${type.shortName}]") {
                                onHover("§eClick to add ${amount}x §d${type.displayName}§e!")
                                onClick { addCarry(name, type.id, amount) }
                            }
                        }

                        // dont break component into 2 lines
                        val remainingWidth = chatWidth - ((prefixWidth + this.width()) % chatWidth)
                        val appendWidth = component.width()
                        // this is (remainingWidth < appendWidth < chatWidth) refactored by intellij
                        if (appendWidth in (remainingWidth + 1)..<chatWidth)
                            append(" ".repeat(remainingWidth / spaceWidth))

                        append(component)
                    }
                }
            }
        }

        tradeCoinsGainedPattern.matchMatcher(event.cleanMessage) {
            val name = lastTradedPlayer ?: return
            val coins = group("coins").formatDouble()

            recentTrades.addOrPut(name, coins)
        }

        tradeCoinsLostPattern.matchMatcher(event.cleanMessage) {
            val name = lastTradedPlayer ?: return
            val coins = group("coins").formatDouble()

            recentTrades.addOrPut(name, -coins)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        CarryTrackerCommand.registerCarryCommand(event)
    }

    @HandleEvent
    fun onSecondPassed() {
        updateDisplay()
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Carry Tracker")
        event.addIrrelevant {
            add("customers: $customers")
            add("carry types: $carryTypes")
            add("saved prices: $priceStorage")
            add("recent trades: $recentTrades")
        }
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = display::isNotEmpty,
            onRender = {
                config.position.renderRenderables(display, posLabel = "Carry Tracker")
            },
        )
    }

    private fun checkCustomerTrades(customer: Customer) {
        val trade = recentTrades.entries.firstOrNull { it.key.equals(customer.name, true) } ?: return
        customer.coinsPaid += trade.value
        recentTrades.remove(trade.key)
    }

    fun addCarry(customerName: String, rawType: String, amount: Int) {
        val type = findCarryType(rawType) ?: run {
            ChatUtils.userError("Unknown carry type: '$rawType'")
            return
        }

        val customer = findCustomer(customerName) ?: Customer(customerName).also {
            customers.add(it)
            checkCustomerTrades(it)
        }

        val carry = customer.findCarry(type)
        if (carry != null) {
            val newTotal = carry.total + amount
            if (newTotal <= 0) {
                ChatUtils.userError("Total carries must be positive!")
                return
            }

            carry.total = newTotal
            updateDisplay()
            ChatUtils.chat("Carry updated: §b${customer.name} ${carry.formatProgress()} §d${carry.type.displayName}")
        } else {
            if (amount <= 0) {
                ChatUtils.userError("Total carries must be positive!")
                return
            }

            val newCarry = Carry(type, amount).also {
                customer.carries.add(it)
            }

            updateDisplay()
            ChatUtils.chat("Carry added: §b${customer.name} ${newCarry.formatProgress()} §d${newCarry.type.displayName}")
        }
    }

    fun removeCustomer(customerName: String) {
        val customer = findCustomer(customerName) ?: run {
            ChatUtils.userError("Customer not found: §b$customerName")
            return
        }

        customers.remove(customer)
        updateDisplay()
        ChatUtils.chat("Customer removed: §b${customer.name}")
    }

    fun removeCarry(customerName: String, rawType: String) {
        val customer = findCustomer(customerName) ?: run {
            ChatUtils.userError("Customer not found: §b$customerName")
            return
        }
        val type = findCarryType(rawType) ?: run {
            ChatUtils.userError("Unknown carry type: '$rawType'")
            return
        }
        val carry = customer.findCarry(type) ?: run {
            ChatUtils.userError("Carry not found: §b${customer.name} §d${type.displayName}")
            return
        }

        customer.carries.remove(carry)
        if (customer.carries.isEmpty()) customers.remove(customer)
        updateDisplay()
        ChatUtils.chat("Carry removed: §b${customer.name} §d${carry.type.displayName}")
    }

    fun updateTotal(customerName: String, rawType: String, amount: Int) {
        val customer = findCustomer(customerName) ?: run {
            ChatUtils.userError("Customer not found: §b$customerName")
            return
        }
        val type = findCarryType(rawType) ?: run {
            ChatUtils.userError("Unknown carry type: '$rawType'")
            return
        }
        val carry = customer.findCarry(type) ?: run {
            ChatUtils.userError("Carry not found: §b${customer.name} §d${type.displayName}")
            return
        }

        val newTotal = carry.total + amount
        if (newTotal <= 0) {
            ChatUtils.userError("Total carries must be positive!")
            return
        }

        carry.total = newTotal
        updateDisplay()
        ChatUtils.chat("Carry updated: §b${customer.name} ${carry.formatProgress()} §d${carry.type.displayName}")
    }

    fun updateDone(customerName: String, rawType: String, amount: Int) {
        val customer = findCustomer(customerName) ?: run {
            ChatUtils.userError("Customer not found: §b$customerName")
            return
        }
        val type = findCarryType(rawType) ?: run {
            ChatUtils.userError("Unknown carry type: '$rawType'")
            return
        }
        val carry = customer.findCarry(type) ?: run {
            ChatUtils.userError("Carry not found: §b${customer.name} §d${type.displayName}")
            return
        }

        val newDone = carry.done + amount
        if (newDone < 0) {
            ChatUtils.userError("Carries done cannot be negative!")
            return
        }

        carry.done = newDone
        updateDisplay()
        ChatUtils.chat("Carry updated: §b${customer.name} ${carry.formatProgress()} §d${carry.type.displayName}")
    }

    fun updatePaid(customerName: String, rawCoins: String) {
        val customer = findCustomer(customerName) ?: run {
            ChatUtils.userError("Customer not found: §b$customerName")
            return
        }
        val coins = rawCoins.formatDoubleOrUserError() ?: return

        customer.coinsPaid += coins
        updateDisplay()

        val paid = customer.coinsPaid.shortFormat()
        val total = customer.getTotalCost().shortFormat()
        ChatUtils.chat("Customer updated: §b${customer.name} §6$paid§8/§6$total coins §epaid")
    }

    fun listPrices(page: Int = 1) {
        TextHelper.displayPaginatedList(
            "Carry Tracker Prices",
            carryTypes.filter { it.pricePer != 0.0 },
            PRICE_LIST_MESSAGE_ID,
            "No carry prices set.",
            page,
        ) { type ->
            val name = type.displayName
            val price = type.pricePer.shortFormat()
            componentBuilder {
                append("§8[§c✖§8]") {
                    onHover("§eClick to delete price!")
                    onClick {
                        deletePrice(type.id)
                        listPrices()
                    }
                }
                append(" ")
                append("§8[§e✎§8]") {
                    onHover("§eClick to edit price!")
                    suggest = "/shcarry price set ${type.id} ${price.lowercase()}"
                }
                append(" §d$name§8: §6$price coins")
            }
        }
    }

    fun setPrice(rawType: String, rawPrice: String) {
        val type = findCarryType(rawType) ?: run {
            ChatUtils.userError("Unknown carry type: '$rawType'")
            return
        }
        val price = rawPrice.formatDoubleOrUserError() ?: return

        if (price == 0.0) deletePrice(rawType)
        else if (price < 0.0) ChatUtils.userError("Carry price must be positive!")
        else {
            type.pricePer = price
            updateDisplay()
            ChatUtils.chat("Set carry price for §d${type.displayName} §eto §6${price.shortFormat()} coins")
        }
    }

    fun deletePrice(rawType: String) {
        val type = findCarryType(rawType) ?: run {
            ChatUtils.userError("Unknown carry type: '$rawType'")
            return
        }

        type.pricePer = 0.0
        updateDisplay()
        ChatUtils.chat("Deleted carry price for §d${type.displayName}")
    }

    fun deleteAllPrices() {
        for (type in carryTypes) type.pricePer = 0.0
        updateDisplay()
        ChatUtils.chat("Deleted all carry prices")
    }

    fun clearAll() {
        val customerSize = customers.size
        customers.clear()
        updateDisplay()
        ChatUtils.chat("Removed §b$customerSize §ecustomers")
    }

    private fun countCarry(customer: Customer, carry: Carry) {
        carry.done++
        customer.stats.done()

        if (config.sendProgressToParty) HypixelCommands.partyChat("${customer.name}: ${carry.done}/${carry.total}")
        if (carry.done >= carry.total) {
            val message = "Carry finished: §b${customer.name} ${carry.formatProgress()} §b${carry.type.displayName}"
            if (config.autoRemoveFinishedCarries) {
                customer.carries.remove(carry)
                if (customer.carries.isEmpty()) customers.remove(customer)

                if (config.carryFinishedNotification.chat) ChatUtils.chat("$message §7(automatically removed)")
            } else {
                if (config.carryFinishedNotification.chat) ChatUtils.clickableChat(
                    "$message\n§e[CLICK to remove this carry]",
                    onClick = { removeCarry(customer.name, carry.type.id) },
                    hover = "§eClick to remove this carry!",
                )
            }
            if (config.carryFinishedNotification.title)
                TitleManager.sendTitle(
                    "§eCarry finished for §b${customer.name}§e!",
                    duration = 3.seconds,
                )
            if (config.carryFinishedNotification.sound)
                SoundUtils.playBeepSound()
        }

        updateDisplay()
    }

    private fun updateDisplay() {
        display = buildList {
            if (customers.isEmpty()) return@buildList
            addString("§c§lCarries")

            for (customer in customers) {
                val totalCost = customer.getTotalCost()
                val totalCostFormat = totalCost.shortFormat()
                val paidFormat = customer.coinsPaid.shortFormat()
                val missingFormat = (totalCost - customer.coinsPaid).shortFormat()

                val totalDone = customer.stats.totalDone
                val sinceStart = customer.stats.sinceStart().format(showMilliSeconds = false)
                val sinceLast = customer.stats.sinceLast().format(showMilliSeconds = false)
                val perHour = customer.stats.perHour().roundToInt()
                val averageTime = customer.stats.averageTime().format(showMilliSeconds = false)
                add(
                    Renderable.clickable(
                        "§b${customer.name} §6$paidFormat§8/§6$totalCostFormat §8(§7$sinceLast §8| " +
                            (if (totalDone == 0) "§7N/A §8| §7N/A" else "§7$perHour/h §8| §7$averageTime avg") + "§8)",
                        tips = buildList {
                            add("§7Carries for §b${customer.name}")
                            add("")
                            add("§7Total cost: §6$totalCostFormat")
                            add("§7Already paid: §6$paidFormat")
                            add("§7Still missing: §6$missingFormat")
                            add("")
                            add("§7Total carries done: §e$totalDone")
                            add("§7Total elapsed time: §e$sinceStart")
                            add("§7Carries per hour: §e$perHour")
                            add("§7Average time per carry: §e$averageTime")
                            add("§7Elapsed time since last carry: §e$sinceLast")
                            add("")
                            add("§eClick to send missing coins in party chat!")
                            add("§e${KeyboardManager.getModifierKeyName()}-click to remove this customer!")
                        },
                        onLeftClick = {
                            if (KeyboardManager.isModifierKeyDown()) removeCustomer(customer.name)
                            else HypixelCommands.partyChat("${customer.name}: $paidFormat/$totalCostFormat coins paid")
                        },
                    ),
                )

                for (carry in customer.carries) {
                    val carryTotalCost = carry.getCost().shortFormat()
                    add(
                        Renderable.clickable(
                            "  §d${carry.type.displayName} ${carry.formatProgress()} §6$carryTotalCost",
                            tips = buildList {
                                add("§b${customer.name} §d${carry.type.displayName} §cCarry")
                                add("")
                                add("§7Total: §e${carry.total}")
                                add("§7Done: §e${carry.done}")
                                add("§7Missing: §e${carry.total - carry.done}")
                                add("")
                                add("§7Total cost: §6$carryTotalCost")
                                add("§7Cost per carry: §6${carry.type.pricePer.shortFormat()}")
                                if (carry.type.pricePer == 0.0) {
                                    add("")
                                    add("§cNo price set for this carry!")
                                    add("§cSet a price with /shcarry price set ${carry.type.id} <price>")
                                }
                                add("")
                                add("§eClick to send current progress in the party chat!")
                                add("§e${KeyboardManager.getModifierKeyName()}-click to remove this carry!")
                            },
                            onLeftClick = {
                                if (KeyboardManager.isModifierKeyDown()) removeCarry(customer.name, carry.type.id)
                                else HypixelCommands.partyChat("${customer.name} ${carry.type.displayName}: ${carry.done}/${carry.total}")
                            },
                        ),
                    )
                }
            }
        }
    }

    private data class Customer(
        val name: String,
        var coinsPaid: Double = 0.0,
        val carries: MutableList<Carry> = mutableListOf(),
        val stats: CustomerStats = CustomerStats(),
    ) {
        fun findCarry(type: CarryType): Carry? = carries.firstOrNull { it.type == type }
        fun getTotalCost(): Double = carries.sumOf { it.getCost() }
    }

    private data class Carry(val type: CarryType, var total: Int, var done: Int = 0) {
        fun getCost(): Double = type.pricePer * total
        fun formatProgress(): String {
            val color = when {
                done > total -> 'c'
                done == total -> 'a'
                else -> 'e'
            }
            return "§$color$done§8/§$color$total"
        }
    }

    private data class CustomerStats(
        private val stopwatch: Stopwatch = Stopwatch(paused = false),
        var totalDone: Int = 0,
    ) {
        fun done() {
            totalDone++
            stopwatch.lap()
        }

        fun sinceStart(): Duration = stopwatch.getDuration()
        fun sinceLast(): Duration = stopwatch.getLapTime() ?: Duration.ZERO
        fun elapsed(): Duration = sinceStart() - sinceLast()

        fun perHour(): Double = totalDone / (elapsed().inPartialHours.takeUnless { it == 0.0 } ?: 1.0)
        fun averageTime(): Duration = elapsed() / totalDone.coerceAtLeast(1)
    }

    private abstract class CarryType {
        abstract val id: String
        abstract val displayName: String
        abstract val shortName: String

        var pricePer: Double = 0.0
            get() = priceStorage.getOrDefault(id, field)
            set(value) {
                field = value
                if (value == 0.0) priceStorage.remove(id)
                else priceStorage.put(id, value)
            }
    }

    private data class SlayerCarryType(
        override val id: String,
        override val shortName: String,
        val slayerType: SlayerType,
        val slayerTier: Int,
        override val displayName: String = "${slayerType.displayName} $slayerTier",
    ) : CarryType()

    private data class DungeonCarryType(
        override val id: String,
        override val displayName: String,
        val dungeonFloor: String,
        override val shortName: String = dungeonFloor,
    ) : CarryType()

    private data class KuudraCarryType(
        override val id: String,
        val kuudraTier: KuudraTier,
        override val displayName: String = "${kuudraTier.displayName} Kuudra",
        override val shortName: String = displayName,
    ) : CarryType()

    private data class CrimsonMinibossCarryType(
        override val id: String,
        val crimsonMiniBoss: CrimsonMiniBoss,
        override val displayName: String = crimsonMiniBoss.displayName,
        override val shortName: String = displayName,
    ) : CarryType()

    private val carryTypes = buildList {
        for (i in 1..5) add(SlayerCarryType("rev$i", "Rev $i", SlayerType.REVENANT, i))
        for (i in 1..5) add(SlayerCarryType("tara$i", "Tara $i", SlayerType.TARANTULA, i))
        for (i in 1..4) add(SlayerCarryType("sven$i", "Sven $i", SlayerType.SVEN, i))
        for (i in 1..4) add(SlayerCarryType("eman$i", "Eman $i", SlayerType.VOID, i))
        for (i in 1..4) add(SlayerCarryType("blaze$i", "Blaze $i", SlayerType.INFERNO, i))
        for (i in 1..5) add(SlayerCarryType("vamp$i", "Vamp $i", SlayerType.VAMPIRE, i))

        add(DungeonCarryType("f0", "Entrance Floor", "E", shortName = "F0"))
        for (i in 1..7) add(DungeonCarryType("f$i", "Floor $i", "F$i"))
        for (i in 1..7) add(DungeonCarryType("m$i", "Master Mode $i", "M$i"))

        for (kuudraTier in KuudraTier.entries) add(KuudraCarryType("k${kuudraTier.tierNumber}", kuudraTier))

        add(CrimsonMinibossCarryType("bladesoul", CrimsonMiniBoss.BLADESOUL))
        add(CrimsonMinibossCarryType("mage_outlaw", CrimsonMiniBoss.MAGE_OUTLAW))
        add(CrimsonMinibossCarryType("barb_duke", CrimsonMiniBoss.BARBARIAN_DUKE_X, shortName = "Barb Duke"))
        add(CrimsonMinibossCarryType("ashfang", CrimsonMiniBoss.ASHFANG))
        add(CrimsonMinibossCarryType("magma_boss", CrimsonMiniBoss.MAGMA_CUBE))
    }

    @HandleEvent
    fun onOtherPlayersSlayerSpawn(event: OtherPlayersSlayerEvent.Spawn) {
        val type = findCarryType { it is SlayerCarryType && it.slayerType == event.slayerType && it.slayerTier == event.tier } ?: return
        val customer = findCustomer(event.owner) ?: return
        val carry = customer.findCarry(type) ?: return

        if (config.slayerSpawnedNotification.chat) ChatUtils.chat("§d${carry.type.displayName} §espawned for §b${customer.name}")
        if (config.slayerSpawnedNotification.title) TitleManager.sendTitle("§eBoss spawned for §b${customer.name}§e!", duration = 3.seconds)
        if (config.slayerSpawnedNotification.sound) SoundUtils.playPlingSound()
    }

    @HandleEvent
    fun onOtherPlayersSlayerDeath(event: OtherPlayersSlayerEvent.Death) {
        val type = findCarryType { it is SlayerCarryType && it.slayerType == event.slayerType && it.slayerTier == event.tier } ?: return
        val customer = findCustomer(event.owner) ?: return
        val carry = customer.findCarry(type) ?: return

        countCarry(customer, carry)
    }

    @HandleEvent
    fun onDungeonComplete(event: DungeonCompleteEvent) {
        val type = findCarryType { it is DungeonCarryType && it.dungeonFloor == event.floor } ?: return

        for (name in DungeonApi.getPlayerNames()) {
            val customer = findCustomer(name) ?: continue
            val carry = customer.findCarry(type) ?: continue

            // wait for other server messages first
            DelayedRun.runNextTick { countCarry(customer, carry) }
        }
    }

    @HandleEvent
    fun onKuudraComplete(event: KuudraCompleteEvent) {
        val type = findCarryType { it is KuudraCarryType && it.kuudraTier == event.kuudraTier } ?: return

        for (name in EntityUtils.getPlayerEntities().map { it.name.string }) {
            val customer = findCustomer(name) ?: continue
            val carry = customer.findCarry(type) ?: continue

            // wait for other server messages first
            DelayedRun.runNextTick { countCarry(customer, carry) }
        }
    }

    @HandleEvent
    fun onCrimsonMinibossDeath(event: CrimsonMiniBossEvent.Death) {
        val type = findCarryType { it is CrimsonMinibossCarryType && it.crimsonMiniBoss == event.miniBoss } ?: return

        for (name in EntityUtils.getPlayerEntities().map { it.name.string }) {
            val customer = findCustomer(name) ?: continue
            val carry = customer.findCarry(type) ?: continue

            // wait for other server messages first
            DelayedRun.runNextTick { countCarry(customer, carry) }
        }
    }

    private fun findCustomer(name: String): Customer? = customers.firstOrNull { it.name.equals(name, ignoreCase = true) }
    private fun findCarryType(id: String): CarryType? = findCarryType { it.id.equals(id, ignoreCase = true) }
    private fun findCarryType(predicate: (CarryType) -> Boolean): CarryType? = carryTypes.firstOrNull(predicate)

    fun isCustomer(customerName: String): Boolean = findCustomer(customerName) != null
    fun getCustomerNames(): List<String> = customers.map { it.name }
    fun getCarryTypeIds(): List<String> = carryTypes.map { it.id }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(139, "misc.carryPosition", "combat.carryTracker.position")
    }
}
