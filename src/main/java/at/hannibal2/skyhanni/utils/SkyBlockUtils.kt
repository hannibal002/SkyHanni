package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.test.SkyBlockIslandTest
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

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

    val debug: Boolean get() = onHypixel && SkyHanniMod.feature.dev.debug.enabled

    fun inAnyIsland(vararg islandTypes: IslandType) = inSkyBlock && currentIsland in islandTypes


    fun inAnyIsland(islandTypes: Collection<IslandType>) = inSkyBlock && currentIsland in islandTypes
}
