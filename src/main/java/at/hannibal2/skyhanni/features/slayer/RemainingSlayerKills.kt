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
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.math.ceil
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object RemainingSlayerKills {

    private val config get() = SlayerApi.config

    private val patternGroup = RepoPattern.group("slayer.remaining-kills")

    /**
     * REGEX-TEST:  ☯ Combat Wisdom 2
     */
    private val combatWisdomPattern by patternGroup.pattern(
        "combat-wisdom",
        " ☯ Combat Wisdom (?<wisdom>\\d+)",
    )

    /**
     * REGEX-TEST: (120/500) Atomic Slayer
     */
    private val progressPattern by patternGroup.pattern(
        "progress",
        "\\((?<current>[\\d,.]+[kmb]?)\\/(?<max>[\\d,.]+[kmb]?)\\) .*",
    )

    /**
     * REGEX-TEST: Your Kill Combo has expired! You reached a 3 Kill Combo!
     */
    private val comboExpiredPattern by patternGroup.pattern(
        "combo.expired",
        "Your Kill Combo has expired! You reached a .* Kill Combo!",
    )

    /**
     * REGEX-TEST: +20 Kill Combo +15☯ Combat Wisdom
     */
    private val killCombatWisdomPattern by patternGroup.pattern(
        "kill-combat-wisdom",
        "\\+\\d+ Kill Combo \\+(?<wisdom>\\d+)☯ Combat Wisdom",
    )

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

        val progress = event.newProgress.removeColor()
        val newMissing = progressPattern.matchMatcher(progress) {
            val current = group("current").formatDouble()
            val max = group("max").formatDouble()
            max - current
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
    fun onChat(event: SystemMessageEvent.Allow) {
        val message = event.cleanMessage
        if (comboExpiredPattern.matches(message)) {
            killComboWisdom = 0
        }
        killCombatWisdomPattern.matchMatcher(message) {
            killComboWisdom = group("wisdom").formatInt()
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
                combatWisdom += countHabaneroOnArmor()
            }

            data.equipments[SlayerApi.activeType]?.let { equipments ->
                for (internalName in EquipmentApi.getAll().map { it.getInternalNameOrNull() }) {
                    equipments[internalName]?.let { wisdom ->
                        combatWisdom += wisdom
                    }
                }
            }
        }

        if (NonGodPotEffectDisplay.isActive(NonGodPotEffect.SMOLDERING) && SlayerApi.activeType == SlayerType.INFERNO) {
            combatWisdom += 10
        }

        // TODO confirm if this is correct
        if (ElectionApi.isDerpy) {
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
        return counter * 2.5
    }

    private fun Mob.names() = buildString {
        if (config.remainingKillsLevel) {
            append("§8[§7Lv${level.addSeparators()}§8] ")
        }
        append("§c$name")
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

        for (line in stack.getLore()) {
            combatWisdomPattern.matchMatcher(line.removeColor()) {
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
            condition = { isEnabled() && SlayerApi.isInCorrectArea },
            onRender = {
                config.remainingKillsPosition.renderRenderables(display, posLabel = "Remaining Slayer Kills")
            },
        )
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.remainingKills
}

