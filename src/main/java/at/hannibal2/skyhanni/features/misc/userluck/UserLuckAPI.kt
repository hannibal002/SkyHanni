package at.hannibal2.skyhanni.features.misc.userluck

object UserLuckAPI {
    val luck get() = UserLuckMultiplier.totalLuckAfterBonus(UserLuckType.getTotalLuck())
}
