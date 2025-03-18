package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.test.SkyBlockIslandTest
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

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
        get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toInt()

    // TODO create extension function
    fun formatPercentage(percentage: Double): String = formatPercentage(percentage, "0.00")

    fun formatPercentage(percentage: Double, format: String?): String =
        DecimalFormat(format).format(percentage * 100).replace(',', '.') + "%"

    // TODO move into chat utils
    fun consoleLog(text: String) {
        SkyHanniMod.consoleLog(text)
    }

    fun getPlayerUuid() = getRawPlayerUuid().toDashlessUUID()

    fun getRawPlayerUuid(): UUID = MinecraftCompat.localPlayer.uniqueID

    fun getPlayerName(): String = MinecraftCompat.localPlayer.name

    // TODO move into renderable utils
    fun fillTable(
        data: List<DisplayTableEntry>,
        padding: Int = 1,
        itemScale: Double = NeuItems.ITEM_FONT_SIZE,
    ): Renderable {
        val sorted = data.sortedByDescending { it.sort }

        val outerList = mutableListOf<List<Renderable>>()
        for (entry in sorted) {
            val item = entry.item.getItemStackOrNull()?.let {
                Renderable.itemStack(it, scale = itemScale)
            } ?: continue
            val left = Renderable.hoverTips(
                entry.left,
                tips = entry.hover,
                highlightsOnHoverSlots = entry.highlightsOnHoverSlots,
            )
            val right = Renderable.string(entry.right)
            outerList.add(listOf(item, left, right))
        }
        return Renderable.table(outerList, xPadding = 5, yPadding = padding)
    }

    @Deprecated("Use List<Renderable>.addButton() instead", ReplaceWith(""))
    inline fun <reified T : Enum<T>> MutableList<List<Any>>.addSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) {
        add(buildSelector<T>(prefix, getName, isCurrent, onChange))
    }

    @Deprecated("do not use", ReplaceWith(""))
    inline fun <reified T : Enum<T>> buildSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) = buildList {
        add(prefix)
        for (entry in enumValues<T>()) {
            val display = getName(entry)
            if (isCurrent(entry)) {
                add("§a[$display]")
            } else {
                add("§e[")
                add(
                    Renderable.link("§e$display") {
                        onChange(entry)
                    },
                )
                add("§e]")
            }
            add(" ")
        }
    }

    fun IslandType.isInIsland() = inSkyBlock && skyBlockIsland == this

    fun inAnyIsland(vararg islandTypes: IslandType) = inSkyBlock && HypixelData.skyBlockIsland in islandTypes
    fun inAnyIsland(islandTypes: Collection<IslandType>) = inSkyBlock && HypixelData.skyBlockIsland in islandTypes

    // TODO move into mayor api
    val isDerpy by RecalculatingValue(1.seconds) { Perk.DOUBLE_MOBS_HP.isActive }

    // TODO move into mayor api
    fun Int.derpy() = if (isDerpy) this / 2 else this

    // TODO move into mayor api
    fun Int.ignoreDerpy() = if (isDerpy) this * 2 else this

    fun inMiningIsland() = IslandType.GOLD_MINES.isInIsland() || IslandType.DEEP_CAVERNS.isInIsland() || MiningApi.inAdvancedMiningIsland()
}
