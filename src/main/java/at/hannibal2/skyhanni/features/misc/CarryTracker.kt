package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.PlayerSuggestions
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.combat.CrimsonMinibossKilledEvent
import at.hannibal2.skyhanni.events.combat.OtherPlayersSlayerEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.kuudra.KuudraCompleteEvent
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraTier
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrUserError
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeUnusedDecimal
import at.hannibal2.skyhanni.utils.StringUtils.width
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.onHover
import at.hannibal2.skyhanni.utils.chat.TextHelper.width
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.InventoryGuiScaleCompat
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
import at.hannibal2.skyhanni.features.nether.CrimsonMinibossRespawnTimer.MiniBoss as CrimsonMiniBoss

@Suppress("LargeClass")
@SkyHanniModule
object CarryTracker {
    private val PRICE_LIST_MESSAGE_ID = ChatUtils.getUniqueMessageId()

    private val config get() = SkyHanniMod.feature.combat.carryTracker
    private val storage get() = config.storage
    private var display: List<Renderable> = emptyList()

    private val customers: MutableList<Customer> = mutableListOf()

    // TODO implement full trade detection; for now this will do
    private var lastTradedPlayer: String? = null
    private val recentTrades: MutableMap<String, Double> = mutableMapOf()

    private val patternGroup = RepoPattern.group("carrytracker")

    /**
     * REGEX-TEST: Trade completed with [MVP+] ClachersHD!
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
                val coins = recentTrades[name] ?: return@runNextTick
                if (coins <= 0.0) return@runNextTick

                findCustomer(name)?.let { customer ->
                    customer.coinsPaid += coins
                    updateDisplay()
                }

                val types = carryTypes.filter { it.pricePer != 0.0 && (coins % it.pricePer) == 0.0 }

                if (types.isNotEmpty()) ChatUtils.chat {
                    append("Click to add carries for:")

                    val chatWidth = Minecraft.getInstance().gui.chat.width
                    val prefixWidth = "[SkyHanni] ".width()
                    val spaceWidth = " ".width()

                    for (type in types) {
                        val amount = (coins / type.pricePer).toInt()
                        val component = " ".asComponent().append(
                            "§b[${amount}x ${type.shortName}]".asComponent {
                                onHover("§eClick to add ${amount}x §d${type.displayName}§e!")
                                onClick { addCarry(name, type.id, amount) }
                            },
                        )

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

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderTop() {
        if (InventoryUtils.inAnyInventory()) {
            InventoryGuiScaleCompat.withOriginalHudScale {
                renderDisplay()
            }
        } else {
            renderDisplay()
        }
    }

    private fun renderDisplay() {
        config.display.renderRenderables(display, posLabel = "Carry Tracker")
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
            add("carryTypes: $carryTypes")
            add("carryPrices: ${storage.carryPrices}")
            add("lastTradedPlayer: $lastTradedPlayer")
            add("recentTrades: $recentTrades")
        }
    }

    @Suppress("LongMethod")
    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcarry") {
            description = "Keep track of carries you do."
            category = CommandCategory.USERS_ACTIVE
            literal("add") {
                arg(
                    "player",
                    BrigadierArguments.word(),
                    PlayerSuggestions.builder { includeAllSources() },
                ) { player ->
                    arg(
                        "type",
                        BrigadierArguments.word(),
                        carryTypes.map { it.id },
                    ) { type ->
                        arg(
                            "amount",
                            BrigadierArguments.integer(),
                            listOf("1", "10", "30", "160", "200"),
                        ) { amount ->
                            callback {
                                addCarry(getArg(player), getArg(type), getArg(amount))
                            }
                        }
                        callback {
                            addCarry(getArg(player), getArg(type))
                        }
                    }
                    callback {
                        addCustomer(getArg(player))
                    }
                }
                callback { showHelpUserError() }
            }
            literal("remove") {
                arg(
                    "player",
                    BrigadierArguments.word(),
                    PlayerSuggestions.builder { include(PlayerNameSource.CARRY_CUSTOMER) },
                ) { player ->
                    arg(
                        "type",
                        BrigadierArguments.word(),
                        carryTypes.map { it.id },
                    ) { type ->
                        arg(
                            "amount",
                            BrigadierArguments.integer(),
                            listOf("1", "10", "30", "160", "200"),
                        ) { amount ->
                            callback {
                                removeCarry(getArg(player), getArg(type), getArg(amount))
                            }
                        }
                        callback {
                            removeCarry(getArg(player), getArg(type))
                        }
                    }
                    callback {
                        removeCustomer(getArg(player))
                    }
                }
                callback { showHelpUserError() }
            }
            literal("updateDone") {
                arg(
                    "player",
                    BrigadierArguments.word(),
                    PlayerSuggestions.builder { include(PlayerNameSource.CARRY_CUSTOMER) },
                ) { player ->
                    arg(
                        "type",
                        BrigadierArguments.word(),
                        carryTypes.map { it.id },
                    ) { type ->
                        arg(
                            "amount",
                            BrigadierArguments.integer(),
                            listOf("1", "5", "10", "-1", "-5", "-10"),
                        ) { amount ->
                            callback {
                                updateDone(getArg(player), getArg(type), getArg(amount))
                            }
                        }
                        callback { showHelpUserError() }
                    }
                    callback { showHelpUserError() }
                }
                callback { showHelpUserError() }
            }
            literal("updatePaid") {
                arg(
                    "player",
                    BrigadierArguments.word(),
                    PlayerSuggestions.builder { include(PlayerNameSource.CARRY_CUSTOMER) },
                ) { player ->
                    arg(
                        "coins",
                        BrigadierArguments.word(),
                        listOf("1m", "5m", "10m", "-1m", "-5m", "-10m"),
                    ) { coins ->
                        callback {
                            updatePaid(getArg(player), getArg(coins))
                        }
                    }
                    callback { showHelpUserError() }
                }
                callback { showHelpUserError() }
            }
            literal("price") {
                literal("list") {
                    arg(
                        "page",
                        BrigadierArguments.integer(),
                    ) { page ->
                        callback {
                            listPrices(getArg(page))
                        }
                    }
                    callback {
                        listPrices()
                    }
                }
                literal("set") {
                    arg(
                        "type",
                        BrigadierArguments.word(),
                        carryTypes.map { it.id },
                    ) { type ->
                        arg(
                            "price",
                            BrigadierArguments.word(),
                            listOf("1m", "2m", "3m", "4m", "5m"),
                        ) { price ->
                            callback {
                                setPrice(getArg(type), getArg(price))
                            }
                        }
                        callback { showHelpUserError() }
                    }
                }
                literal("delete") {
                    arg(
                        "type",
                        BrigadierArguments.word(),
                        carryTypes.map { it.id },
                    ) { type ->
                        callback {
                            deletePrice(getArg(type))
                        }
                    }
                    callback { showHelpUserError() }
                }
                literal("deleteAll") {
                    callback {
                        deleteAllPrices()
                    }
                }
                callback { showHelpUserError() }
            }
            literal("clearAll") {
                callback {
                    clearAll()
                }
            }
            simpleCallback {
                showHelpUserError()
            }
        }
    }

    private fun showHelpUserError() {
        ChatUtils.userError(
            "Usage:\n" +
                "  §c/shcarry add <player> [type] [amount]\n" +
                "  §c/shcarry remove <player> [type] [amount]\n" +
                "  §c/shcarry updateDone <player> <type> <amount>\n" +
                "  §c/shcarry updatePaid <player> <coins>\n" +
                "  §c/shcarry price list\n" +
                "  §c/shcarry price set <type> <price>\n" +
                "  §c/shcarry price delete <type>\n" +
                "  §c/shcarry price deleteAll\n" +
                "  §c/shcarry clearAll",
            true,
        )
    }

    private fun addCustomer(customerName: String) {
        findCustomer(customerName)?.let { customer ->
            ChatUtils.userError("Customer already exists: §b${customer.name}")
            return
        }

        val customer = addCustomerInternal(customerName)
        ChatUtils.chat("Customer added: §b${customer.name}")
    }

    private fun addCustomerInternal(name: String): Customer {
        val customer = Customer(name)
        customers.add(customer)
        recentTrades.entries.firstOrNull { it.key.equals(name, true) }?.let {
            customer.coinsPaid += it.value
        }

        return customer
    }

    private fun addCarry(customerName: String, rawType: String, amount: Int = 0) {
        findCarryType(rawType)?.let { type ->
            val customer = findCustomer(customerName) ?: addCustomerInternal(customerName)

            customer.findCarry(type)?.let { carry ->
                updateTotal(customer, carry, amount)
            } ?: run {
                if (amount < 0) {
                    ChatUtils.userError("Carry amount cannot be negative!")
                    return
                }

                val carry = Carry(type, amount)
                customer.carries.add(carry)

                updateDisplay()
                ChatUtils.chat("Carry added: §b${customer.name} ${carry.formatProgress()} §d${carry.type.displayName}")
            }
        } ?: ChatUtils.userError("Unknown carry type: '$rawType'")
    }

    private fun removeCustomer(customerName: String) {
        findCustomer(customerName)?.let { customer ->
            removeCustomerInternal(customer)
        } ?: ChatUtils.userError("Customer not found: §b$customerName")
    }

    private fun removeCustomerInternal(customer: Customer) {
        customers.remove(customer)
        updateDisplay()
        ChatUtils.chat("Customer removed: §b${customer.name}")
    }

    private fun removeCarry(customerName: String, rawType: String, amount: Int? = null) {
        findCustomer(customerName)?.let { customer ->
            findCarryType(rawType)?.let { type ->
                customer.findCarry(type)?.let { carry ->
                    if (amount != null) updateTotal(customer, carry, -amount)
                    else removeCarryInternal(customer, carry)
                } ?: ChatUtils.userError("Carry not found: §b${customer.name} §d${type.displayName}")
            } ?: ChatUtils.userError("Unknown carry type: '$rawType'")
        } ?: ChatUtils.userError("Customer not found: §b$customerName")
    }

    private fun removeCarryInternal(customer: Customer, carry: Carry) {
        customer.carries.remove(carry)
        updateDisplay()
        ChatUtils.chat("Carry removed: §b${customer.name} §d${carry.type.displayName}")
    }

    private fun updateTotal(customer: Customer, carry: Carry, amount: Int) {
        val newTotal = carry.total + amount
        if (newTotal < 0) {
            ChatUtils.userError("New carry amount cannot be negative!")
            return
        }
        carry.total = newTotal

        updateDisplay()
        ChatUtils.chat("Carry updated: §b${customer.name} ${carry.formatProgress()} §d${carry.type.displayName}")
    }

    private fun updateDone(customerName: String, rawType: String, amount: Int) {
        findCustomer(customerName)?.let { customer ->
            findCarryType(rawType)?.let { type ->
                customer.findCarry(type)?.let { carry ->
                    updateDoneInternal(customer, carry, amount)
                } ?: ChatUtils.userError("Carry not found: §b${customer.name} §d${type.displayName}")
            } ?: ChatUtils.userError("Unknown carry type: '$rawType'")
        } ?: ChatUtils.userError("Customer not found: §b$customerName")
    }

    private fun updateDoneInternal(customer: Customer, carry: Carry, amount: Int) {
        val newDone = carry.done + amount
        if (newDone < 0) {
            ChatUtils.userError("New carries done cannot be negative!")
            return
        }
        carry.done = newDone

        updateDisplay()
        ChatUtils.chat("Carry updated: §b${customer.name} ${carry.formatProgress()} §d${carry.type.displayName}")
    }

    private fun updatePaid(customerName: String, rawCoins: String) {
        findCustomer(customerName)?.let { customer ->
            rawCoins.formatDoubleOrUserError()?.let { coins ->
                customer.coinsPaid += coins
                updateDisplay()
                ChatUtils.chat(
                    "Customer updated: §b${customer.name} " +
                        "§6${customer.coinsPaid.shortFormat()}§8/§6${customer.getTotalCost().shortFormat()}" +
                        " coins §epaid",
                )
            }
        } ?: ChatUtils.userError("Customer not found: §b$customerName")
    }

    private fun listPrices(page: Int = 1) {
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

    private fun setPrice(rawType: String, rawPrice: String) {
        findCarryType(rawType)?.let { type ->
            rawPrice.formatDoubleOrUserError()?.let { price ->
                if (price < 0.0) ChatUtils.userError("Carry price cannot be negative!")
                else if (price == 0.0) ChatUtils.userError("Carry price cannot be 0! Use /shcarry price delete $rawType instead")
                else {
                    type.pricePer = price
                    updateDisplay()
                    ChatUtils.chat("Set carry price for §d${type.displayName} §eto §6${price.shortFormat()} coins")
                }
            }
        } ?: ChatUtils.userError("Unknown carry type: '$rawType'")
    }

    private fun deletePrice(rawType: String) {
        findCarryType(rawType)?.let { type ->
            type.pricePer = 0.0
            updateDisplay()
            ChatUtils.chat("Deleted carry price for §d${type.displayName}")
        } ?: ChatUtils.userError("Unknown carry type: '$rawType'")
    }

    private fun deleteAllPrices() {
        for (type in carryTypes) type.pricePer = 0.0
        updateDisplay()
        ChatUtils.chat("Deleted all carry prices")
    }

    private fun clearAll() {
        val customerSize = customers.size
        val carrySize = customers.sumOf { it.carries.size }
        customers.clear()
        updateDisplay()
        ChatUtils.chat("Removed §b$customerSize §ecustomers including §b$carrySize §ecarries")
    }

    private fun countCarry(customer: Customer, carry: Carry) {
        carry.done++
        customer.stats.done()
        updateDisplay()

        HypixelCommands.partyChat("${customer.name}: ${carry.done}/${carry.total}")
        if (carry.done >= carry.total) {
            ChatUtils.chat {
                append("Carry finished: §b${customer.name} ${carry.formatProgress()} §b${carry.type.displayName}\n")

                if (customer.carries.size > 1) {
                    append("§e[CLICK to remove this carry]") {
                        onHover("§eClick to remove this carry!")
                        onClick { removeCarryInternal(customer, carry) }
                    }
                } else {
                    append("§e[CLICK to remove this customer]") {
                        onHover("§eClick to remove this customer!")
                        onClick { removeCustomerInternal(customer) }
                    }
                }
            }
            TitleManager.sendTitle("§eCarry finished for §b${customer.name}§e!", duration = 3.seconds)
            SoundUtils.playBeepSound()
        }
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
                val perHour = customer.stats.perHour()
                val averageTime = customer.stats.averageTime().format(showMilliSeconds = false)
                add(
                    Renderable.clickable(
                        "§b${customer.name} §6$paidFormat§8/§6$totalCostFormat §8(§7$sinceLast §8| " +
                            (if (totalDone == 0) "§7N/A §8| §7N/A" else "§7${perHour.roundToInt()}/h §8| §7$averageTime avg") + "§8)",
                        tips = buildList {
                            add("§7Carries for §b${customer.name}")
                            add("")
                            add("§7Total cost: §6$totalCostFormat")
                            add("§7Already paid: §6$paidFormat")
                            add("§7Still missing: §6$missingFormat")
                            add("")
                            add("§7Total carries done: §e$totalDone")
                            add("§7Total elapsed time: §e$sinceStart")
                            add("§7Carries per hour: §e${perHour.roundTo(1).removeUnusedDecimal()}")
                            add("§7Average time per carry: §e$averageTime")
                            add("§7Elapsed time since last carry: §e$sinceLast")
                            add("")
                            add("§eClick to send missing coins in party chat!")
                            add("§e${KeyboardManager.getModifierKeyName()}-click to remove this customer!")
                        },
                        onLeftClick = {
                            if (KeyboardManager.isModifierKeyDown()) removeCustomerInternal(customer)
                            else HypixelCommands.partyChat("${customer.name}: $paidFormat/$totalCostFormat coins paid")
                        },
                    ),
                )

                for (carry in customer.carries) {
                    val carryTotalCostFormat = carry.getCost().shortFormat()
                    add(
                        Renderable.clickable(
                            "  §d${carry.type.displayName} ${carry.formatProgress()} §6$carryTotalCostFormat",
                            tips = buildList {
                                add("§b${customer.name} §d${carry.type.displayName} §cCarry")
                                add("")
                                add("§7Total: §e${carry.total}")
                                add("§7Done: §e${carry.done}")
                                add("§7Missing: §e${carry.total - carry.done}")
                                add("")
                                add("§7Total cost: §6$carryTotalCostFormat")
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
                                if (KeyboardManager.isModifierKeyDown()) removeCarryInternal(customer, carry)
                                else HypixelCommands.partyChat("${customer.name} ${carry.type.displayName}: ${carry.done}/${carry.total}")
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun findCustomer(name: String): Customer? = customers.firstOrNull { it.name.equals(name, ignoreCase = true) }
    private fun findCarryType(id: String): CarryType? = findCarryType { it.id.equals(id, ignoreCase = true) }
    private fun findCarryType(predicate: (CarryType) -> Boolean): CarryType? = carryTypes.firstOrNull(predicate)

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
            get() = storage.carryPrices.getOrDefault(id, field)
            set(value) {
                field = value
                if (value == 0.0) storage.carryPrices.remove(id)
                else storage.carryPrices.put(id, value)
            }
    }

    private data class SlayerCarryType(
        override val id: String,
        override val displayName: String,
        override val shortName: String,
        val slayerType: SlayerType,
        val slayerTier: Int,
    ) : CarryType()

    private data class DungeonCarryType(
        override val id: String,
        override val displayName: String,
        val dungeonFloor: String,
        override val shortName: String = dungeonFloor,
    ) : CarryType()

    private data class KuudraCarryType(
        override val id: String,
        override val displayName: String,
        val kuudraTier: KuudraTier,
        override val shortName: String = displayName,
    ) : CarryType()

    private data class CrimsonMinibossCarryType(
        override val id: String,
        override val displayName: String,
        override val shortName: String,
        val crimsonMiniboss: CrimsonMiniBoss,
    ) : CarryType()

    private val carryTypes: List<CarryType> = buildList {
        for (i in 1..5) add(SlayerCarryType("rev$i", "${SlayerType.REVENANT.displayName} $i", "Rev $i", SlayerType.REVENANT, i))
        for (i in 1..5) add(SlayerCarryType("tara$i", "${SlayerType.TARANTULA.displayName} $i", "Tara $i", SlayerType.TARANTULA, i))
        for (i in 1..4) add(SlayerCarryType("sven$i", "${SlayerType.SVEN.displayName} $i", "Sven $i", SlayerType.SVEN, i))
        for (i in 1..4) add(SlayerCarryType("eman$i", "${SlayerType.VOID.displayName} $i", "Eman $i", SlayerType.VOID, i))
        for (i in 1..4) add(SlayerCarryType("blaze$i", "${SlayerType.INFERNO.displayName} $i", "Blaze $i", SlayerType.INFERNO, i))
        for (i in 1..5) add(SlayerCarryType("vamp$i", "${SlayerType.VAMPIRE.displayName} $i", "Vamp $i", SlayerType.VAMPIRE, i))

        add(DungeonCarryType("f0", "Entrance Floor", "E"))
        for (i in 1..7) add(DungeonCarryType("f$i", "Floor $i", "F$i"))
        for (i in 1..7) add(DungeonCarryType("m$i", "Master Mode $i", "M$i"))

        for (i in 1..5) add(KuudraCarryType("k$i", "${KuudraTier.entries[i - 1].displayName} Kuudra", KuudraTier.entries[i - 1]))

        add(CrimsonMinibossCarryType("bladesoul", CrimsonMiniBoss.BLADESOUL.displayName, "Bladesoul", CrimsonMiniBoss.BLADESOUL))
        add(CrimsonMinibossCarryType("mage_outlaw", CrimsonMiniBoss.MAGE_OUTLAW.displayName, "Mage Outlaw", CrimsonMiniBoss.MAGE_OUTLAW))
        add(
            CrimsonMinibossCarryType(
                "barb_duke",
                CrimsonMiniBoss.BARBARIAN_DUKE_X.displayName,
                "Barb Duke",
                CrimsonMiniBoss.BARBARIAN_DUKE_X,
            ),
        )
        add(CrimsonMinibossCarryType("ashfang", CrimsonMiniBoss.ASHFANG.displayName, "Ashfang", CrimsonMiniBoss.ASHFANG))
        add(CrimsonMinibossCarryType("magma_boss", CrimsonMiniBoss.MAGMA_BOSS.displayName, "Magma Boss", CrimsonMiniBoss.MAGMA_BOSS))
    }

    @HandleEvent
    fun onOtherPlayersSlayerSpawn(event: OtherPlayersSlayerEvent.Spawn) {
        findCarryType { it is SlayerCarryType && it.slayerType == event.slayerType && it.slayerTier == event.tier }?.let { type ->
            findCustomer(event.owner)?.let { customer ->
                customer.findCarry(type)?.let { carry ->
                    ChatUtils.chat("§d${carry.type.displayName} §espawned for §b${customer.name}")
                    TitleManager.sendTitle("§eBoss spawned for §b${customer.name}§e!", duration = 3.seconds)
                    SoundUtils.playPlingSound()
                }
            }
        }
    }

    @HandleEvent
    fun onOtherPlayersSlayerDeath(event: OtherPlayersSlayerEvent.Death) {
        findCarryType { it is SlayerCarryType && it.slayerType == event.slayerType && it.slayerTier == event.tier }?.let { type ->
            findCustomer(event.owner)?.let { customer ->
                customer.findCarry(type)?.let { carry ->
                    // MobEvent.Death already triggers late, no need to wait
                    countCarry(customer, carry)
                }
            }
        }
    }

    @HandleEvent
    fun onDungeonComplete(event: DungeonCompleteEvent) {
        findCarryType { it is DungeonCarryType && it.dungeonFloor == event.floor }?.let { type ->
            for (partyMember in PartyApi.partyMembers) {
                findCustomer(partyMember)?.let { customer ->
                    customer.findCarry(type)?.let { carry ->
                        // wait for other server messages first
                        DelayedRun.runNextTick { countCarry(customer, carry) }
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onKuudraComplete(event: KuudraCompleteEvent) {
        findCarryType { it is KuudraCarryType && it.kuudraTier.tierNumber == event.kuudraTier }?.let { type ->
            for (partyMember in PartyApi.partyMembers) {
                findCustomer(partyMember)?.let { customer ->
                    customer.findCarry(type)?.let { carry ->
                        // wait for other server messages first
                        DelayedRun.runNextTick { countCarry(customer, carry) }
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onCrimsonMinibossKilled(event: CrimsonMinibossKilledEvent) {
        findCarryType { it is CrimsonMinibossCarryType && it.crimsonMiniboss == event.miniboss }?.let { type ->
            for (partyMember in PartyApi.partyMembers) {
                findCustomer(partyMember)?.let { customer ->
                    customer.findCarry(type)?.let { carry ->
                        // wait for other server messages first
                        DelayedRun.runNextTick { countCarry(customer, carry) }
                    }
                }
            }
        }
    }

    fun isCustomer(customerName: String): Boolean = findCustomer(customerName) != null
    fun getCustomerNames(): List<String> = customers.map { it.name }
}
