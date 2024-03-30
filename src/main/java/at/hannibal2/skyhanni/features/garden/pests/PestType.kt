package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName

enum class PestType(val displayName: String, val damageIndicatorBoss: BossType, val spray: SprayType, val vinyl: VinylType, val internalName: NEUInternalName) {
    BEETLE("Beetle", BossType.GARDEN_PEST_BEETLE, SprayType.DUNG, VinylType.NOT_JUST_A_PEST, "PEST_BEETLE_MONSTER".asInternalName()),
    CRICKET("Cricket", BossType.GARDEN_PEST_CRICKET, SprayType.HONEY_JAR, VinylType.CRICKET_CHOIR, "PEST_CRICKET_MONSTER".asInternalName()),
    EARTHWORM("Earthworm", BossType.GARDEN_PEST_EARTHWORM, SprayType.COMPOST, VinylType.EARTHWORM_ENSEMBLE, "PEST_EARTHWORM_MONSTER".asInternalName()),
    FLY("Fly", BossType.GARDEN_PEST_FLY, SprayType.DUNG, VinylType.PRETTY_FLY, "PEST_FLY_MONSTER".asInternalName()),
    LOCUST("Locust", BossType.GARDEN_PEST_LOCUST, SprayType.PLANT_MATTER, VinylType.CICADA_SYMPHONY, "PEST_LOCUST_MONSTER".asInternalName()),
    MITE("Mite", BossType.GARDEN_PEST_MITE, SprayType.TASTY_CHEESE, VinylType.DYNAMITES, "PEST_MITE_MONSTER".asInternalName()),
    MOSQUITO("Mosquito", BossType.GARDEN_PEST_MOSQUITO, SprayType.COMPOST, VinylType.BUZZIN_BEATS, "PEST_MOSQUITO_MONSTER".asInternalName()),
    MOTH("Moth", BossType.GARDEN_PEST_MOTH, SprayType.HONEY_JAR, VinylType.WINGS_OF_HARMONY, "PEST_MOTH_MONSTER".asInternalName()),
    RAT("Rat", BossType.GARDEN_PEST_RAT, SprayType.TASTY_CHEESE, VinylType.RODENT_REVOLUTION, "PEST_RAT_MONSTER".asInternalName()),
    SLUG("Slug", BossType.GARDEN_PEST_SLUG, SprayType.PLANT_MATTER, VinylType.SLOW_AND_GROOVY, "PEST_SLUG_MONSTER".asInternalName()),
}
