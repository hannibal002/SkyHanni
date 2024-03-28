package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.HotmData

object HotmAPI {

    fun copyCurrentTree() = HotmData.storage?.deepCopy()

    val activeMiningAbility get() = HotmData.abilities.firstOrNull { it.enabled }

    object Powder {

        val currentMithril get() = HotmData.availableMithrilPowder

        val currentGemstone get() = HotmData.availableGemstonePowder

        val maxMithril get() = HotmData.mithrilPowder

        val maxGemstone get() = HotmData.gemstonePowder
    }
}
