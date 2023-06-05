package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityCreeper
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.roundToInt

class GhostCounter {

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

    private var kills = 0
    private var sorrowCount = 0
    private var voltaCount = 0
    private var plasmaCount = 0
    private var ghostlyBootCount = 0
    private var bagOfCashCount = 0
    private var avgMagicFind = 0
    private var totalDrops = 0

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
        counter.ghostKills = kills
        counter.sorrowCount = sorrowCount
        counter.voltaCount = voltaCount
        counter.plasmaCount = plasmaCount
        counter.ghoostlyBootsCount = ghostlyBootCount
        counter.bagOfCashCount = bagOfCashCount
        counter.avgMagicFind = avgMagicFind
        counter.totalDrops = totalDrops
        display = formatDisplay(drawDisplay())
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val counter = hidden?.ghostCounter ?: return
        kills = counter.ghostKills
        sorrowCount = counter.sorrowCount
        voltaCount = counter.voltaCount
        plasmaCount = counter.plasmaCount
        ghostlyBootCount = counter.ghoostlyBootsCount
        bagOfCashCount = counter.bagOfCashCount
        update()
    }

    private fun drawDisplay() = buildList<List<Any>> {
        addAsSingletonList("§6Ghosts counter")
        addAsSingletonList("  §bGhosts Killed: $kills")
        addAsSingletonList("  §bSorrows: $sorrowCount")
        addAsSingletonList("  §bVolta: $voltaCount")
        addAsSingletonList("  §bPlasma: $plasmaCount")
        addAsSingletonList("  §bGhostly Boots: $ghostlyBootCount")
        addAsSingletonList("  §bBag of cash: $bagOfCashCount")

        val value = when (sorrowCount) {
            0 -> "0"
            else -> "${(((kills / sorrowCount) + Math.ulp(1.0)) * 100).roundToInt() / 100}"
        }
        addAsSingletonList("  §bGhosts/sorrow: $value")

    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyOnMist && !inMist) return
        config.position.renderStringsAndItems(display, posLabel = "Ghost Counter")
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
                    //println(lastXp)
                    if (lastXp != "0") {
                        println(num)
                        if (num >= 0) {
                            kills += num
                            //println("yes")
                        }
                    }
                }
                lastXp = res
            }

            /*if (total != lastXp){
                val res = total.replace(Regex("/\\D/g"), "")
                println(res)
                gain = (res.toInt() - lastXp.toDouble()).roundToInt().toString()
                num = (gain.toDouble() - gained.toDouble()).roundToInt()
                if (gained in 150..450){
                    if (lastXp.toInt() != 0){
                        if (num >= 0){
                            counter.kill += 1;
                            counter.totalKill += 1
                            update()
                            println("kills")
                        }
                    }
                }
                lastXp = res
            }*/
        }
    }
    /*    @SubscribeEvent
        fun onKill(event: LivingDeathEvent) {
            if (!isEnabled()) return
            if (LorenzUtils.skyBlockArea != "The Mist") return
            val counter = profileSpecific?.ghostCounter ?: return
            if (isGhost(event.entity)) {
                if (ghostList.remove(event.entity.uniqueID)) {
                    counter.kill += 1
                    counter.totalKill += 1
                    update()
                }
            }
        }

        @SubscribeEvent
        fun onAttack(e: AttackEntityEvent) {
            if (!isEnabled()) return
                if (e.target is EntityCreeper) {
                    if (isGhost(e.target)) {
                        ghostList.add(e.target.uniqueID)
                    }
                }
        }*/

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockArea != "The Mist") return
        println(event.message)
        sorrowPattern.matchMatcher(event.message) {
            sorrowCount++
            update()
        }

        voltaPattern.matchMatcher(event.message) {
            voltaCount++
            avgMagicFind = group("mf").slice(0..4).toInt()
            totalDrops++
            update()
        }

        plasmaPattern.matchMatcher(event.message) {
            plasmaCount++
            avgMagicFind = group("mf").slice(0..4).toInt()
            totalDrops++
            update()
        }

        ghostybootPattern.matchMatcher(event.message) {
            ghostlyBootCount++
            avgMagicFind = group("mf").slice(0..4).toInt()
            totalDrops++
            update()
        }

        coinsPattern.matchMatcher(event.message) {
            bagOfCashCount++
            update()
        }
    }

    private fun isGhost(entity: Entity): Boolean {
        if (entity !is EntityCreeper) return false
        return entity.isInvisible && entity.powered
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

}