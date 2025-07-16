package de.hype.bingonet.shared.objects.minions

import de.hype.bingonet.shared.compilation.sbenums.minions.*
import io.github.moulberry.repo.data.NEUItem
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AppliedMinionData(
    val minionType: MinionType,
    minionTier: Int,
    val fueles: Fueles? = null,
    val item1: MinionItem? = null,
    val item2: MinionItem? = null,
    val storage: MinionStorage? = null,
) {
    constructor(
        minion: Minion,
        fueles: Fueles? = null,
        item1: MinionItem? = null,
        item2: MinionItem? = null,
        storage: MinionStorage? = null,
    ) : this(minion.minionType, minion.minionTier, fueles, item1, item2, storage)

    fun getFullTime(): Duration {
        if (sc3000) return Duration.INFINITE
        val slots = minionData.storage / 64
        var usedSlots = 0
        var storageUsedAmount = 0
        val values = drops.entries.sortedBy { it.value }
        for (i1 in 0..<values.size - 1) {
            val usedNow = (values[i1].value * ((slots - usedSlots) * 64)).toInt()
            storageUsedAmount += usedNow
            usedSlots += (ceil(usedNow.toDouble() / 64.0).toInt())
        }
        storageUsedAmount += 64 * (slots - usedSlots)

        return (minionData.timeBetweenActions * (storageUsedAmount - 63)).seconds
    }

    val minionData: MinionData = MinionRepoManager.typeMappedMinions[minionType.typeId]!![minionTier - 1].copy()
    var sc3000: Boolean = false
    var compactor: Boolean = false
    var smelter: Boolean = false
    val category: MinionCategory = minionType.category
    val drops: MutableMap<NEUItem, Double> = HashMap(minionType.drops)


    init {
        fueles?.applyToMinion(this)
        item1?.applyToMinion(this)
        item2?.applyToMinion(this)
        storage?.applyToMinion(this)
        minionData.timeBetweenActions *= minionType.requiredActions


    }

    val simpleDisplayName
        get() = minionData.simpleDisplayName

    fun isType(minionType: MinionTypes): Boolean {
        return this.minionType.typeId == minionType.name
    }
}
