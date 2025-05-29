package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.test.SkyBlockIslandTest
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.entity.EntityLivingBase
import java.text.SimpleDateFormat
import java.util.UUID
//#if MC < 1.21
import net.minecraft.entity.SharedMonsterAttributes
//#else
//$$ import net.minecraft.entity.attribute.EntityAttributes
//#endif

// Dont use. prefer SkyBlockUtils or other utils classes
@Suppress("DEPRECATION")
@Deprecated("Use SkyBlockUtils or other utils classes. 450 usages prevent us from doing so in one push.")
object LorenzUtils {

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.onHypixel"))
    val onHypixel get() = HypixelData.connectedToHypixel && MinecraftCompat.localPlayerExists

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.isOnAlphaServer"))
    val isOnAlphaServer get() = onHypixel && HypixelData.hypixelAlpha

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.inSkyBlock"))
    val inSkyBlock get() = onHypixel && HypixelData.skyBlock

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.inHypixelLobby"))
    val inHypixelLobby get() = onHypixel && HypixelData.inLobby

    /**
     * Consider using [IslandType.isInIsland] instead
     */
    @Deprecated("moved", ReplaceWith("SkyBlockUtils.currentIsland"))
    val skyBlockIsland get() = SkyBlockIslandTest.testIsland ?: HypixelData.skyBlockIsland

    @Deprecated("Scoreboard data is updating delayed while moving, dont use.", ReplaceWith("SkyBlockUtils.graphArea"))
    val skyBlockArea get() = scoreboardArea

    // almost always prefer this over scoreboardArea
    @Deprecated("moved", ReplaceWith("SkyBlockUtils.graphArea"))
    val graphArea get() = if (inSkyBlock) IslandAreas.currentArea else null

    // Only use scoreboardArea if graph data is not useable in this scenario.
    @Deprecated("moved", ReplaceWith("SkyBlockUtils.scoreboardArea"))
    val scoreboardArea get() = if (inSkyBlock) HypixelData.skyBlockArea else null

    @Deprecated("moved", ReplaceWith("KuudraApi.inKuudra"))
    val inKuudraFight get() = inSkyBlock && KuudraApi.kuudraTier != null

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.noTradeMode"))
    val noTradeMode get() = HypixelData.noTrade

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.isStrandedProfile"))
    val isStrandedProfile get() = inSkyBlock && HypixelData.stranded

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.isBingoProfile"))
    val isBingoProfile get() = inSkyBlock && (HypixelData.bingo || TestBingo.testBingo)

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.isIronmanProfile"))
    val isIronmanProfile get() = inSkyBlock && HypixelData.ironman

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.lastWorldSwitch"))
    val lastWorldSwitch get() = HypixelData.joinedWorld

    @Deprecated("moved", ReplaceWith("SkyHanniDebugsAndTests.isAprilFoolsDay"))
    val isAprilFoolsDay get() = SkyHanniDebugsAndTests.isAprilFoolsDay

    @Deprecated("moved", ReplaceWith("SkyHanniDebugsAndTests.enabled"))
    val debug: Boolean get() = onHypixel && SkyHanniMod.feature.dev.debug.enabled

    // TODO move into lorenz logger. then rewrite lorenz logger and use something different entirely
    @Deprecated("moved to TimeUtils", ReplaceWith(""))
    fun SimpleDateFormat.formatCurrentTime(): String = this.format(System.currentTimeMillis())

    // TODO use derpy() on every use case
    @Deprecated("moved to EntityUtils", ReplaceWith(""))
    val EntityLivingBase.baseMaxHealth: Int
        //#if MC < 1.21
        get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toInt()
    //#else
    //$$ get() = this.getAttributeValue(EntityAttributes.MAX_HEALTH).toInt()
    //#endif

    @Deprecated("moved", ReplaceWith("PlayerUtils.getUuid()"))
    fun getPlayerUuid() = getRawPlayerUuid().toDashlessUUID()

    @Deprecated("moved", ReplaceWith("PlayerUtils.getRawUuid()"))
    fun getRawPlayerUuid(): UUID = MinecraftCompat.localPlayer.uniqueID

    @Deprecated("moved", ReplaceWith("PlayerUtils.getName()"))
    fun getPlayerName(): String = MinecraftCompat.localPlayer.name

    @Deprecated("moved into IslandType", ReplaceWith("this.isCurrent()"))
    fun IslandType.isInIsland() = inSkyBlock && skyBlockIsland == this

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.inAnyIsland(islandTypes)"))
    fun inAnyIsland(vararg islandTypes: IslandType) = inSkyBlock && HypixelData.skyBlockIsland in islandTypes

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.inAnyIsland(islandTypes)"))
    fun inAnyIsland(islandTypes: Collection<IslandType>) = inSkyBlock && HypixelData.skyBlockIsland in islandTypes
}
