package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.EndLootFoundEvent
import at.hannibal2.skyhanni.features.combat.end.RareDropAlert
import at.hannibal2.skyhanni.features.combat.end.RareDropAlert.Drop
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

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

    /** Special drops: rare enough to be worth covering the screen for. */
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

    /**
     * Listed by hand rather than derived from [DragonType]: a dragon added in the future will
     * not be tracked until its four pieces are added here.
     */
    private val dragonArmor = mapOf(
        "PROTECTOR_DRAGON_HELMET".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Protector Dragon Helmet", withTitle = false),
        "PROTECTOR_DRAGON_CHESTPLATE".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Protector Dragon Chestplate", withTitle = false),
        "PROTECTOR_DRAGON_LEGGINGS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Protector Dragon Leggings", withTitle = false),
        "PROTECTOR_DRAGON_BOOTS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Protector Dragon Boots", withTitle = false),
        "OLD_DRAGON_HELMET".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Old Dragon Helmet", withTitle = false),
        "OLD_DRAGON_CHESTPLATE".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Old Dragon Chestplate", withTitle = false),
        "OLD_DRAGON_LEGGINGS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Old Dragon Leggings", withTitle = false),
        "OLD_DRAGON_BOOTS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Old Dragon Boots", withTitle = false),
        "UNSTABLE_DRAGON_HELMET".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Unstable Dragon Helmet", withTitle = false),
        "UNSTABLE_DRAGON_CHESTPLATE".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Unstable Dragon Chestplate", withTitle = false),
        "UNSTABLE_DRAGON_LEGGINGS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Unstable Dragon Leggings", withTitle = false),
        "UNSTABLE_DRAGON_BOOTS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Unstable Dragon Boots", withTitle = false),
        "YOUNG_DRAGON_HELMET".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Young Dragon Helmet", withTitle = false),
        "YOUNG_DRAGON_CHESTPLATE".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Young Dragon Chestplate", withTitle = false),
        "YOUNG_DRAGON_LEGGINGS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Young Dragon Leggings", withTitle = false),
        "YOUNG_DRAGON_BOOTS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Young Dragon Boots", withTitle = false),
        "STRONG_DRAGON_HELMET".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Strong Dragon Helmet", withTitle = false),
        "STRONG_DRAGON_CHESTPLATE".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Strong Dragon Chestplate", withTitle = false),
        "STRONG_DRAGON_LEGGINGS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Strong Dragon Leggings", withTitle = false),
        "STRONG_DRAGON_BOOTS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Strong Dragon Boots", withTitle = false),
        "WISE_DRAGON_HELMET".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Wise Dragon Helmet", withTitle = false),
        "WISE_DRAGON_CHESTPLATE".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Wise Dragon Chestplate", withTitle = false),
        "WISE_DRAGON_LEGGINGS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Wise Dragon Leggings", withTitle = false),
        "WISE_DRAGON_BOOTS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Wise Dragon Boots", withTitle = false),
        "SUPERIOR_DRAGON_HELMET".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Superior Dragon Helmet", withTitle = false),
        "SUPERIOR_DRAGON_CHESTPLATE".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Superior Dragon Chestplate", withTitle = false),
        "SUPERIOR_DRAGON_LEGGINGS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Superior Dragon Leggings", withTitle = false),
        "SUPERIOR_DRAGON_BOOTS".toInternalName() to
            Drop(RareDropAlert.LEGENDARY, "Superior Dragon Boots", withTitle = false),
    )

    private val watchedDrops = specialDrops + rareDrops + dragonArmor

    @HandleEvent
    private fun onEndLootFound(event: EndLootFoundEvent) {
        // No boss check: the drop tables of the two bosses do not overlap, and a drop collected
        // long after the kill cannot be attributed to a boss reliably anyway.
        if (!config.dropAlert) return
        // Everything else is caught by its rarity colour, so armour and weapons are reported
        // even when the label cannot be resolved to a known item.
        val drop = watchedDrops[event.internalName] ?: return
        RareDropAlert.show(event.internalName, drop, event.amount)
    }
}
