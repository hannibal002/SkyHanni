package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class SkyblockMobType(
    displayName: String? = null,
    val hypixelIcon: Char,
    val enchant: NeuInternalName? = null,
) {
    AIRBORNE(
        icon = SkyblockIcon.AIRBORNE,
        enchant = "GRAVITY".toInternalName(),
    ),
    ANIMAL(
        icon = SkyblockIcon.ANIMAL,
    ),
    AQUATIC(
        icon = SkyblockIcon.AQUATIC,
        enchant = "IMPALING".toInternalName(),
    ),
    ARCANE(
        icon = SkyblockIcon.ARCANE,
    ),
    ARTHROPOD(
        icon = SkyblockIcon.ARTHROPOD,
        enchant = "BANE_OF_ARTHROPODS".toInternalName(),
    ),
    CONSTRUCT(
        icon = SkyblockIcon.CONSTRUCT,
    ),
    CRITTER(
        icon = SkyblockIcon.CRITTER,
    ),
    CUBIC(
        icon = SkyblockIcon.CUBIC,
        enchant = "CUBISM".toInternalName(),
    ),
    ELUSIVE(
        icon = SkyblockIcon.ELUSIVE,
    ),
    ENDER(
        icon = SkyblockIcon.ENDER,
        enchant = "ENDER_SLAYER".toInternalName(),
    ),
    FROZEN(
        icon = SkyblockIcon.FROZEN,
    ),
    GLACIAL(
        icon = SkyblockIcon.GLACIAL,
        enchant = null,
    ),
    HUMANOID(
        icon = SkyblockIcon.HUMANOID,
    ),
    INFERNAL(
        icon = SkyblockIcon.INFERNAL,
        enchant = "SMOLDERING".toInternalName(),
    ),
    MAGMATIC(
        icon = SkyblockIcon.MAGMATIC,
        enchant = "PYROCLASM".toInternalName(),
    ),
    MYTHOLOGICAL(
        icon = SkyblockIcon.MYTHOLOGICAL,
    ),
    PEST(
        icon = SkyblockIcon.PEST,
    ),
    SHIELDED(
        icon = SkyblockIcon.SHIELDED,
    ),
    SKELETAL(
        icon = SkyblockIcon.SKELETAL,
        enchant = "SMITE".toInternalName(),
    ),
    SPOOKY(
        icon = SkyblockIcon.SPOOKY,
    ),
    SUBTERRANEAN(
        icon = SkyblockIcon.SUBTERRANEAN,
    ),
    UNDEAD(
        icon = SkyblockIcon.UNDEAD,
        enchant = "SMITE".toInternalName(),
    ),
    WITHER(
        icon = SkyblockIcon.WITHER,
        enchant = "SMITE".toInternalName(),
    ),
    WOODLAND(
        icon = SkyblockIcon.WOODLAND,
        enchant = "WOODSPLITTER".toInternalName(),
    ),
    ;

    val displayName = displayName ?: toFormattedName()

    constructor(displayName: String? = null, icon: SkyblockIcon, enchant: NeuInternalName? = null) : this(
        displayName,
        icon.icon,
        enchant,
    )
}
