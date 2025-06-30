package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.indexOfFirstMatch
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import net.minecraft.inventory.Slot
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.KClass

abstract class HotxHandler<Data : HotxData<Reward>, Reward, RotPerkE>(val data: Collection<Data>)
    where RotPerkE : Enum<*>,
          RotPerkE : RepoPatternEnum {

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

    /**
     * Needs a group "token" (only digits)
     */
    protected abstract val heartTokensPattern: Pattern

    /**
     * Needs a group "token" (only digits)
     */
    protected abstract val resetTokensPattern: Pattern
    protected abstract val readingLevelTransform: Matcher.() -> Int

    var inInventory: Boolean = false
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
        currencyReset()
    }

    fun Slot.parse() {
        val item = this.stack ?: return

        if (this.handleCurrency()) return

        val entry = data.firstOrNull { it.guiNamePattern.matches(item.displayName) } ?: return
        entry.slot = this
        entry.item = item

        val lore = item.getLore().takeIf { it.isNotEmpty() } ?: return

        if (entry != core && notUnlockedPattern.matches(lore.last())) {
            entry.rawLevel = 0
            entry.enabled = false
            entry.isUnlocked = false
            return
        }

        entry.isUnlocked = true

        entry.rawLevel = levelPattern.matchMatcher(lore.first(), readingLevelTransform) ?: entry.maxLevel

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

        fetchRotatingPerk(entry, lore)?.let {
            setRotatingPerk(it)
        }
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
        val item = this.stack ?: return false

        val isHeartItem = when {
            heartItemPattern.matches(item.displayName) -> true
            resetItemPattern.matches(item.displayName) -> false
            else -> return false
        }

        if (isHeartItem) { // Reset on the heart Item to remove duplication
            availableTokens = 0
            currencyReset(true)
            heartItem = this
        }

        val lore = item.getLore()

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

    open fun onInventoryClose(event: InventoryCloseEvent) {
        if (!inInventory) return
        inInventory = false
        data.forEach {
            it.slot = null
            it.item = null
        }
        heartItem = null
    }

    open fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = inventoryPattern.matches(event.inventoryName)
        if (!inInventory) return
        DelayedRun.runNextTick {
            InventoryUtils.getItemsInOpenChest().forEach { it.parse() }
            extraInventoryHandling()
        }
    }

    abstract val rotatingPerkClazz: KClass<RotPerkE>
    private val rotatingPerkClassName by lazy { rotatingPerkClazz.java.simpleName.removeSuffix("Perk") }
    private val allRotatingPerks by lazy { rotatingPerkClazz.java.enumConstants }

    abstract val resetChatPattern: Pattern
    open val rotatingPerkPattern: Pattern by lazy { HotxPatterns.rotatingPerkPattern }

    abstract fun extraChatHandling(event: SkyHanniChatEvent)
    abstract fun setRotatingPerk(newRotatingPerk: RotPerkE?)

    open fun onChat(event: SkyHanniChatEvent) {
        if (resetChatPattern.matches(event.message)) {
            resetTree()
            return
        }
        extraChatHandling(event)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified PType : RepoPatternEnum> getPatternedPerks(): Map<RotPerkE, Pattern> =
        allRotatingPerks.filterIsInstance<PType>().map {
            it as RotPerkE to when (PType::class) {
                ChatRepoPatternEnum::class -> (it as ChatRepoPatternEnum).chatPattern
                ItemRepoPatternEnum::class -> (it as ItemRepoPatternEnum).itemPattern
                else -> error("Unsupported pattern type: ${PType::class.java}")
            }
        }.associate { it }

    private val chatPatternedCache: Map<RotPerkE, Pattern> by lazy {
        getPatternedPerks<ChatRepoPatternEnum>()
    }

    abstract fun tryBlock(event: SkyHanniChatEvent)

    fun tryReadRotatingPerkChat(event: SkyHanniChatEvent): Boolean? {
        rotatingPerkPattern.matchMatcher(event.message) {
            val perk = group("perk")
            val foundPerk = chatPatternedCache.firstNotNullOfOrNull { (enum, pattern) ->
                if (!pattern.matches(perk)) return@firstNotNullOfOrNull null
                enum
            } ?: return false
            tryBlock(event)
            setRotatingPerk(foundPerk)
            return true
        }
        return null
    }

    abstract val rotatingPerkEntry: Data

    private val itemPatternedCache: Map<RotPerkE, Pattern> by lazy {
        getPatternedPerks<ItemRepoPatternEnum>()
    }

    private fun fetchRotatingPerk(entry: Data, lore: List<String>): RotPerkE? {
        if (entry != rotatingPerkEntry) return null
        if (!entry.enabled || !entry.isUnlocked) return null

        val index = HotxPatterns.itemPreEffectPattern.indexOfFirstMatch(lore) ?: run {
            ErrorManager.logErrorStateWithData(
                "Could not read the $rotatingPerkClassName effect from the $name tree",
                "itemPreEffectPattern didn't match",
                "lore" to lore,
            )
            return null
        }
        val nextLine = lore[index + 1]
        val perkLore = HotxPatterns.rotatingPerkPattern.matchGroup(nextLine, "perk") ?: return null
        itemPatternedCache.firstNotNullOfOrNull { (enum, itemPattern) ->
            if (itemPattern.matches(perkLore)) return enum
            return@firstNotNullOfOrNull null
        } ?: run {
            ErrorManager.logErrorStateWithData(
                "Could not read the $rotatingPerkClassName effect from the $name tree",
                "no itemPattern matched",
                "nextLine" to nextLine,
            )
        }
        return null
    }
}
