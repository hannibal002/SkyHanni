package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.features.combat.damageindicator.BossType

enum class PestType(val displayName: String, val damageIndicatorBoss: BossType) {
    BEETLE("Beetle", BossType.GARDEN_PEST_BEETLE),
    CRICKET("Cricket", BossType.GARDEN_PEST_CRICKET),
    EARTHWORM("Earthworm", BossType.GARDEN_PEST_EARTHWORM),
    FLY("Fly", BossType.GARDEN_PEST_FLY),
    LOCTUS("Loctus", BossType.GARDEN_PEST_LOCUST),
    MITE("Mite", BossType.GARDEN_PEST_MITE),
    MOSQUITO("Mosquito", BossType.GARDEN_PEST_MOSQUITO),
    MOTH("Moth", BossType.GARDEN_PEST_MOTH),
    RAT("Rat", BossType.GARDEN_PEST_RAT),
    SLUG("Slug", BossType.GARDEN_PEST_SLUG),
}
