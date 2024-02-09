package at.hannibal2.skyhanni.utils.repopatterns

import kotlin.reflect.KProperty

data class RepoPatternKeyOwner(
    val ownerClass: Class<*>?,
    val property: KProperty<*>?,
)
