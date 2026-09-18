package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossDeathEvent
import at.hannibal2.skyhanni.events.EndLootFoundEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LootStandUtils.readLoot
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the drops of a finished End boss fight from their floating item labels and publishes
 * them as [EndLootFoundEvent]. Dragons and the protector drop loot the same way, so one scanner
 * covers both.
 *
 * Driven by the drops themselves: a stand reports its own item and its own label the moment the
 * server sends either, and anything arriving outside the window after a kill is dropped right
 * away. Nothing is searched for on a timer.
 *
 * Deliberately independent of the profit tracker's own scan, which keeps its whitelist and its
 * weight based estimation - a feature reacting to drops should not depend on whether that
 * tracker happens to be interested in the item.
 */
@SkyHanniModule
object EndLootScanner {

    /**
     * How long after a boss died its drops are still looked for. Long enough to cover the
     * automatic pickup, which only happens about half a minute after the kill.
     */
    private val SCAN_WINDOW = 60.seconds

    /**
     * Both bosses can be killed within the same window, so their scan periods are tracked
     * separately instead of one overwriting the other.
     */
    private val activeWindows = mutableMapOf<EndBoss, SimpleTimeMark>()

    private val seenDrops = mutableSetOf<UUID>()

    /**
     * Items already reported, for each boss whose loot window is still open. One drop is made of
     * several stands and is collected later on top of that, so the same item can turn up three
     * times. A boss's list starts over with its next kill, so a new kill always counts - even when
     * it drops the same item again. Only the Pearlescent Dye drops from both bosses; one from each
     * within overlapping windows would be reported once.
     */
    private val reported = mutableMapOf<EndBoss, MutableSet<NeuInternalName>>()

    /**
     * Opened on the death message rather than on the fight summary: the loot is already lying
     * around while the summary prints, and fast pickups would be missed otherwise.
     *
     * Note: seen drops are deliberately not cleared here. A second boss dying while the first
     * window is still open must not cause its drops to be announced twice.
     */
    @HandleEvent
    private fun onEndBossDeath(event: EndBossDeathEvent) {
        activeWindows[event.boss] = SimpleTimeMark.now() + SCAN_WINDOW
        reported[event.boss] = mutableSetOf()
    }

    /**
     * A drop is made of several stands: one wears the item, another carries the label. The two
     * arrive as separate packets and either one can name the item, so both are listened to.
     */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        inspect(event.entity)
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onEntityNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        inspect(event.entity)
    }

    /** What a stand carries is read by [LootStandUtils]; which boss it belongs to is decided here. */
    private fun inspect(stand: ArmorStand) {
        // Every stand in the End reports here, so anything outside a loot window leaves at once.
        val boss = mostRecentBoss() ?: return
        if (stand.uuid in seenDrops) return

        val loot = stand.readLoot() ?: return
        // Noted only once something could be read from it: item and label arrive one after the
        // other, so a stand that says nothing yet may well be readable at its next packet.
        seenDrops.add(stand.uuid)
        report(boss, loot.internalName, loot.amount)
    }

    /**
     * Second, slower path: drops that were too far away to ever be rendered still end up in the
     * inventory when they are collected automatically. Reporting them late beats not at all, and a
     * drop already reported from its stand is filtered out by [report].
     */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onItemAdd(event: ItemAddEvent) {
        // Only what was really picked up off the ground: sacks, the hunting box and the test
        // command post the same event without a drop having been collected.
        if (event.source != ItemAddManager.Source.ITEM_ADD) return
        val boss = mostRecentBoss() ?: return
        report(boss, event.internalName, event.amount)
    }

    private fun report(boss: EndBoss, internalName: NeuInternalName, amount: Int) {
        // Checked across all open windows: a drop collected after the other boss died is still the
        // same drop, even though it is now attributed to that other boss.
        if (reported.values.any { internalName in it }) return
        reported.getOrPut(boss) { mutableSetOf() }.add(internalName)
        EndLootFoundEvent(boss, internalName, amount).post()
    }

    /**
     * The boss whose loot window is open, expired ones cleared on the way - which is the only place
     * that still needs to happen now that nothing runs on a timer.
     *
     * The label alone does not say which boss a drop came from, so the most recent kill is assumed.
     * This only matters while both windows overlap, and the drop tables barely do.
     */
    private fun mostRecentBoss(): EndBoss? {
        for (expired in activeWindows.filterValues { it.isInPast() }.keys) {
            activeWindows.remove(expired)
            reported.remove(expired)
        }
        return activeWindows.maxByOrNull { it.value }?.key
    }

    @HandleEvent
    private fun onWorldChange() {
        activeWindows.clear()
        seenDrops.clear()
        reported.clear()
    }
}
