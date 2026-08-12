package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class SkyblockMobType(
    displayName: String? = null,
    val hypixelIcon: SkyblockIcon,
    val enchant: NeuInternalName? = null,
) {
    AIRBORNE(
        hypixelIcon = SkyblockIcon.AIRBORNE,
        enchant = "GRAVITY".toInternalName(),
    ),
    ANIMAL(
        hypixelIcon = SkyblockIcon.ANIMAL,
    ),
    AQUATIC(
        hypixelIcon = SkyblockIcon.AQUATIC,
        enchant = "IMPALING".toInternalName(),
    ),
    ARCANE(
        hypixelIcon = SkyblockIcon.ARCANE,
    ),
    ARTHROPOD(
        hypixelIcon = SkyblockIcon.ARTHROPOD,
        enchant = "BANE_OF_ARTHROPODS".toInternalName(),
    ),
    CONSTRUCT(
        hypixelIcon = SkyblockIcon.CONSTRUCT,
    ),
    CRITTER(
        hypixelIcon = SkyblockIcon.CRITTER,
    ),
    CUBIC(
        hypixelIcon = SkyblockIcon.CUBIC,
        enchant = "CUBISM".toInternalName(),
    ),
    ELUSIVE(
        hypixelIcon = SkyblockIcon.ELUSIVE,
    ),
    ENDER(
        hypixelIcon = SkyblockIcon.ENDER,
        enchant = "ENDER_SLAYER".toInternalName(),
    ),
    FROZEN(
        hypixelIcon = SkyblockIcon.FROZEN,
    ),
    GLACIAL(
        hypixelIcon = SkyblockIcon.GLACIAL,
        enchant = null,
    ),
    HUMANOID(
        hypixelIcon = SkyblockIcon.HUMANOID,
    ),
    INFERNAL(
        hypixelIcon = SkyblockIcon.INFERNAL,
        enchant = "SMOLDERING".toInternalName(),
    ),
    MAGMATIC(
        hypixelIcon = SkyblockIcon.MAGMATIC,
        enchant = "PYROCLASM".toInternalName(),
    ),
    MYTHOLOGICAL(
        hypixelIcon = SkyblockIcon.MYTHOLOGICAL,
    ),
    PEST(
        hypixelIcon = SkyblockIcon.PEST,
    ),
    SHIELDED(
        hypixelIcon = SkyblockIcon.SHIELDED,
    ),
    SKELETAL(
        hypixelIcon = SkyblockIcon.SKELETAL,
        enchant = "SMITE".toInternalName(),
    ),
    SPOOKY(
        hypixelIcon = SkyblockIcon.SPOOKY,
    ),
    SUBTERRANEAN(
        hypixelIcon = SkyblockIcon.SUBTERRANEAN,
    ),
    UNDEAD(
        hypixelIcon = SkyblockIcon.UNDEAD,
        enchant = "SMITE".toInternalName(),
    ),
    WITHER(
        hypixelIcon = SkyblockIcon.WITHER,
        enchant = "SMITE".toInternalName(),
    ),
    WOODLAND(
        hypixelIcon = SkyblockIcon.WOODLAND,
        enchant = "WOODSPLITTER".toInternalName(),
    ),
    ;

    val displayName = displayName ?: toFormattedName()

    override fun toString(): String = hypixelIcon.toString()
}
