package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName

enum class SkyblockMobType(
    displayName: String? = null,
    val hypixelIcon: SkyblockIcon,
) {
    AIRBORNE(
        hypixelIcon = SkyblockIcon.AIRBORNE,
    ),
    ANIMAL(
        hypixelIcon = SkyblockIcon.ANIMAL,
    ),
    AQUATIC(
        hypixelIcon = SkyblockIcon.AQUATIC,
    ),
    ARCANE(
        hypixelIcon = SkyblockIcon.ARCANE,
    ),
    ARTHROPOD(
        hypixelIcon = SkyblockIcon.ARTHROPOD,
    ),
    CONSTRUCT(
        hypixelIcon = SkyblockIcon.CONSTRUCT,
    ),
    CRITTER(
        hypixelIcon = SkyblockIcon.CRITTER,
    ),
    CUBIC(
        hypixelIcon = SkyblockIcon.CUBIC,
    ),
    ELUSIVE(
        hypixelIcon = SkyblockIcon.ELUSIVE,
    ),
    ENDER(
        hypixelIcon = SkyblockIcon.ENDER,
    ),
    FROZEN(
        hypixelIcon = SkyblockIcon.FROZEN,
    ),
    GLACIAL(
        hypixelIcon = SkyblockIcon.GLACIAL,
    ),
    HUMANOID(
        hypixelIcon = SkyblockIcon.HUMANOID,
    ),
    INFERNAL(
        hypixelIcon = SkyblockIcon.INFERNAL,
    ),
    MAGMATIC(
        hypixelIcon = SkyblockIcon.MAGMATIC,
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
    ),
    SPOOKY(
        hypixelIcon = SkyblockIcon.SPOOKY,
    ),
    SUBTERRANEAN(
        hypixelIcon = SkyblockIcon.SUBTERRANEAN,
    ),
    UNDEAD(
        hypixelIcon = SkyblockIcon.UNDEAD,
    ),
    WITHER(
        hypixelIcon = SkyblockIcon.WITHER,
    ),
    WOODLAND(
        hypixelIcon = SkyblockIcon.WOODLAND,
    ),
    ;

    val displayName = displayName ?: toFormattedName()
}
