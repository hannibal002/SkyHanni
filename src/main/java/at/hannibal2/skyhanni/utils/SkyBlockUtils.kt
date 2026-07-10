package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.hypixelapi.HypixelLocationApi
import at.hannibal2.skyhanni.data.AreaLocationApi
import at.hannibal2.skyhanni.data.AreaType
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend
import at.hannibal2.skyhanni.test.SkyBlockIslandTest
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

object SkyBlockUtils {

    val onHypixel get() = HypixelData.connectedToHypixel && MinecraftCompat.localPlayerExists

    val isOnAlphaServer get() = onHypixel && HypixelData.hypixelAlpha

    @JvmStatic
    val inSkyBlock get() = onHypixel && HypixelLocationApi.inSkyblock

    val inHypixelLobby get() = onHypixel && HypixelData.inLobby

    /**
     * Consider using [IslandType.isInIsland] instead
     */
    val currentIsland get() = SkyBlockIslandTest.testIsland ?: HypixelLocationApi.island

    /**
     * Consider using [AreaType.isInGraphArea] instead
     */
    val graphArea get() = if (inSkyBlock) IslandAreaBackend.currentArea.takeUnless { it == AreaNode.NO_AREA } else null

    /**
     * Consider using [AreaType.isInScoreboardArea] instead
     */
    val scoreboardArea get() = if (inSkyBlock) HypixelData.skyBlockArea else null

    /**
     * Consider using [AreaType.isInArea] instead
     * Or [SkyBlockUtils.area] even
     */
    val rawArea get() = graphArea ?: scoreboardArea

    /**
     * Consider using [AreaType.isInArea] instead
     * Or [SkyBlockUtils.area] even
     */
    val area: AreaType get() = AreaLocationApi.currentArea

    val noTradeMode get() = HypixelData.noTrade

    val isStrandedProfile get() = inSkyBlock && HypixelData.stranded

    val isBingoProfile get() = inSkyBlock && (HypixelData.bingo || TestBingo.testBingo)

    val isIronmanProfile get() = inSkyBlock && HypixelData.ironman

    val lastWorldSwitch get() = HypixelData.joinedWorld

    val debug: Boolean get() = onHypixel && SkyHanniMod.feature.dev.debug.enabled

    fun inAnyIsland(vararg islandTypes: IslandType) = inSkyBlock && currentIsland in islandTypes

    fun inAnyIsland(islandTypes: Collection<IslandType>) = inSkyBlock && currentIsland in islandTypes
}
