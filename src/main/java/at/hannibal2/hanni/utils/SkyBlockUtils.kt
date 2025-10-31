package at.hannibal2.hanni.utils

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.features.misc.IslandAreas
import at.hannibal2.hanni.test.SkyBlockIslandTest
import at.hannibal2.hanni.test.TestBingo
import at.hannibal2.hanni.utils.compat.MinecraftCompat

object SkyBlockUtils {

    val onHypixel get() = HypixelData.connectedToHypixel && MinecraftCompat.localPlayerExists

    val isOnAlphaServer get() = onHypixel && HypixelData.hypixelAlpha

    val inSkyBlock get() = onHypixel && HypixelData.skyBlock

    val inHypixelLobby get() = onHypixel && HypixelData.inLobby

    /**
     * Consider using [IslandType.isInIsland] instead
     */
    val currentIsland get() = SkyBlockIslandTest.testIsland ?: HypixelData.skyBlockIsland

    // almost always prefer this over scoreboardArea
    val graphArea get() = if (inSkyBlock) IslandAreas.currentArea else null

    // Only use scoreboardArea if graph data is not useable in this scenario.
    val scoreboardArea get() = if (inSkyBlock) HypixelData.skyBlockArea else null

    val noTradeMode get() = HypixelData.noTrade

    val isStrandedProfile get() = inSkyBlock && HypixelData.stranded

    val isBingoProfile get() = inSkyBlock && (HypixelData.bingo || TestBingo.testBingo)

    val isIronmanProfile get() = inSkyBlock && HypixelData.ironman

    val lastWorldSwitch get() = HypixelData.joinedWorld

    val debug: Boolean get() = onHypixel && HanniMod.feature.dev.debug.enabled

    fun inAnyIsland(vararg islandTypes: IslandType) = inSkyBlock && currentIsland in islandTypes

    fun inAnyIsland(islandTypes: Collection<IslandType>) = inSkyBlock && currentIsland in islandTypes
}
