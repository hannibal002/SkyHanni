package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.misc.GhostCounter.Option.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityCreeper
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.roundToInt

object GhostCounter {

    private val config get() = SkyHanniMod.feature.misc.ghostCounter
    private val hidden get() = ProfileStorageData.profileSpecific
    private var display = listOf<List<Any>>()
    private val sorrowPattern = "§6§lRARE DROP! §r§9Sorrow §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val plasmaPattern = "§6§lRARE DROP! §r§9Plasma §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val voltaPattern = "§6§lRARE DROP! §r§9Volta §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val ghostybootPattern = "§6§lRARE DROP! §r§9Ghostly Boots §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val coinsPattern = "§eThe ghost's death materialized §r§61,000,000 coins §r§efrom the mists!".toPattern()
    private val skillXPPattern = ".*§3\\+(?<gained>.*) .* \\((?<total>.*)\\/(?<current>.*)\\).*".toPattern()
    private var lastXp: String = "0"
    private var gain: Int = 0
    private var num: Int = 0
    private var inMist = false

    val map: MutableMap<Option, Int> = mutableMapOf(
            KILLS to 0,
            SORROWCOUNT to 0,
            VOLTACOUNT to 0,
            PLASMACOUNT to 0,
            GHOSTLYBOOTS to 0,
            BAGOFCASH to 0,
            TOTALDROPS to 0,
            GHOSTSINCESORROW to 0,
            TOTALMF to 0
    )

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.ghostDisplayText) {
            newList.add(map[index])
        }
        return newList
    }

    fun update() {
        if (!isEnabled()) return
        val counter = hidden?.ghostCounter ?: return
        counter.ghostKills = KILLS.get()
        counter.sorrowCount = SORROWCOUNT.get()
        counter.voltaCount = VOLTACOUNT.get()
        counter.plasmaCount = PLASMACOUNT.get()
        counter.ghoostlyBootsCount = GHOSTLYBOOTS.get()
        counter.bagOfCashCount = BAGOFCASH.get()
        counter.totalDrops = TOTALDROPS.get()
        counter.ghostSinceSorrow = GHOSTSINCESORROW.get()
        counter.totalMF = TOTALMF.get()
        display = formatDisplay(drawDisplay())
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val counter = hidden?.ghostCounter ?: return
        KILLS.set(counter.ghostKills)
        SORROWCOUNT.set(counter.sorrowCount)
        VOLTACOUNT.set(counter.voltaCount)
        PLASMACOUNT.set(counter.plasmaCount)
        GHOSTLYBOOTS.set(counter.ghoostlyBootsCount)
        BAGOFCASH.set(counter.bagOfCashCount)
        TOTALDROPS.set(counter.totalDrops)
        GHOSTSINCESORROW.set(counter.ghostSinceSorrow)
        TOTALMF.set(counter.totalMF)
        update()
    }

    private fun drawDisplay() = buildList<List<Any>> {
        val value = when (SORROWCOUNT.get()) {
            0 -> "0"
            else -> "${(((KILLS.get() / SORROWCOUNT.get()) + Math.ulp(1.0)) * 100).roundToInt() / 100}"
        }
        val mgc = when (TOTALDROPS.get()) {
            0 -> "0"
            else -> "${(((TOTALMF.get() / TOTALDROPS.get()) + Math.ulp(1.0)) * 100).roundToInt() / 100}"
        }

        addAsSingletonList("§6Ghosts counter")
        if (config.showIcon) {
            add(listOf(NEUItems.getItemStack("DIAMOND_SWORD"), "§6Ghosts Killed: §b${KILLS.get()}"))
            add(listOf(NEUItems.getItemStack("SORROW"), "§6Ghosts Since Sorrow: §b${GHOSTSINCESORROW.get()}"))
            add(listOf(NEUItems.getItemStack("SORROW"), "§6Sorrows: §b${SORROWCOUNT.get()}"))
            add(listOf(NEUItems.getItemStack("VOLTA"), "§6Volta: §b${VOLTACOUNT.get()}"))
            add(listOf(NEUItems.getItemStack("PLASMA"), "§6Plasma: §b${PLASMACOUNT.get()}"))
            add(listOf(NEUItems.getItemStack("GHOST_BOOTS"), "§6Ghostly Boots: §b${GHOSTLYBOOTS.get()}"))
            add(listOf(NEUItems.getItemStack("BAG_OF_CASH"), "§6Bag Of Cash: §b${BAGOFCASH.get()}"))
            add(listOf(NEUItems.getItemStack("SORROW"), "§6Ghosts/sorrow: §b$value"))
            add(listOf(NEUItems.getItemStack("PET_ITEM_LUCKY_CLOVER"), "§6Avg Magic Find: §b$mgc"))
        } else {
            addAsSingletonList("  §6Ghosts Killed: §b${KILLS.get()}")
            addAsSingletonList("  §6Ghosts Since Sorrow: §b${GHOSTSINCESORROW.get()}")
            addAsSingletonList("  §6Sorrows: §b${SORROWCOUNT.get()}")
            addAsSingletonList("  §6Volta: §b${VOLTACOUNT.get()}")
            addAsSingletonList("  §6Plasma: §b${PLASMACOUNT.get()}")
            addAsSingletonList("  §6Ghostly Boots: §b${GHOSTLYBOOTS.get()}")
            addAsSingletonList("  §6Bag Of Cash: §b${BAGOFCASH.get()}")
            addAsSingletonList("  §6Ghosts/sorrow: §b$value")
            addAsSingletonList("  §6Avg Magic Find: §b$mgc")
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyOnMist && !inMist) return
        config.position.renderStringsAndItems(display,
                extraSpace = 1,
                posLabel = "Ghost Counter")
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return
        skillXPPattern.matchMatcher(event.message) {
            val gained = group("gained").toDouble()
            val total = group("total")
            if (total != lastXp) {
                val res = total.replace("\\D".toRegex(), "")
                gain = (res.toLong() - lastXp.toLong()).toDouble().roundToInt()
                num = (gain.toDouble() / gained).roundToInt()
                if (gained in 150.0..450.0) {
                    if (lastXp != "0") {
                        if (num >= 0) {
                            KILLS.add(num)
                            GHOSTSINCESORROW.add(num)
                        }
                    }
                }
                lastXp = res
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockArea != "The Mist") return
        println(event.message)
        sorrowPattern.matchMatcher(event.message) {
            SORROWCOUNT.add(1)
            TOTALMF.add(group("mf").substring(4).toInt())
            GHOSTSINCESORROW.set(0)
            TOTALDROPS.add(1)
            update()
        }
        voltaPattern.matchMatcher(event.message) {
            VOLTACOUNT.add(1)
            TOTALMF.add(group("mf").substring(4).toInt())
            TOTALDROPS.add(1)
            update()
        }
        plasmaPattern.matchMatcher(event.message) {
            PLASMACOUNT.add(1)
            TOTALMF.add(group("mf").substring(4).toInt())
            TOTALDROPS.add(1)
            update()
        }
        ghostybootPattern.matchMatcher(event.message) {
            GHOSTLYBOOTS.add(1)
            TOTALMF.add(group("mf").substring(4).toInt())
            TOTALDROPS.add(1)
            update()
        }
        coinsPattern.matchMatcher(event.message) {
            BAGOFCASH.add(1)
            update()
        }
    }

    private fun isGhost(entity: Entity): Boolean {
        if (entity !is EntityCreeper) return false
        return entity.isInvisible && entity.powered
    }

    fun reset() {
        for (opt in Option.values()) {
            opt.set(0)
        }
        update()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        inMist = LorenzUtils.skyBlockArea == "The Mist"
        update()
    }

    fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.enabled && LorenzUtils.skyBlockIsland == IslandType.DWARVEN_MINES
    }

    enum class Option {
        KILLS, SORROWCOUNT, VOLTACOUNT, PLASMACOUNT, GHOSTLYBOOTS, BAGOFCASH, TOTALDROPS, GHOSTSINCESORROW, TOTALMF
    }

    fun Option.get(): Int {
        return map[this] ?: 0
    }

    fun Option.set(i: Int) {
        map[this] = i
    }

    fun Option.add(i: Int) {
        map[this] = map.getOrDefault(this, 0) + i
    }
}