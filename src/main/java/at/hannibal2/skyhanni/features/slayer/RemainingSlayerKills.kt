package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerProgressChangeEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.misc.effects.NonGodPotEffectDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.math.ceil
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object RemainingSlayerKills {

    private val config get() = SlayerApi.config

    data class SlayerData(
        @Expose @SerializedName("normal_mobs")
        val normalMobs: Map<SlayerType, Map<String, List<Mob>>>,

        @Expose @SerializedName("mini_bosses")
        val miniBosses: Map<SlayerType, Map<String, List<Mob>>>,

        @Expose
        val weapons: Map<SlayerType, Map<NeuInternalName, Int>>,

        @Expose
        val equipments: Map<SlayerType, Map<NeuInternalName, Int>>,
    )

    data class Mob(
        @Expose val name: String,
        @Expose val level: Int,
        @Expose @SerializedName("max_health") val maxHealth: Int,
        @Expose val xp: Double,
    )

    private var data: SlayerData? = null
    private var display = emptyList<Renderable>()
    private var lastMissing: Double? = null
    private var baseCombatWisdom: Int? = null
    private var lastReminder = SimpleTimeMark.farPast()
    private var killComboWisdom = 0

    @HandleEvent(priority = HIGHEST)
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<SlayerData>("Slayer")
    }

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        lastMissing = null
        baseCombatWisdom = null
        lastReminder = SimpleTimeMark.farPast()
        update()
    }

    @HandleEvent
    fun onSlayerProgressChange(event: SlayerProgressChangeEvent) {
        if (!isEnabled()) return

        val progress = event.newProgress
        // TODO repo patterns
        val pattern = "§7\\(§e(?<current>.*)§7\\/§c(?<max>.*)§7\\) .*".toPattern()
        val newMissing = pattern.matchMatcher(progress) {
            val current = group("current").formatDouble()
            val max = group("max").formatDouble()
            max - current
        }
        // TODO remove debug
        lastMissing?.let { last ->
            if (newMissing != null) {
                val diff = last - newMissing
                println("diff: $diff")
            }
        }
        lastMissing = newMissing
        update()
    }

    @HandleEvent(GraphAreaChangeEvent::class)
    fun onAreaChange() {
        if (!isEnabled()) return
        update()
    }

    @HandleEvent
    fun onChat(event: SystemMessageEvent) {
        // TODO repo patterns
        val pattern = "§cYour Kill Combo has expired! You reached a (.*) Kill Combo!".toPattern()
        pattern.matchMatcher(event.message) {
            killComboWisdom = 0
        }
        if (event.message == "§5§l+20 Kill Combo §r§8§r§3+15☯ Combat Wisdom") {
            killComboWisdom = 15
        }
    }

    private fun update() {
        display = createDisplay().map { StringRenderable(it) }
    }

    private fun createDisplay(): List<String> {
        val missing = lastMissing ?: return emptyList()
        if (!SlayerApi.isInCorrectArea) return emptyList()
        val slayerType = SlayerApi.currentAreaType ?: return emptyList()

        return buildList {
            add("§e§lRemaining ${slayerType.displayName} ${SlayerApi.tier} kills")
            addAll(getMobNames(missing))
            if (baseCombatWisdom == null) {
                remindToUpdateCombatWisdom()
                add("§cNo base Combat Wisdom information! §e/stats")
            }
        }
    }

    private fun getMobNames(missing: Double): List<String> {
        val mobs = getMobs() ?: return listOf()

        val multiplier = getMultiplier()
        return mobs.map { mob ->
            val timesNeeded = missing / (mob.xp * multiplier)
            val kills = "§e${ceil(timesNeeded).addSeparators()}x"
            " §7- $kills ${mob.names()}" to timesNeeded
        }.sortedByDescending { it.second }.map { it.first }
    }

    private fun getMobs(): List<Mob>? {
        val data = data ?: return null
        val areas = data.normalMobs[SlayerApi.currentAreaType] ?: mapOf()
        val normalMobs = areas[SkyBlockUtils.graphArea] ?: listOf()

        return buildList {
            addAll(normalMobs)
            data.miniBosses[SlayerApi.activeType]?.get(SlayerApi.tier.toString())?.let {
                addAll(it)
            }
        }
    }

    private fun getMultiplier(): Double {
        var combatWisdom = 1.0

        combatWisdom += (baseCombatWisdom ?: 0)

        combatWisdom += killComboWisdom

        data?.let { data ->
            data.weapons[SlayerApi.activeType]?.get(InventoryUtils.itemInHandId)?.let { wisdom ->
                combatWisdom += wisdom
                println("weapon wisdom: $wisdom")
                combatWisdom += countHabaneroOnArmor()
            }

            data.equipments[SlayerApi.activeType]?.let { equipments ->
                for (internalName in EquipmentApi.getAll().map { it.getInternalNameOrNull() }) {
                    equipments[internalName]?.let { wisdom ->
                        println("equipment wisdom: $wisdom")
                        combatWisdom += wisdom
                    }
                }
            }
        }

        if (NonGodPotEffectDisplay.isActive(NonGodPotEffect.SMOLDERING) && SlayerApi.activeType == SlayerType.INFERNO) {
            println("Smoldering Polarization: +10")
            combatWisdom += 10
        }
//         val slayerWeapons = ""

        // TODO confirm if this is correct
        if (ElectionApi.isDerpy) {
            println("derpy +10")
            combatWisdom += 30
        }

        // TODO add 20% xp boost globally from hypixel event

        return 1 + 0.01 * combatWisdom
    }

    private fun countHabaneroOnArmor(): Double {
        var counter = 0
        for (stack in InventoryUtils.getArmor().filterNotNull()) {
            for ((enchantment, level) in stack.getHypixelEnchantments().orEmpty()) {
                if (enchantment != "ultimate_habanero_tactics") continue
                when (level) {
                    4 -> {
                        counter++
                    }

                    5 -> {
                        counter += 2
                    }

                    else -> error("unknown habanero level: $level")
                }
            }
        }
        val result = counter * 2.5
        println("countHabaneroOnArmor: $result")
        return result
    }

    private fun Mob.names() = buildString {
        if (config.remainingKillsLevel) {
            append("§8[§7Lv${level.addSeparators()}§8] ")
        }
        append("§c${name}")
        if (config.remainingKillsHealth) {
            append(" §a${maxHealth.shortFormat()}§c❤")
        }
    }

    private fun remindToUpdateCombatWisdom() {
        if (lastReminder.passedSince() < 5.minutes) return

        lastReminder = SimpleTimeMark.now()
        ChatUtils.clickToActionOrDisable(
            "Remaining Slayer Kills feature needs to know your combat wisdom to work.",
            config::remainingKills,
            actionName = "open stats menu",
            action = {
                HypixelCommands.stats()
            },
        )
    }


    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName != "Your Equipment and Stats") return
        val stack = event.inventoryItems[34] ?: return

        val pattern = " §3☯ Combat Wisdom §f(?<wisdom>.*)".toPattern()
        for (line in stack.getLore()) {
            pattern.matchMatcher(line) {
                baseCombatWisdom = group("wisdom").formatInt()
                update()
                return
            }
        }
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() },
            onRender = {
                config.remainingKillsPosition.renderRenderables(display, posLabel = "Remaining Slayer Kills")
            },
        )
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.remainingKills
}

