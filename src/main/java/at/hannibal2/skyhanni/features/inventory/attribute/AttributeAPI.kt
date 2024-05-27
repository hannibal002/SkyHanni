package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAttributes
import net.minecraft.item.ItemStack
import java.util.regex.Pattern

object AttributeAPI {

    enum class GodRollType {
        GODROLL,
        GOOD_ROLL,
        NONE
    }

    enum class Attribute(val displayName: String, val internalName: String, val shortName: String) {
        ARACHNO("Arachno", "arachno", "AR"),
        ATTACK_SPEED("Attack Speed", "attack_speed", "AS"),
        BLAZING("Blazing", "blazing", "BL"),
        COMBO("Combo", "combo", "CO"),
        ELITE("Elite", "elite", "EL"),
        ENDER("Ender", "ender", "EN"),
        IGNITION("Ignition", "ignition", "IG"),
        LIFE_RECOVERY("Life Recovery", "life_recovery", "LR"),
        MANA_STEAL("Mana Steal", "mana_steal", "MS"),
        MIDAS_TOUCH("Midas Touch", "midas_touch", "MT"),
        UNDEAD("Undead", "undead", "UN"),
        WARRIOR("Warrior", "warrior", "WA"),
        DEADEYE("Deadeye", "deadeye", "DE"),
        ARACHNO_RESISTANCE("Arachno Resistance", "arachno_resistance", "AR"),
        BLAZING_RESISTANCE("Blazing Resistance", "blazing_resistance", "BR"),
        BREEZE("Breeze", "breeze", "BZ"),
        DOMINANCE("Dominance", "dominance", "DO"),
        ENDER_RESISTANCE("Ender Resistance", "ender_resistance", "ER"),
        EXPERIENCE("Experience", "experience", "EX"),
        FORTITUDE("Fortitude", "fortitude", "FO"),
        LIFE_REGENERATION("Life Regeneration", "life_regeneration", "LR"),
        LIFELINE("Lifeline", "lifeline", "LL"),
        MAGIC_FIND("Magic Find", "magic_find", "MF"),
        MANA_POOL("Mana Pool", "mana_pool", "MP"),
        MANA_REGENERATION("Mana Regeneration", "mana_regeneration", "MR"),
        VITALITY("Vitality", "mending", "VI"),
        SPEED("Speed", "speed", "SP"),
        UNDEAD_RESISTANCE("Undead Resistance", "undead_resistance", "UR"),
        VETERAN("Veteran", "veteran", "VE"),
        BLAZING_FORTUNE("Blazing Fortune", "blazing_fortune", "BF"),
        FISHING_EXPERIENCE("Fishing Experience", "fishing_experience", "FE"),
        INFECTION("Infection", "infection", "IN"),
        DOUBLE_HOOK("Double Hook", "double_hook", "DH"),
        FISHERMAN("Fisherman", "fisherman", "FH"),
        FISHING_SPEED("Fishing Speed", "fishing_speed", "FS"),
        HUNTER("Hunter", "hunter", "HU"),
        TROPHY_HUNTER("Trophy Hunter", "trophy_hunter", "TH"),
        ;

        override fun toString() = displayName

        companion object {
            fun getByInternalNameOrNull(internalName: String) = entries.firstOrNull { it.internalName == internalName }
        }
    }

    private data class GodRoll(
        val attributes: Pair<Attribute, Attribute>,
        val godRollItemTypes: List<GodRollItems>,
        val goodRollItemTypes: List<GodRollItems> = listOf()
    ) {
        fun isGodRoll(internalName: NEUInternalName) =
            godRollItemTypes.any { it.regex.matches(internalName.asString()) }

        fun isGoodRoll(internalName: NEUInternalName) =
            goodRollItemTypes.any { it.regex.matches(internalName.asString()) }
    }

    private data class GodRollItems(val displayName: String, val regex: Pattern)

    private fun getByName(name: String) =
        godRollItems.firstOrNull { it.displayName == name } ?: GodRollItems("", "".toPattern())

    // TODO: move to repo
    private val godRollItems = listOf<GodRollItems>(
        GodRollItems(
            "CRIMSON",
            "(?:(?:HOT|BURNING|FIERY|INFERNAL)_)?crimson_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)".toPattern()
        ),
        GodRollItems(
            "AURORA",
            "(?:(?:HOT|BURNING|FIERY|INFERNAL)_)?AURORA_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)".toPattern()
        ),
        GodRollItems(
            "TERROR",
            "(?:(?:HOT|BURNING|FIERY|INFERNAL)_)?TERROR_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)".toPattern()
        ),
        GodRollItems(
            "FERVOR",
            "(?:(?:HOT|BURNING|FIERY|INFERNAL)_)?FERVOR_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)".toPattern()
        ),
        GodRollItems(
            "MAGMA_LORD",
            "MAGMA_LORD_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)".toPattern()
        ),
        GodRollItems(
            "LAVA_ROD",
            "(?:MAGMA|INFERNO|HELLFIRE)_ROD".toPattern()
        ),
        GodRollItems(
            "FISHING_EQUIPMENT",
            "".toPattern()
        ),
        GodRollItems(
            "DUNGEON_EQUIPMENT",
            "".toPattern()
        ),
        GodRollItems(
            "COMBAT_EQUIPMENT",
            "".toPattern()
        ),
        GodRollItems(
            "MAGE_EQUIPMENT",
            "".toPattern()
        )

    )

    // TODO: move to repo
    private val godRolls = listOf<GodRoll>(
        GodRoll(
            Pair(Attribute.BLAZING_FORTUNE, Attribute.FISHING_EXPERIENCE),
            listOf(
                getByName("MAGMA_LORD"),
                getByName("FISHING_EQUIPMENT")
            )
        )
    )

    fun List<Pair<Attribute, Int>>.getGodRollType(internalName: NEUInternalName): GodRollType? {
        return godRolls.firstOrNull { godRoll -> godRoll.attributes == this.map { it.first } }?.let { godRoll ->
            when {
                godRoll.isGodRoll(internalName) -> GodRollType.GODROLL
                godRoll.isGoodRoll(internalName) -> GodRollType.GOOD_ROLL
                else -> null
            }
        }
    }

    fun ItemStack.getAttributesWithLevels(): List<Pair<Attribute, Int>>? =
        getAttributes()?.takeIf { it.isNotEmpty() }?.mapNotNull { (attr, level) ->
            Attribute.getByInternalNameOrNull(attr.lowercase())?.let { it to level }
        }

}
