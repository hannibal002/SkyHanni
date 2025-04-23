package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.test.SkyBlockIslandTest
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.entity.EntityLivingBase
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.util.UUID
//#if MC < 1.21
import net.minecraft.entity.SharedMonsterAttributes
//#else
//$$ import net.minecraft.entity.attribute.EntityAttributes
//#endif

object LorenzUtils {

    val connectedToHypixel get() = HypixelData.hypixelLive || HypixelData.hypixelAlpha

    val onHypixel get() = connectedToHypixel && MinecraftCompat.localPlayerExists

    val isOnAlphaServer get() = onHypixel && HypixelData.hypixelAlpha

    val inSkyBlock get() = onHypixel && HypixelData.skyBlock

    val inHypixelLobby get() = onHypixel && HypixelData.inLobby

    /**
     * Consider using [IslandType.isInIsland] instead
     */
    val skyBlockIsland get() = SkyBlockIslandTest.testIsland ?: HypixelData.skyBlockIsland

    @Deprecated("Scoreboard data is updating delayed while moving", ReplaceWith("IslandAreas.currentAreaName"))
    val skyBlockArea get() = if (inSkyBlock) HypixelData.skyBlockArea else null

    val inKuudraFight get() = inSkyBlock && KuudraApi.inKuudra()

    val noTradeMode get() = HypixelData.noTrade

    val isStrandedProfile get() = inSkyBlock && HypixelData.stranded

    val isBingoProfile get() = inSkyBlock && (HypixelData.bingo || TestBingo.testBingo)

    val isIronmanProfile get() = inSkyBlock && HypixelData.ironman

    val lastWorldSwitch get() = HypixelData.joinedWorld

    private var previousApril = false

    val isAprilFoolsDay: Boolean
        get() {
            val itsTime = LocalDate.now().let { it.month == Month.APRIL && it.dayOfMonth == 1 }
            val always = SkyHanniMod.feature.dev.debug.alwaysFunnyTime
            val never = SkyHanniMod.feature.dev.debug.neverFunnyTime
            val result = (!never && (always || itsTime))
            if (previousApril != result) {
                ModifyVisualWords.update()
            }
            previousApril = result
            return result
        }

    val debug: Boolean get() = onHypixel && SkyHanniMod.feature.dev.debug.enabled

    // TODO move into lorenz logger. then rewrite lorenz logger and use something different entirely
    fun SimpleDateFormat.formatCurrentTime(): String = this.format(System.currentTimeMillis())

    // TODO use derpy() on every use case
    val EntityLivingBase.baseMaxHealth: Int
        //#if MC < 1.21
        get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toInt()
    //#else
    //$$ get() = this.getAttributeValue(EntityAttributes.MAX_HEALTH).toInt()
    //#endif

    fun getPlayerUuid() = getRawPlayerUuid().toDashlessUUID()

    fun getRawPlayerUuid(): UUID = MinecraftCompat.localPlayer.uniqueID

    fun getPlayerName(): String = MinecraftCompat.localPlayer.name

    fun IslandType.isInIsland() = inSkyBlock && skyBlockIsland == this

    fun inAnyIsland(vararg islandTypes: IslandType) = inSkyBlock && HypixelData.skyBlockIsland in islandTypes
    fun inAnyIsland(islandTypes: Collection<IslandType>) = inSkyBlock && HypixelData.skyBlockIsland in islandTypes
}
