package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.EndLootFoundEvent
import at.hannibal2.skyhanni.features.combat.end.RareDropAlert
import at.hannibal2.skyhanni.features.combat.end.RareDropAlert.Drop
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

/**
 * Announces the major loot of an End Stone Protector fight. Special drops also interrupt the
 * screen with a title, rare drops are only reported in chat; the ordinary drops - shards,
 * enchanted end stone, crystal fragments and roses - are ignored entirely.
 *
 * Drops are found by [at.hannibal2.skyhanni.features.combat.end.EndLootScanner], which reads
 * their floating item labels, so nothing has to be picked up first.
 */
@SkyHanniModule
object GolemDropAlert {

    private val config get() = SkyHanniMod.feature.combat.endIsland.golem

    /** Special drops: rare enough to be worth covering the screen for. */
    private val specialDrops = mapOf(
        "PET_ITEM_TIER_BOOST_DROP".toInternalName() to Drop(RareDropAlert.LEGENDARY, "TIER BOOST CORE"),
        "DYE_PEARLESCENT".toInternalName() to Drop(RareDropAlert.DYE, "PEARLESCENT DYE"),
    )

    /** Rare drops: worth reporting in chat, but not worth a title. */
    private val rareDrops = mapOf(
        // NEU rarity suffixes: 3 is epic, 4 is legendary.
        "GOLEM;4".toInternalName() to Drop(RareDropAlert.LEGENDARY, "LEGENDARY GOLEM PET", withTitle = false),
        "GOLEM;3".toInternalName() to Drop(RareDropAlert.EPIC, "EPIC GOLEM PET", withTitle = false),
    )

    private val watchedDrops = specialDrops + rareDrops


    @HandleEvent
    private fun onEndLootFound(event: EndLootFoundEvent) {
        // No boss check - see the note in DragonDropAlert.
        if (!config.dropAlert) return
        val drop = watchedDrops[event.internalName] ?: return
        RareDropAlert.show(event.internalName, drop, event.amount)
    }
}
