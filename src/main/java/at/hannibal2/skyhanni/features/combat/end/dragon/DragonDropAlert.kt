package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.EndLootFoundEvent
import at.hannibal2.skyhanni.features.combat.end.RareDropAlert
import at.hannibal2.skyhanni.features.combat.end.RareDropAlert.Drop
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

/**
 * Announces the major loot of a dragon fight. Special drops also interrupt the screen with a
 * title, rare drops are only reported in chat; the ordinary drops - fragments and ender pearls -
 * are ignored entirely.
 *
 * Drops are found by [at.hannibal2.skyhanni.features.combat.end.EndLootScanner], which reads
 * their floating item labels, so nothing has to be picked up first.
 */
@SkyHanniModule
object DragonDropAlert {

    private val config get() = SkyHanniMod.feature.combat.endIsland.dragon


    private val specialDrops = mapOf(
        // NEU rarity suffixes: 3 is epic, 4 is legendary.
        "ENDER_DRAGON;4".toInternalName() to Drop(RareDropAlert.LEGENDARY, "LEGENDARY ENDER DRAGON PET"),
        "ENDER_DRAGON;3".toInternalName() to Drop(RareDropAlert.EPIC, "EPIC ENDER DRAGON PET"),
        "DRAGON_HORN".toInternalName() to Drop(RareDropAlert.EPIC, "DRAGON HORN"),
        "DYE_PEARLESCENT".toInternalName() to Drop(RareDropAlert.DYE, "PEARLESCENT DYE"),
    )

    /** Rare drops: worth reporting in chat, but not worth a title. */
    private val rareDrops = mapOf(
        "ASPECT_OF_THE_DRAGON".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Aspect of the Dragons", withTitle = false),
        "DRAGON_CLAW".toInternalName() to Drop(RareDropAlert.RARE, "Dragon Claw", withTitle = false),
        "DRAGON_SCALE".toInternalName() to Drop(RareDropAlert.RARE, "Dragon Scale", withTitle = false),
        "DRAGON_NEST_TRAVEL_SCROLL".toInternalName() to
            Drop(RareDropAlert.EPIC, "Travel Scroll to Dragon's Nest", withTitle = false),
    )

    /** Every dragon drops the same four pieces. */
    private val ARMOR_PIECES = listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")

    /**
     * Derived from [DragonType] rather than listing all 28 items: a dragon added later is covered
     * on its own, and the names are read from the repo instead of being written out again.
     */
    private val dragonArmor: Set<NeuInternalName> = DragonType.entries
        .filter { it != DragonType.UNKNOWN }
        .flatMap { type -> ARMOR_PIECES.map { "${type.name}_DRAGON_$it".toInternalName() } }
        .toSet()

    private val watchedDrops = specialDrops + rareDrops

    @HandleEvent
    private fun onEndLootFound(event: EndLootFoundEvent) {
        // No boss check: the drop tables of the two bosses do not overlap, and a drop collected
        // long after the kill cannot be attributed to a boss reliably anyway.
        if (!config.dropAlert) return
        val drop = watchedDrops[event.internalName] ?: armorDrop(event.internalName) ?: return
        RareDropAlert.show(event.internalName, drop, event.amount)
    }

    /** Resolved on the drop rather than up front, so the repo is guaranteed to be loaded. */
    private fun armorDrop(internalName: NeuInternalName): Drop? {
        if (internalName !in dragonArmor) return null
        return Drop(RareDropAlert.LEGENDARY, internalName.repoItemName.removeColor(), withTitle = false)
    }
}
