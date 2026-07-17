package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.model.SkyblockStat
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
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
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

    private val config get() = SlayerApi.config.slayerRemainingKills
    private val debugToggle get() = SkyHanniMod.feature.dev.debug.remainingKillsDebug

    private val patternGroup = RepoPattern.group("slayer.remaining-kills")

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

        @Expose
        val pets: Map<SlayerType, Map<String, SlayerSpecificPetData>>,

        @Expose
        val champion: List<Double>,

        @Expose @SerializedName("habanero_wisdom_per_level") val habaneroMultiplier: Double,

        @Expose @SerializedName("multiplicative_mayor_perks") val multiplicativeMayors: Map<String, Double>,

        @Expose @SerializedName("arbitrary_multiplier") val arbitraryMultiplier: Double,
    )

    data class SlayerSpecificPetData(
        // These are only the first halves of a pet's Internal Name, this is the name used within PetData/PetUtils for these.
        @Expose @SerializedName("proper_pet_names") val properPetNames: List<String>? = null,
        @Expose @SerializedName("scaling") val perLevelMultiplier: List<Float>,
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
    private var lastMax: Double? = null
    private var lastReminder = SimpleTimeMark.farPast()
    private var killComboWisdom = 0

    @HandleEvent(priority = HIGHEST)
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<SlayerData>("Slayer")
    }

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        lastMissing = null
        lastMax = null
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
            lastMax = max
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
    fun onSystemMessage(event: SystemMessageEvent.Allow) {
        val message = event.cleanMessage
        if (comboExpiredPattern.matches(message)) {
            killComboWisdom = 0
        }
        killCombatWisdomPattern.matchMatcher(message) {
            killComboWisdom += group("wisdom").formatInt()
        }
        // This is an attempt to future-proof this due to proposed Magic Find Update changes.
        // https://hypixel.net/threads/design-thread-magic-find.6015417/
    }

    private fun update() {
        display = createDisplay().map { StringRenderable(it) }
    }

    private fun createDisplay(): List<String> {
        val missing = lastMissing ?: return emptyList()
        val maxXP = lastMax ?: return emptyList()
        if (!SlayerApi.isInCorrectArea) return emptyList()
        val slayerType = SlayerApi.currentAreaType ?: return emptyList()

        return buildList {
            add("§e§lRemaining ${slayerType.displayName} ${SlayerApi.tier} kills")
            addAll(getMobNames(missing, maxXP))
        }
    }

    private fun getMobNames(missing: Double, totalQuestXP: Double): List<String> {
        val mobs = getMobs() ?: return listOf()

        val combatWisdomMultiplier = getCombatWisdomMultiplier()
        debugMessage("$combatWisdomMultiplier multiplier for Combat Wisdom .")
        val multiplicativeMultiplier = getMultiplicativeMultiplier()
        debugMessage("$multiplicativeMultiplier multiplier for multiplicatives.")
        return mobs.map { mob ->
            var expectedXP = (mob.xp * combatWisdomMultiplier * multiplicativeMultiplier)
            val maxObtainableAtATime = (totalQuestXP * 0.75)
            // The maximum amount of progress that a kill can contribute towards your Slayer Quest has been raised from 50% to 75%.
            // https://hypixel.net/threads/hypixel-skyblock-0-20-9-crimson-isle-qol.5809290/
            expectedXP = expectedXP.coerceAtMost(maxObtainableAtATime)
            debugMessage("Base Mob XP: ${mob.xp}, Post Multiplier XP = $expectedXP")
            val timesNeeded = missing / expectedXP
            val kills = "§e${ceil(timesNeeded).addSeparators()}x"
            " §7- $kills ${mob.names(expectedXP, totalQuestXP)}" to timesNeeded
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

    private fun getCombatWisdomMultiplier(): Double {
        var combatWisdom = 1.0
        val baseCombatWisdom = SkyblockStat.COMBAT_WISDOM.lastKnownValue
        if (baseCombatWisdom == null) {
            remindToUpdateCombatWisdom()
        } else {
            combatWisdom += (baseCombatWisdom)
            debugMessage("Combat Wisdom in /eq is $baseCombatWisdom")
        }

        combatWisdom += killComboWisdom
        debugMessage("kill combo wisdom is $killComboWisdom")

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

        return 1 + 0.01 * combatWisdom
    }

    private fun getMultiplicativeMultiplier(): Double {
        var multiplier = 1.0
        val data = data ?: return 1.0

        ElectionApi.getAllActivePerks().forEach { multiplier *= data.multiplicativeMayors[it.name] ?: 1.0 }

        multiplier *= data.arbitraryMultiplier
        // Derpy/Aura XP Boost were disallowed in First Aura simultaneously, this is for if they change that opinion

        // Do not add multiplicative bonuses here from Seasonal buffs without checking fully
        // They are not implemented but can be added using the "arbitrary_multiplier" field in repo to remote update functionally.
        // They have historically not worked on slayer spawn entirely.

        // TODO Automatic Raffle Boost Detection as the 50% is the only "Seasonal" boost known to work on slayer spawn.

        multiplier *= getAdditivelyMultiplicativeValues()

        return multiplier
    }

    /**
     * According to the Independent Wiki Stacking Enchants (Toxo/Champ) Are Additive with the Pet Bonuses but nothing else.
     * https://hypixelskyblock.minecraft.wiki/w/Combat_Wisdom#Notes
     */
    private fun getAdditivelyMultiplicativeValues(): Double {

        var additiveWithMultMultipliers = 1.0

        val championLevel = (InventoryUtils.getItemInHand()?.getHypixelEnchantments().orEmpty()["champion"] ?: 0) - 1

        if (championLevel != -1) additiveWithMultMultipliers += (data?.champion?.getOrNull(championLevel) ?: 0.0)

        val currentPet = CurrentPetApi.currentPet
        val fauxInternalName = currentPet?.fauxInternalName ?: return additiveWithMultMultipliers
        debugMessage("$fauxInternalName")
        val petProperName = PetUtils.getPetProperName(fauxInternalName).orEmpty()
        val petRarity = PetUtils.getPetRarity(fauxInternalName) ?: return additiveWithMultMultipliers
        debugMessage("Split Internal Name, ID = $petProperName, rarity = $petRarity")
        if (petProperName.isEmpty()) return additiveWithMultMultipliers
        data?.let { data ->
            val slayerPetData = data.pets[SlayerApi.activeType]
            val levellingData = slayerPetData?.firstNotNullOfOrNull { slayerPet ->
                slayerPet.value.takeIf { (it.properPetNames != null && it.properPetNames.contains(petProperName)) }
            } ?: return additiveWithMultMultipliers

            additiveWithMultMultipliers += (levellingData.perLevelMultiplier[petRarity.id] * currentPet.level)
            debugMessage(
                "$additiveWithMultMultipliers Pet & Champion Multiplier, ${currentPet.level} is Pet Level.",
            )
        }

        return additiveWithMultMultipliers
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
        return data?.habaneroMultiplier?.times(counter) ?: 0.0
    }

    private fun Mob.names(xp: Double, totalQuestXP: Double) = buildString {
        if (config.showOverkill && xp > totalQuestXP) {
            append("§4Overkilling Necessary XP! ")
        }
        if (config.includeExpectedXP) {
            append("§3${xp.roundTo(2)} xp ")
        }
        if (config.includeMobLevel) {
            append("§8[§7Lv${level.addSeparators()}§8] ")
        }
        append("§c$name")
        if (config.includeMobHealth) {
            append(" §a${maxHealth.shortFormat()}§c❤")
        }
    }

    private fun debugMessage(message: String) {
        if (debugToggle) ChatUtils.debug(message)
    }

    private fun remindToUpdateCombatWisdom() {
        if (lastReminder.passedSince() < 5.minutes) return

        lastReminder = SimpleTimeMark.now()
        ChatUtils.clickToActionOrDisable(
            "Remaining Slayer Kills feature needs to know your combat wisdom to work.",
            config::display,
            actionName = "open stats menu",
            action = {
                HypixelCommands.stats()
            },
        )
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

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.display
}

