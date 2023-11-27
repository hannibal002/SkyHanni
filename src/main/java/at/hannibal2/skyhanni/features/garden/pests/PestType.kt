package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.features.combat.damageindicator.BossType

enum class PestType(val displayName: String, val damageIndicatorBoss: BossType, val spray: SprayType) {
    BEETLE("Beetle", BossType.GARDEN_PEST_BEETLE, SprayType.DUNG),
    CRICKET("Cricket", BossType.GARDEN_PEST_CRICKET, SprayType.HONEY_JAR),
    EARTHWORM("Earthworm", BossType.GARDEN_PEST_EARTHWORM, SprayType.COMPOST),
    FLY("Fly", BossType.GARDEN_PEST_FLY, SprayType.DUNG),
    LOCUST("Locust", BossType.GARDEN_PEST_LOCUST, SprayType.PLANT_MATTER),
    MITE("Mite", BossType.GARDEN_PEST_MITE, SprayType.TASTY_CHEESE),
    MOSQUITO("Mosquito", BossType.GARDEN_PEST_MOSQUITO, SprayType.COMPOST),
    MOTH("Moth", BossType.GARDEN_PEST_MOTH, SprayType.HONEY_JAR),
    RAT("Rat", BossType.GARDEN_PEST_RAT, SprayType.TASTY_CHEESE),
    SLUG("Slug", BossType.GARDEN_PEST_SLUG, SprayType.PLANT_MATTER),
}
