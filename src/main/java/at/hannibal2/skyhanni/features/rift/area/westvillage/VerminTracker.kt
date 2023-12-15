package at.hannibal2.skyhanni.features.rift.area.westvillage

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

object VerminTracker {
    private val silverfishPattern by RepoPattern.pattern(
        "rift.area.westvillage.vermintracker.silverfish",
        ".*§eYou vacuumed a §.*Silverfish.*"
    )
    private val spiderPattern by RepoPattern.pattern(
        "rift.area.westvillage.vermintracker.spider",
        ".*§eYou vacuumed a §.*Spider.*"
    )
    private val flyPattern by RepoPattern.pattern(
        "rift.area.westvillage.vermintracker.fly",
        ".*§eYou vacuumed a §.*Fly.*"
    )
    private val verminBinPattern by RepoPattern.pattern(
        "rift.area.westvillage.vermintracker.binline",
        "§fVermin Bin: §\\w(?<count>\\d+) (?<vermin>\\w+)"
    )
    private val verminBagPattern by RepoPattern.pattern(
        "rift.area.westvillage.vermintracker.bagline",
        "§fVacuum Bag: §\\w(?<count>\\d+) (?<vermin>\\w+)"
    )

    private val config get() = RiftAPI.config.area.westVillage.verminTracker

    private val tracker = SkyHanniTracker("Vermin Tracker", { Data() }, { it.rift.verminTracker })
    { drawDisplay(it) }

    class Data : TrackerData() {
        override fun reset() {
            count.clear()
        }

        @Expose
        var count: MutableMap<VerminType, Int> = mutableMapOf()
    }

    enum class VerminType(val vermin: String, val pattern: Pattern) {
        SILVERFISH("§aSilverfish", silverfishPattern),
        SPIDER("§aSpiders", spiderPattern),
        FLY("§aFlies", flyPattern),
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        for (verminType in VerminType.entries) {
            if (verminType.pattern.matches(event.message)) {
                addVermin(verminType)
                if (config.hideChat) {
                    event.blockedReason = "vermin_vacuumed"
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!RiftAPI.inRift() || event.inventoryName != "Vermin Bin") return

        val bin = event.inventoryItems[13]?.getLore() ?: return
        var bag = InventoryUtils.getItemsInOwnInventory()
            .firstOrNull { it.getInternalName() == "TURBOMAX_VACUUM".asInternalName() }
            ?.getLore() ?: emptyList()

        val binCounts = countVermin(bin, verminBinPattern)
        VerminType.entries.forEach {
            setVermin(it, binCounts[it] ?: 0)
        }

        if (bag == emptyList<String>()) return

        val bagCounts = countVermin(bag, verminBagPattern)
        VerminType.entries.forEach {
            addVermin(it, bagCounts[it] ?: 0)
        }
    }

    private fun countVermin(lore: List<String>, pattern: Pattern): MutableMap<VerminTracker.VerminType, Int> {
        val verminCounts = mutableMapOf(
            VerminType.SILVERFISH to 0,
            VerminType.SPIDER to 0,
            VerminType.FLY to 0
        )
        for (line in lore) {
            pattern.matchMatcher(line) {
                val vermin = group("vermin")?.lowercase() ?: continue
                val verminCount = group("count")?.toInt() ?: continue
                val verminType = getVerminType(vermin)
                verminCounts[verminType] = verminCount
            }
        }
        return verminCounts
    }

    private fun getVerminType(vermin: String): VerminType {
        return when (vermin) {
            "silverfish" -> VerminType.SILVERFISH
            "silverfishes" -> VerminType.SILVERFISH
            "spider" -> VerminType.SPIDER
            "spiders" -> VerminType.SPIDER
            "fly" -> VerminType.FLY
            "flies" -> VerminType.FLY
            else -> VerminType.SILVERFISH
        }
    }

    private fun addVermin(vermin: VerminType, count: Int = 1) {
        tracker.modify { it.count.addOrPut(vermin, count) }
    }

    private fun setVermin(vermin: VerminType, count: Int) {
        tracker.modify { it.count.set(vermin, count) }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§7Vermin Tracker:")
        for ((vermin, amount) in data.count.sortedDesc()) {
            val verminName = vermin.vermin
            addAsSingletonList(" §7- §e${amount.addSeparators()} $verminName")
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        tracker.renderDisplay(config.pos)
    }

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetvermintracker")
    }

    private fun isEnabled() = RiftAPI.inRift() && config.enabled
}
