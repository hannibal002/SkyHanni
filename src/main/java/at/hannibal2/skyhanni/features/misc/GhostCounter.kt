package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SkillExperience
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.misc.GhostCounter.Option.*
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityCreeper
import net.minecraftforge.event.entity.living.LivingDeathEvent
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
    private val ghostlybootPattern = "§6§lRARE DROP! §r§9Ghostly Boots §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val coinsPattern = "§eThe ghost's death materialized §r§61,000,000 coins §r§efrom the mists!".toPattern()
    private val skillXPPattern = ".*§3\\+(?<gained>.*) .* \\((?<total>.*)\\/(?<current>.*)\\).*".toPattern()
    private val killComboPattern = "[+]\\d+ Kill Combo [+](?<coin>.*) coins per kill".toPattern()
    private val killComboExpiredPattern = "§cYour Kill Combo has expired! You reached a (?<combo>.*) Kill Combo!".toPattern()
    private val scavengerMap = mapOf(
            1 to 0.3,
            2 to 0.6,
            3 to 0.9,
            4 to 1.2,
            5 to 1.5,
    )
    private var lastXp: String = "0"
    private var gain: Int = 0
    private var num: Double = 0.0
    private var killComboCoins = 0
    private var inMist = false
    private var hasScavengerTalisman = false

    val map: MutableMap<Option, Double> = mutableMapOf(
            KILLS to 0.0,
            SORROWCOUNT to 0.0,
            VOLTACOUNT to 0.0,
            PLASMACOUNT to 0.0,
            GHOSTLYBOOTS to 0.0,
            BAGOFCASH to 0.0,
            TOTALDROPS to 0.0,
            GHOSTSINCESORROW to 0.0,
            TOTALMF to 0.0,
            SCAVENGERCOINS to 0.0,
            MAXKILLCOMBO to 0.0
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
        counter.scavengerCoins = SCAVENGERCOINS.get()
        counter.hasScavengerTalisman = hasScavengerTalisman
        counter.maxKillCombo = MAXKILLCOMBO.get()
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
        SCAVENGERCOINS.set(counter.scavengerCoins)
        MAXKILLCOMBO.set(counter.maxKillCombo)
        hasScavengerTalisman = counter.hasScavengerTalisman

        update()
    }

    private fun drawDisplay() = buildList<List<Any>> {
        val value = when (SORROWCOUNT.get()) {
            0.0 -> "0"
            else -> "${(((KILLS.get() / SORROWCOUNT.get()) + Math.ulp(1.0)).toInt() * 100) / 100}"
        }
        val mgc = when (TOTALDROPS.get()) {
            0.0 -> "0"
            else -> "${(((TOTALMF.get() / TOTALDROPS.get()) + Math.ulp(1.0)).toInt() * 100) / 100}"
        }

        addAsSingletonList("§6Ghosts counter")
        if (config.showIcon) {
            add(listOf(NEUItems.getItemStack("DIAMOND_SWORD"), "§6Ghosts Killed: §b${KILLS.getInt()}"))
            add(listOf(NEUItems.getItemStack("SORROW"), "§6Ghosts Since Sorrow: §b${GHOSTSINCESORROW.getInt()}"))
            add(listOf(NEUItems.getItemStack("SORROW"), "§6Sorrows: §b${SORROWCOUNT.getInt()}"))
            add(listOf(NEUItems.getItemStack("VOLTA"), "§6Volta: §b${VOLTACOUNT.getInt()}"))
            add(listOf(NEUItems.getItemStack("PLASMA"), "§6Plasma: §b${PLASMACOUNT.getInt()}"))
            add(listOf(NEUItems.getItemStack("GHOST_BOOTS"), "§6Ghostly Boots: §b${GHOSTLYBOOTS.getInt()}"))
            add(listOf(NEUItems.getItemStack("BAG_OF_CASH"), "§6Bag Of Cash: §b${BAGOFCASH.getInt()}"))
            add(listOf(NEUItems.getItemStack("SORROW"), "§6Ghosts/sorrow: §b$value"))
            add(listOf(NEUItems.getItemStack("PET_ITEM_LUCKY_CLOVER"), "§6Avg Magic Find: §b$mgc"))
            add(listOf(NEUItems.getItemStack("COIN_TALISMAN"), "§6Scavenger Coins: §b${SCAVENGERCOINS.getInt()}"))
            add(listOf(NEUItems.getItemStack("DIAMOND_SWORD"), "§6Kill Combo: ${KILLCOMBO.getInt()} §9MAX: §e${MAXKILLCOMBO.getInt()}"))
        } else {
            addAsSingletonList("  §6Ghosts Killed: §b${KILLS.getInt()}")
            addAsSingletonList("  §6Ghosts Since Sorrow: §b${GHOSTSINCESORROW.getInt()}")
            addAsSingletonList("  §6Sorrows: §b${SORROWCOUNT.getInt()}")
            addAsSingletonList("  §6Volta: §b${VOLTACOUNT.getInt()}")
            addAsSingletonList("  §6Plasma: §b${PLASMACOUNT.getInt()}")
            addAsSingletonList("  §6Ghostly Boots: §b${GHOSTLYBOOTS.getInt()}")
            addAsSingletonList("  §6Bag Of Cash: §b${BAGOFCASH.getInt()}")
            addAsSingletonList("  §6Ghosts/sorrow: §b$value")
            addAsSingletonList("  §6Avg Magic Find: §b$mgc")
            addAsSingletonList("  §6Scavenger Coins: §b${SCAVENGERCOINS.getInt()}")
            addAsSingletonList("  §6Kill Combo: ${KILLCOMBO.getInt()} §9MAX: §e${MAXKILLCOMBO.getInt()}")
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

    // Taken from GhostCounterV3 CT module
    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return
        skillXPPattern.matchMatcher(event.message) {
            val gained = group("gained").toDouble()
            val total = group("total")
            if (total != lastXp) {
                val res = total.replace("\\D".toRegex(), "")
                gain = (res.toLong() - lastXp.toLong()).toDouble().roundToInt()
                num = (gain.toDouble() / gained)
                if (gained in 150.0..450.0) {
                    if (lastXp != "0") {
                        if (num >= 0) {
                            KILLS.add(num)
                            GHOSTSINCESORROW.add(num)
                            InventoryUtils.getItemInHand()?.let {
                                val scavengerLevel = it.getEnchantments()?.get("scavenger") ?: 0
                                var baseValue = 0.0
                                baseValue += (scavengerMap.getOrDefault(scavengerLevel, 0).toDouble() * 250)
                                if (hasScavengerTalisman || config.forceScavengerTalisman)
                                    baseValue += (250 * 0.5)
                                baseValue += killComboCoins * num
                                SCAVENGERCOINS.add(baseValue * num)
                                KILLCOMBO.add(num)
                            }
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
        sorrowPattern.matchMatcher(event.message) {
            SORROWCOUNT.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            GHOSTSINCESORROW.set(0.0)
            TOTALDROPS.add(1.0)
            update()
        }
        voltaPattern.matchMatcher(event.message) {
            VOLTACOUNT.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            TOTALDROPS.add(1.0)
            update()
        }
        plasmaPattern.matchMatcher(event.message) {
            PLASMACOUNT.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            TOTALDROPS.add(1.0)
            update()
        }
        ghostlybootPattern.matchMatcher(event.message) {
            GHOSTLYBOOTS.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            TOTALDROPS.add(1.0)
            update()
        }
        coinsPattern.matchMatcher(event.message) {
            BAGOFCASH.add(1.0)
            update()
        }
        killComboExpiredPattern.matchMatcher(event.message){
            if (KILLCOMBO.getInt() > MAXKILLCOMBO.getInt()){
                MAXKILLCOMBO.set(group("combo").toDouble())
            }
            KILLCOMBOCOINS.set(0.0)
            KILLCOMBO.set(0.0)
        }
        killComboPattern.matchMatcher(event.message.removeColor()){
            killComboCoins += group("coin").toInt()
        }
    }

    fun reset() {
        for (opt in Option.values()) {
            opt.set(0.0)
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
        KILLS, SORROWCOUNT, VOLTACOUNT, PLASMACOUNT, GHOSTLYBOOTS, BAGOFCASH, TOTALDROPS,
        GHOSTSINCESORROW, TOTALMF, SCAVENGERCOINS, COINS, COINSPERCENT, MAXKILLCOMBO,
        KILLCOMBO, KILLCOMBOCOINS
    }

    fun Option.getInt(): Int {
        return map[this]?.toInt() ?: 0
    }

    fun Option.get(): Double {
        return map[this]?: 0.0
    }

    fun Option.set(i: Double) {
        map[this] = i
    }

    fun Option.add(i: Double) {
        map[this] = map.getOrDefault(this, 0.0) + i
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        val inventoryName = event.inventoryName
        if (inventoryName.startsWith("Accessory Bag")) {
            val stacks = event.inventoryItems
            for ((_, stack) in stacks) {
                val name = stack.name ?: return
                if (name.removeColor().contains("Scavenger Talisman")) {
                    hasScavengerTalisman = true
                }
            }
        }
    }
}