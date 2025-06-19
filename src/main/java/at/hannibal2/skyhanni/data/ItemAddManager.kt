package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
//#if TODO
import at.hannibal2.skyhanni.events.SackChangeEvent
//#endif
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.features.inventory.SuperCraftFeatures.craftedPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.evictOldestEntry
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ItemAddManager {
    enum class Source(val displayName: String) {
        ITEM_ADD("Picked up in inventory"),
        SACKS("Went into Sacks"),
        COMMAND("Invented via command"),
    }

    private val ARCHFIEND_DICE = "ARCHFIEND_DICE".toInternalName()
    private val HIGH_CLASS_ARCHFIEND_DICE = "HIGH_CLASS_ARCHFIEND_DICE".toInternalName()

    private val diceRollChatPattern by RepoPattern.pattern(
        "data.itemmanager.diceroll",
        "§eYour §r§(?:5|6High Class )Archfiend Dice §r§erolled a §r§.(?<number>.)§r§e! Bonus: §r§.(?<hearts>.*)❤",
    )

    private var inSackInventory = false
    private var lastSackInventoryLeave = SimpleTimeMark.farPast()

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName.contains("Sack")) {
            inSackInventory = true
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (inSackInventory) {
            inSackInventory = false
            lastSackInventoryLeave = SimpleTimeMark.now()
        }
    }

    //#if TODO
    @HandleEvent(onlyOnSkyblock = true)
    fun onSackChange(event: SackChangeEvent) {

        if (inSackInventory || lastSackInventoryLeave.passedSince() < 10.seconds) return

        for (sackChange in event.sackChanges) {
            val change = sackChange.delta
            val internalName = sackChange.internalName
            if (change > 0 && internalName !in superCraftedItems) {
                Source.SACKS.addItem(internalName, change)
            }
        }
        superCraftedItems.clear()
    }
    //#endif

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemAdd(event: ItemAddInInventoryEvent) {

        val internalName = event.internalName
        if (internalName == ARCHFIEND_DICE || internalName == HIGH_CLASS_ARCHFIEND_DICE) {
            if (lastDiceRoll.passedSince() < 500.milliseconds) {
                return
            }
        }

        Source.ITEM_ADD.addItem(internalName, event.amount)
    }

    private fun Source.addItem(internalName: NeuInternalName, amount: Int) {
        ItemAddEvent(internalName, amount, this).post()
    }

    private val recentItems = mutableMapOf<ItemAddEvent, SimpleTimeMark>()

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        recentItems[event] = SimpleTimeMark.now()
        recentItems.evictOldestEntry(15)
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Recent Item Adds")
        if (recentItems.isEmpty()) return event.addIrrelevant("no items added")

        val text = formattedList().map { it.removeColor() }
        if (recentItems.values.max().passedSince() < 20.seconds) {
            event.addData(text)
        } else {
            event.addIrrelevant(text)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shdebugrecentitemadds") {
            description = "Shows recent item addions."
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                ChatUtils.clickToClipboard("Recent Item adds", formattedList())
            }
        }
    }

    private fun formattedList() = recentItems.map { (itemAddEvent, time) ->
        val itemName = itemAddEvent.internalName.repoItemName
        val amount = itemAddEvent.amount
        val source = itemAddEvent.source.displayName
        val passedSince = time.passedSince().format()
        "§r$itemName §7(§8x$amount§7) §e$source §b$passedSince ago §7(§b$time§7)"
    }

    private var lastDiceRoll = SimpleTimeMark.farPast()
    private val superCraftedItems = TimeLimitedSet<NeuInternalName>(30.seconds)

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (diceRollChatPattern.matches(event.message)) {
            lastDiceRoll = SimpleTimeMark.now()
        }
        craftedPattern.matchMatcher(event.message) {
            val internalName = NeuInternalName.fromItemName(group("item"))
            //#if TODO
            if (!SackApi.sackListInternalNames.contains(internalName.asString())) return@matchMatcher
            //#endif
            superCraftedItems.add(internalName)
        }
    }
}
