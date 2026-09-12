package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossDeathEvent
import at.hannibal2.skyhanni.events.EndLootFoundEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getAllEquipment
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the drops of a finished End boss fight from their floating item labels and publishes
 * them as [EndLootFoundEvent]. Dragons and the protector drop loot the same way, so one scanner
 * covers both.
 *
 * Deliberately independent of the profit tracker's own scan, which keeps its whitelist and its
 * weight based estimation - a feature reacting to drops should not depend on whether that
 * tracker happens to be interested in the item.
 */
@SkyHanniModule
object EndLootScanner {

    private val repoGroup = RepoPattern.group("combat.end-loot")

    /**
     * Pet labels carry no rarity word - epic and legendary read exactly the same and differ only
     * in colour, so the generic name resolver cannot tell them apart. The colour is therefore
     * turned into the rarity suffix here.
     *
     * REGEX-TEST: §7[Lvl 1] §6Golem
     * REGEX-TEST: §7[Lvl 1] §5Ender Dragon
     * REGEX-TEST: §7[Lvl 100] §6Ender Dragon
     */
    private val petPattern by repoGroup.pattern(
        "pet",
        "§7\\[Lvl \\d+] §(?<rarity>[56])(?<name>.+)",
    )

    /**
     * How long after a boss died its drops are still looked for. Long enough to cover the
     * automatic pickup, which only happens about half a minute after the kill.
     */
    private val SCAN_WINDOW = 60.seconds

    /** NEU rarity suffixes. */
    private const val EPIC_RARITY = 3
    private const val LEGENDARY_RARITY = 4

    /**
     * Both bosses can be killed within the same window, so their scan periods are tracked
     * separately instead of one overwriting the other.
     */
    private val activeWindows = mutableMapOf<EndBoss, SimpleTimeMark>()

    private val seenDrops = mutableSetOf<UUID>()

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
    }

    /**
     * Scanned every tick rather than every second: drops are collected quickly, and one that
     * comes and goes between two scans would never be seen. Deliberately without a radius -
     * whatever the server has not sent is out of reach anyway.
     */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onTick(event: SkyHanniTickEvent) {
        activeWindows.values.removeIf { it.isInPast() }
        val boss = mostRecentBoss() ?: return

        scanNameplates(boss)
    }

    /**
     * Boss loot exists only as an armor stand with a floating label - measured in game, it has
     * no item entity behind it. Scanning item entities was tried and only ever turned up
     * unrelated ground loot from other players, so the label is the one reliable source.
     *
     * The stand's equipment is still preferred when present, because a stack names its item
     * exactly, including a pet's rarity, without any text parsing.
     */
    @OptIn(AllEntitiesGetter::class)
    private fun scanNameplates(boss: EndBoss) {
        for (entity in EntityUtils.getEntities<ArmorStand>()) {
            if (entity.uuid in seenDrops) continue

            // A drop is made of several stands: one wears the item, another carries the label.
            // The item bearing one is read too, because its name may not have arrived yet - or
            // may never arrive at this distance.
            //
            // Every slot is checked, not just head and hand: an armor piece sits in the slot it
            // belongs to, which is why armor was previously only ever found through its label.
            val carried = entity.carriedLoot()
            val fromStack = carried?.getInternalNameOrNull()?.takeIf { it != NeuInternalName.NONE }
            val label = if (entity.hasCustomName()) entity.name.formattedTextCompatLessResets() else null

            if (fromStack == null && label == null) continue
            seenDrops.add(entity.uuid)

            val internalName = fromStack
                ?: label?.let { resolvePet(it) ?: NeuInternalName.fromItemNameOrNull(it) }
            if (internalName == null) continue

            val amount = carried?.count?.takeIf { it > 1 }
                ?: label?.split("§8x")?.last()?.toIntOrNull()
                ?: 1
            EndLootFoundEvent(boss, internalName, amount).post()
        }
    }

    /**
     * Second, slower path: drops that were too far away to ever be rendered still end up in the
     * inventory when they are collected automatically. Reporting them late beats not at all.
     *
     * Drops already announced from their label are filtered out downstream, so a nearby drop is
     * not reported twice.
     */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onItemAdd(event: ItemAddEvent) {
        val boss = mostRecentBoss() ?: return
        EndLootFoundEvent(boss, event.internalName, event.amount).post()
    }

    /** First item the stand carries in any of its slots, ignoring empty ones. */
    private fun ArmorStand.carriedLoot(): SafeItemStack? = getAllEquipment()
        .firstOrNull { it != null && it.getInternalNameOrNull().let { name -> name != null && name != NeuInternalName.NONE } }

    /** Legendary is gold, epic is dark purple - see [petPattern]. */
    private fun resolvePet(label: String): NeuInternalName? = petPattern.matchMatcher(label) {
        val rarityId = if (group("rarity") == "6") LEGENDARY_RARITY else EPIC_RARITY
        val petName = group("name").trim().uppercase().replace(" ", "_")
        "$petName;$rarityId".toInternalName()
    }

    /**
     * The label alone does not say which boss a drop came from, so the most recent kill is
     * assumed. This only matters while both windows overlap, and the drop tables barely do.
     */
    private fun mostRecentBoss(): EndBoss? = activeWindows.maxByOrNull { it.value }?.key

    @HandleEvent
    private fun onIslandChange(event: IslandChangeEvent) {
        activeWindows.clear()
        seenDrops.clear()
    }
}
