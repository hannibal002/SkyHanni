package at.hannibal2.skyhanni.features.rift.area.westvillage

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import java.util.regex.Pattern

@SkyHanniModule
object VerminTracker {

    private val patternGroup = RepoPattern.group("rift.area.westvillage.vermintracker")

    /**
     * REGEX-TEST: §eYou vacuumed a §r§aSilverfish§r§e!
     */
    private val silverfishPattern by patternGroup.pattern(
        "silverfish",
        ".*§eYou vacuumed a §.*Silverfish.*",
    )

    /**
     * REGEX-TEST: §eYou vacuumed a §r§aSpider§r§e!
     */
    private val spiderPattern by patternGroup.pattern(
        "spider",
        ".*§eYou vacuumed a §.*Spider.*",
    )

    /**
     * REGEX-TEST: §eYou vacuumed a §r§aFly§r§e!
     */
    private val flyPattern by patternGroup.pattern(
        "fly",
        ".*§eYou vacuumed a §.*Fly.*",
    )

    /**
     * REGEX-TEST: Vermin Bin: 27 Silverfishes
     * REGEX-TEST: Vermin Bin: 19 Flies
     */
    private val verminBinPattern by patternGroup.pattern(
        "binline-nocolor",
        "Vermin Bin: (?<count>\\d+) (?<vermin>\\w+)",
    )

    private var hasVacuum = false
    private val TURBOMAX_VACUUM = "TURBOMAX_VACUUM".toInternalName()

    private val config get() = RiftApi.config.area.westVillage.verminTracker

    private val tracker = SkyHanniTracker("Vermin Tracker", { Data() }, { it.rift.verminTracker }) {
        drawDisplay(it)
    }

    class Data : TrackerData() {

        override fun reset() {
            count.clear()
        }

        @Expose
        var count: MutableMap<VerminType, Int> = mutableMapOf()
    }

    enum class VerminType(val order: Int, val vermin: String, val pattern: Pattern) {
        FLY(1, "§aFlies", flyPattern),
        SPIDER(2, "§aSpiders", spiderPattern),
        SILVERFISH(3, "§aSilverfish", silverfishPattern),
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onSecondPassed(event: SecondPassedEvent) {
        checkVacuum()
    }

    private fun checkVacuum() {
        hasVacuum = InventoryUtils.getItemsInOwnInventory()
            .any { it.getInternalName() == TURBOMAX_VACUUM }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChat(event: SkyHanniChatEvent) {
        for (verminType in VerminType.entries) {
            if (verminType.pattern.matches(event.message)) {
                tracker.modify { it.count.addOrPut(verminType, 1) }

                if (config.hideChat) {
                    event.blockedReason = "vermin_vacuumed"
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Vermin Bin") return

        val bin = event.inventoryItems[13]?.getLore() ?: return

        val binCounts = countVerminBin(bin)
        VerminType.entries.forEach { setVermin(it, binCounts[it] ?: 0) }

        val bag = InventoryUtils.getItemsInOwnInventory()
            .firstOrNull { it.getInternalName() == TURBOMAX_VACUUM }
            ?.getExtraAttributes() ?: return

        val bagCounts = mapOf(
            VerminType.SILVERFISH to bag.getInteger("vacuumed_silverfish"),
            VerminType.SPIDER to bag.getInteger("vacuumed_spider"),
            VerminType.FLY to bag.getInteger("vacuumed_mosquito"),
        )
        VerminType.entries.forEach { addVermin(it, bagCounts[it] ?: 0) }
    }

    private fun countVerminBin(lore: List<String>): Map<VerminType, Int> =
        buildMap {
            verminBinPattern.matchAll(lore.map { it.removeColor() }) {
                val vermin = group("vermin").lowercase()
                val verminCount = group("count").formatInt()
                val verminType = getVerminType(vermin)
                put(verminType, verminCount)
            }
        }

    private fun getVerminType(vermin: String): VerminType {
        return when (vermin) {
            "silverfish", "silverfishes" -> VerminType.SILVERFISH
            "spider", "spiders" -> VerminType.SPIDER
            "fly", "flies" -> VerminType.FLY
            else -> VerminType.SILVERFISH
        }
    }

    private fun addVermin(vermin: VerminType, count: Int = 1) {
        tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { it.count.addOrPut(vermin, count) }
    }

    private fun setVermin(vermin: VerminType, count: Int) {
        tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { it.count[vermin] = count }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Vermin Tracker:")
        for ((vermin, amount) in data.count.entries.sortedBy { it.key.order }) {
            val verminName = vermin.vermin
            addSearchString(" §7- §e${amount.addSeparators()} $verminName", verminName)
        }
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!isEnabled()) return false
        if (!config.showOutsideWestVillage && !RiftApi.inWestVillage()) return false
        if (!config.showWithoutVacuum && !hasVacuum) return false

        return true
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.THE_RIFT) {
            tracker.firstUpdate()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetvermintracker") {
            description = "Resets the Vermin Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }

    private fun isEnabled() = RiftApi.inRift() && config.enabled
}
