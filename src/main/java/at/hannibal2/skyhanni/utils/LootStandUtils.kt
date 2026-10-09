package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getAllEquipment
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand

/**
 * Reads dropped loot off the armor stands SkyBlock builds it from. One drop is made of several
 * stands: one wears the item, another carries the floating label. Either one names the item, so
 * both are understood here.
 *
 * Such loot has no item entity behind it - measured in game - which is why the stands are the
 * source at all. A feature listens to `EntityEquipmentChangeEvent` and `EntityCustomNameUpdateEvent`
 * for armor stands and passes the entity to [readLoot]; when a drop counts, and to what, stays with
 * the caller.
 */
@SkyHanniModule
object LootStandUtils {

    private val patternGroup = RepoPattern.group("utils.loot-stand")

    /**
     * Pet labels carry no rarity word - epic and legendary read exactly the same and differ only
     * in colour, so the generic name resolver cannot tell them apart. The colour is therefore
     * turned into the rarity suffix here.
     *
     * REGEX-TEST: §7[Lvl 1] §6Golem
     * REGEX-TEST: §7[Lvl 1] §5Ender Dragon
     * REGEX-TEST: §7[Lvl 100] §6Ender Dragon
     */
    private val petPattern by patternGroup.pattern(
        "pet",
        "§7\\[Lvl \\d+] §(?<rarity>[56])(?<name>.+)",
    )

    /**
     * Stacked drops carry their amount in the label, behind the item name.
     *
     * REGEX-TEST: §5Dragon Claw §8x3
     * REGEX-TEST: §aEnchanted Ender Pearl §8x16
     */
    private val amountPattern by patternGroup.pattern(
        "amount",
        ".*§8x(?<amount>[\\d,]+)",
    )

    /** NEU rarity suffixes. */
    private const val EPIC_RARITY = 3
    private const val LEGENDARY_RARITY = 4

    /** @param amount how many items dropped at once, 1 unless the label says otherwise */
    data class StandLoot(val internalName: NeuInternalName, val amount: Int)

    /**
     * The loot this stand stands for, or null while it carries neither a known item nor a readable
     * label. That covers every other armor stand in the world, and a loot stand whose second packet
     * has not arrived yet - so a stand is worth asking again on its next update.
     *
     * The item stack names its item exactly and is used wherever it can be. Pets are the exception:
     * every rarity of a pet shares one skull, so resolving the stack names the pet but guesses the
     * rarity. That one is only ever a colour, and it is read from the item's own name - which comes
     * with the equipment packet, far further out than the label above the drop.
     */
    fun ArmorStand.readLoot(): StandLoot? {
        val carried = carriedItem()
        val fromStack = carried?.getInternalNameOrNull()?.takeIf { it != NeuInternalName.NONE }
        val label = if (hasCustomName()) name.formattedTextCompatLessResets() else null

        // The item's own name first, the label above the drop second: both carry the rarity as a
        // colour, but the item arrives with its packet while the label needs the player to be near.
        val pet = carried?.let { resolvePet(it.hoverName.formattedTextCompatLessResets()) }
            ?: label?.let { resolvePet(it) }

        val internalName = when {
            pet != null -> pet
            // Neither name has arrived, and resolving the stack alone would guess the rarity.
            // Waiting costs nothing: the stand is read again with its next packet.
            fromStack?.isPet == true -> return null
            fromStack != null -> fromStack
            else -> label?.let { NeuInternalName.fromItemNameOrNull(it) } ?: return null
        }

        val amount = carried?.count?.takeIf { it > 1 }
            ?: label?.let { amountPattern.matchMatcher(it) { group("amount").formatIntOrNull() } }
            ?: 1
        return StandLoot(internalName, amount)
    }

    /**
     * First item the stand carries in any of its slots, ignoring empty ones. Every slot is checked,
     * not just head and hand: an armor piece sits in the slot it belongs to, which is why armor was
     * previously only ever found through its label.
     */
    private fun ArmorStand.carriedItem(): SafeItemStack? = getAllEquipment()
        .firstOrNull { it != null && it.getInternalNameOrNull().let { name -> name != null && name != NeuInternalName.NONE } }

    /** Legendary is gold, epic is dark purple - see [petPattern]. */
    private fun resolvePet(label: String): NeuInternalName? = petPattern.matchMatcher(label) {
        val rarityId = if (group("rarity") == "6") LEGENDARY_RARITY else EPIC_RARITY
        val petName = group("name").trim().uppercase().replace(" ", "_")
        "$petName;$rarityId".toInternalName()
    }
}
