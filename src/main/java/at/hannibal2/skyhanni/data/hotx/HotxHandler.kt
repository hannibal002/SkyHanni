package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.takeUnlessEmpty
import at.hannibal2.skyhanni.utils.RegexUtils.indexOfFirstMatch
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.world.inventory.Slot
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class HotxHandler<Data : HotxData<Reward>, Reward>(val data: Collection<Data>) {

    /**
     * Name of the Tree Eg: HotM, HotF
     */
    abstract val name: String
    abstract val core: Data
    abstract var tokens: Int
        protected set
    abstract var availableTokens: Int
        protected set

    protected abstract val enabledPattern: Pattern
    protected abstract val inventoryPattern: Pattern
    protected abstract val levelPattern: Pattern
    protected abstract val notUnlockedPattern: Pattern
    protected abstract val heartItemPattern: Pattern
    protected abstract val resetItemPattern: Pattern
    abstract val position: Position
    abstract val shouldShowDisplay: Boolean

    /**
     * Needs a group "token" (only digits)
     */
    protected abstract val heartTokensPattern: Pattern

    /**
     * Needs a group "token" (only digits)
     */
    protected abstract val resetTokensPattern: Pattern
    protected abstract val readingLevelTransform: Matcher.() -> Int

    val inApplicableIsland: Boolean get() = islandTypeTag.isInIsland()
    val inInventory: Boolean get() = treeInventoryDetector.isInside()
    var heartItem: Slot? = null

    init {
        data.forEach { it.guiNamePattern }
    }

    /**
     * Function that is called after the entries are read.
     */
    abstract fun extraInventoryHandling()

    protected abstract fun Slot.extraHandling(entry: Data, lore: List<String>)

    fun getPerkByNameOrNull(name: String): Data? = data.find { it.guiName == name }

    protected fun resetTree() {
        data.forEach {
            it.rawLevel = 0
            it.enabled = false
            it.isUnlocked = false
        }

        rotatingPerkSlots.forEach {
            it.currentPerk = null
        }

        currencyReset()
    }

    fun Slot.parse() {
        val item = this.item.takeUnlessEmpty() ?: return

        if (handleCurrency()) return

        val entry = data.firstOrNull { it.guiNamePattern.matches(item.cleanName) } ?: return
        entry.slot = this
        entry.item = item

        val rawLore = item.getLore()
        val lore = item.getLoreComponent().takeIf { it.isNotEmpty() }?.map { it.string.removeColor() } ?: return

        if (entry != core && notUnlockedPattern.matches(lore.last())) {
            entry.rawLevel = 0
            entry.enabled = false
            entry.isUnlocked = false
            return
        }

        entry.isUnlocked = true

        // This needs color codes
        entry.rawLevel = levelPattern.matchMatcher(rawLore.first(), readingLevelTransform) ?: entry.maxLevel

        // raw level to ignore the blue egg buff
        if (entry.rawLevel > entry.maxLevel) {
            ErrorManager.skyHanniError(
                "$name Perk '${entry.name}' over max level",
                "name" to entry.name,
                "activeLevel" to entry.activeLevel,
                "maxLevel" to entry.maxLevel,
            )
        }

        if (entry == core) {
            entry.enabled = entry.rawLevel != 0
            return
        }
        entry.enabled = lore.any { enabledPattern.matches(it) }

        fetchRotatingPerk(entry, lore)

        extraHandling(entry, lore)
    }

    fun debugTree(event: DebugDataCollectEvent) {
        event.title("$name - Tree")
        event.addIrrelevant(
            data.filter { it.isUnlocked }.map {
                "${if (it.enabled) "✔" else "✖"} ${it.printName}: ${it.activeLevel}"
            },
        )
    }

    /**
     * @return True means it read an item, false means it did not.
     */
    protected fun Slot.handleCurrency(): Boolean {
        val item = this.item.takeUnlessEmpty() ?: return false

        val isHeartItem = when {
            heartItemPattern.matches(item.cleanName) -> true
            resetItemPattern.matches(item.cleanName) -> false
            else -> return false
        }

        if (isHeartItem) { // Reset on the heart Item to remove duplication
            availableTokens = 0
            currencyReset(true)
            heartItem = this
        }

        val lore = item.getLoreComponent().map { it.string.removeColor() }

        val tokenPattern = if (isHeartItem) heartTokensPattern else resetTokensPattern
        lore@ for (line in lore) {
            tokenPattern.matchMatcher(line) {
                val token = group("token").toInt()
                if (isHeartItem) {
                    availableTokens = token
                }
                tokens += token
                continue@lore
            }
            readFromHeartOrReset(line, isHeartItem)
        }
        return true
    }

    protected abstract fun readFromHeartOrReset(line: String, isHeartItem: Boolean)

    protected open fun currencyReset(full: Boolean = false) {
        availableTokens = tokens
    }

    private val treeInventoryDetector by lazy {
        InventoryDetector(
            repoPattern = { inventoryPattern },
            onOpenInventory = {
                DelayedRun.runNextTick {
                    InventoryUtils.getItemsInOpenChest().forEach { it.parse() }
                    extraInventoryHandling()
                }
            },
            onCloseInventory = { _ ->
                data.forEach {
                    it.slot = null
                    it.item = null
                }
                heartItem = null
            },
        )
    }

    protected abstract val islandTypeTag: IslandTypeTag
    protected open val rotatingPerkPattern: Pattern by lazy { HotxPatterns.rotatingPerkPattern }
    abstract val rotatingPerkSlots: List<RotatingPerkSlot<Data>>

    abstract val resetChatPattern: Pattern

    abstract fun extraChatHandling(event: SkyHanniChatEvent.Allow)

    open fun onChat(event: SkyHanniChatEvent.Allow) {
        if (resetChatPattern.matches(event.cleanMessage)) {
            resetTree()
            return
        }
        extraChatHandling(event)
    }

    abstract fun tryBlock(event: SkyHanniChatEvent.Allow)

    fun tryReadRotatingPerkChat(event: SkyHanniChatEvent.Allow): Boolean? {
        rotatingPerkPattern.matchMatcher(event.cleanMessage) {
            val perkString = group("perk")

            val result = rotatingPerkSlots.firstNotNullOfOrNull { slot ->
                val perk = slot.perks.firstOrNull {
                    it.chatPattern.matches(perkString)
                }

                if (perk != null) {
                    slot to perk
                } else {
                    null
                }
            } ?: return false

            tryBlock(event)
            result.first.currentPerk = result.second

            return true
        }

        return null
    }

    private fun fetchRotatingPerk(entry: Data, lore: List<String>) {
        val slot = rotatingPerkSlots.firstOrNull {
            it.entry == entry
        } ?: return

        if (!entry.enabled || !entry.isUnlocked) return

        // Hypixel sometimes doesn't show the current perk in lore if switching hotx trees via loadouts
        val index = HotxPatterns.itemPreEffectPattern.indexOfFirstMatch(lore) ?: return

        val nextLine = lore.getOrNull(index + 1) ?: return

        val perkLore = HotxPatterns.rotatingPerkPattern
            .matchGroup(nextLine, "perk")
            ?: return

        slot.currentPerk = slot.perks.firstOrNull {
            it.itemPattern.matches(perkLore)
        }

        if (slot.currentPerk == null) {
            ErrorManager.logErrorStateWithData(
                "Could not read rotating perk from $name tree",
                "no itemPattern matched",
                "entry" to entry.guiName,
                "perkLore" to perkLore,
            )
        }
    }
}
