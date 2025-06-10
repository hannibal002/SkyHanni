package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.hypixelapi.HypixelLocationApi
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
//#if TODO
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.test.SkyBlockIslandTest
//#endif
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

// todo 1.21 impl needed
object SkyBlockUtils {

    private val isModApiDetection get() = HypixelLocationApi.isModApiDetection

    val onHypixel: Boolean
        get() {
            val inHypixel = if (isModApiDetection) HypixelLocationApi.inHypixel else HypixelData.connectedToHypixel
            return inHypixel && MinecraftCompat.localPlayerExists
        }

    val isOnAlphaServer: Boolean
        get() {
            val inAlpha = if (isModApiDetection) HypixelLocationApi.inAlpha else HypixelData.hypixelAlpha
            return onHypixel && inAlpha
        }

    val inSkyBlock: Boolean
        get() {
            val inSkyblock = if (isModApiDetection) HypixelLocationApi.inSkyblock else HypixelData.skyBlock
            return onHypixel && inSkyblock
        }

    val inHypixelLobby: Boolean
        get() {
            //#if TODO
            val inLobby = if (isModApiDetection) HypixelLocationApi.inLobby else HypixelData.inLobby
            return onHypixel && inLobby
            //#else
            //$$ return onHypixel && HypixelData.inLobby
            //#endif
        }

    //#if TODO
    /**
     * Consider using [IslandType.isInIsland] instead
     */
    val currentIsland: IslandType
        get() {
            val island = if (isModApiDetection) HypixelLocationApi.island else HypixelData.skyBlockIsland
            return SkyBlockIslandTest.testIsland ?: island
        }

    // almost always prefer this over scoreboardArea
    val graphArea get() = if (inSkyBlock) IslandAreas.currentArea else null
    //#else
    //$$ val currentIsland get() = HypixelData.skyBlockIsland
    //#endif

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
