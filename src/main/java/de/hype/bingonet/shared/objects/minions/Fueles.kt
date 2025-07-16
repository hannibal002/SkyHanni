package de.hype.bingonet.shared.objects.minions

import de.hype.bingonet.shared.compilation.extensionutils.modifyValues
import de.hype.bingonet.shared.compilation.sbenums.minions.MinionCategory

enum class Fueles(
    val durationMinutes: Int,
    val isBingoObtainable: Boolean = true,
    private val builder: (AppliedMinionData) -> Unit
) {
    COAL(30, builder = {
        it.minionData.timeBetweenActions *= 0.95
    }),
    CHARCOAL(30, builder = {
        it.minionData.timeBetweenActions *= 0.95
    }),
    BLOCK_OF_COAL(5 * 60, builder = {
        it.minionData.timeBetweenActions *= 0.95
    }),
    ENCHANTED_COAL(24 * 60, builder = {
        it.minionData.timeBetweenActions *= 0.90
    }),
    ENCHANTED_CHAR_COAL(36 * 60, builder = {
        it.minionData.timeBetweenActions *= 0.80
    }),
    HAMSTER_WHEEL(24 * 60, builder = {
        it.minionData.timeBetweenActions *= 0.5
    }),
    FOUL_FLESH(5 * 60, builder = {
        it.minionData.timeBetweenActions *= 0.1
    }),
    ENCHANTED_BREAD(12 * 60, builder = {
        it.minionData.timeBetweenActions *= 0.95
    }),
    CATALYST(3 * 60, builder = {
        multiplyDrops(it, 3.0)
    }),
    HYPER_CATALYST(6 * 60, false, builder = {
        multiplyDrops(it, 4.0)
    }),
    TASTY_CHESSE(1 * 60, builder = {
        multiplyDrops(it, 2.0)
    }),
    SOLAR_PANEL(-1, false, builder = {
        it.minionData.timeBetweenActions *= 0.75
    }),
    ENCHANTED_LAVA_BUCKET(-1, builder = {
        it.minionData.timeBetweenActions *= 0.75
    }),
    MAGMA_BUCKET(-1, false, builder = {
        it.minionData.timeBetweenActions *= 0.70
    }),
    PLASMA_BUCKET(-1, false, builder = {
        it.minionData.timeBetweenActions *= 0.65
    }),
    EVER_BURNING_FLAME(-1, false, builder = {
        if (it.category == MinionCategory.COMBAT) {
            it.minionData.timeBetweenActions *= 0.60
        } else {
            it.minionData.timeBetweenActions *= 0.65
        }
    });

    companion object {
        fun multiplyDrops(minionData: AppliedMinionData, multiplier: Double) {
            minionData.drops.modifyValues { drop ->
                return@modifyValues drop.value * multiplier
            }
        }
    }

    fun applyToMinion(minionData: AppliedMinionData) {
        builder.invoke(minionData)
    }
}

