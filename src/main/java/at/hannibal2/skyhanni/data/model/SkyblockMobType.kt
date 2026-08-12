package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class SkyblockMobType(
    val displayName: String,
    val hypixelIcon: Char,
    val enchant: NeuInternalName? = null,
) {
    AIRBORNE(
        displayName = "Airborne",
        icon = SkyblockIcon.AIRBORNE,
        enchant = "GRAVITY".toInternalName(),
    ),
    ANIMAL(
        displayName = "Animal",
        icon = SkyblockIcon.ANIMAL,
    ),
    AQUATIC(
        displayName = "Aquatic",
        icon = SkyblockIcon.AQUATIC,
        enchant = "IMPALING".toInternalName(),
    ),
    ARCANE(
        displayName = "Arcane",
        icon = SkyblockIcon.ARCANE,
    ),
    ARTHROPOD(
        displayName = "Arthropod",
        icon = SkyblockIcon.ARTHROPOD,
        enchant = "BANE_OF_ARTHROPODS".toInternalName(),
    ),
    CONSTRUCT(
        displayName = "Construct",
        icon = SkyblockIcon.CONSTRUCT,
    ),
    CRITTER(
        displayName = "Critter",
        icon = SkyblockIcon.CRITTER,
    ),
    CUBIC(
        displayName = "Cubic",
        icon = SkyblockIcon.CUBIC,
        enchant = "CUBISM".toInternalName(),
    ),
    ELUSIVE(
        displayName = "Elusive",
        icon = SkyblockIcon.ELUSIVE,
    ),
    ENDER(
        displayName = "Ender",
        icon = SkyblockIcon.ENDER,
        enchant = "ENDER_SLAYER".toInternalName(),
    ),
    FROZEN(
        displayName = "Frozen",
        icon = SkyblockIcon.FROZEN,
    ),
    GLACIAL(
        displayName = "Glacial",
        icon = SkyblockIcon.GLACIAL,
        enchant = null,
    ),
    HUMANOID(
        displayName = "Humanoid",
        icon = SkyblockIcon.HUMANOID,
    ),
    INFERNAL(
        displayName = "Infernal",
        icon = SkyblockIcon.INFERNAL,
        enchant = "SMOLDERING".toInternalName(),
    ),
    MAGMATIC(
        displayName = "Magmatic",
        icon = SkyblockIcon.MAGMATIC,
        enchant = "PYROCLASM".toInternalName(),
    ),
    MYTHOLOGICAL(
        displayName = "Mythological",
        icon = SkyblockIcon.MYTHOLOGICAL,
    ),
    PEST(
        displayName = "Pest",
        icon = SkyblockIcon.PEST,
    ),
    SHIELDED(
        displayName = "Shielded",
        icon = SkyblockIcon.SHIELDED,
    ),
    SKELETAL(
        displayName = "Skeletal",
        icon = SkyblockIcon.SKELETAL,
        enchant = "SMITE".toInternalName(),
    ),
    SPOOKY(
        displayName = "Spooky",
        icon = SkyblockIcon.SPOOKY,
    ),
    SUBTERRANEAN(
        displayName = "Subterranean",
        icon = SkyblockIcon.SUBTERRANEAN,
    ),
    UNDEAD(
        displayName = "Undead",
        icon = SkyblockIcon.UNDEAD,
        enchant = "SMITE".toInternalName(),
    ),
    WITHER(
        displayName = "Wither",
        icon = SkyblockIcon.WITHER,
        enchant = "SMITE".toInternalName(),
    ),
    WOODLAND(
        displayName = "Woodland",
        icon = SkyblockIcon.WOODLAND,
        enchant = "WOODSPLITTER".toInternalName(),
    ),
    ;

    constructor(displayName: String, icon: SkyblockIcon, enchant: NeuInternalName? = null) : this(
        displayName,
        icon.icon,
        enchant,
    )
}
