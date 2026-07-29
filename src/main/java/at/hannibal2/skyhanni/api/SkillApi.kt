package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuSkillLevelJson
import at.hannibal2.skyhanni.events.AccessoryBagUpdateEvent
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelUpEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillProgress
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.SPACE_SPLITTER
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.calculateLevelXP
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.calculateSkillLevel
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.getLevelExact
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.getSkillInfo
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.xpRequiredForLevel
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrUserError
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.network.chat.Component
import java.util.LinkedList
import java.util.regex.Matcher
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkillApi {
    private val patternGroup = RepoPattern.group("api.skilldisplay")

    /**
     * REGEX-TEST: +1.1 Mining (48.39%)
     */
    private val skillPercentPattern by patternGroup.pattern(
        "skill.percent",
        "\\+(?<gained>[\\d.,]+) (?<skillName>.+) \\((?<progress>[\\d.,]+)%\\)",
    )

    /**
     * REGEX-TEST: +6.3 Foraging (24/750)
     */
    private val skillMultiplierPattern by patternGroup.pattern(
        "skill.multiplier",
        "\\+(?<gained>[\\d.,]+) (?<skillName>.+) \\((?<current>[\\d.,]+)\\/(?<needed>[\\d,.]+[kmb]?)\\)",
    )

    /**
     * REGEX-TEST: ☺ You claimed 2,500 Foraging XP from the Jerry Box!
     */
    private val jerryBoxSkillXpPattern by patternGroup.pattern(
        "chat.jerry-box.skillxp",
        ".*You claimed (?<gained>[\\d,]+) (?<skillName>\\w+) XP from the Jerry Box!",
    )

    /**
     * REGEX-TEST: COMMON! +500 Combat XP gift with [VIP] sn0wfxll!
     */
    private val giftSkillXpPattern by patternGroup.pattern(
        "chat.gift.skillxp",
        "(?:COMMON|RARE|SWEET|SANTA(?: TIER)?|PARTY(?: TIER)?)! \\+(?<gained>[\\d,]+) (?<skillName>[\\w ]+) XP gift with .*!?",
    )

    /**
     * REGEX-TEST: LILY-SPLOSION!
     * REGEX-TEST: LILI-SPLOSION!
     */
    private val lilySplosionStartPattern by patternGroup.pattern(
        "chat.lily-splosion.start",
        "LIL[YI]-SPLOSION!",
    )

    /**
     * REGEX-TEST: +120 Fishing Experience
     */
    private val lilySplosionSkillXpPattern by patternGroup.pattern(
        "chat.lily-splosion.skillxp",
        "\\+(?<gained>[\\d,]+) (?<skillName>\\w+) Experience",
    )

    // TODO find out whats going on here
    /**
     * WRAPPED-REGEX-TEST: " Farming 35: 12.4%"
     */
    private val skillTabPattern by patternGroup.pattern(
        "skill.tab.colorless",
        " (?<type>\\w+)(?: (?<level>\\d+))?: (?<progress>[0-9.]+)%",
    )

    /**
     * WRAPPED-REGEX-TEST: " Farming 60: MAX"
     * WRAPPED-REGEX-TEST: " Mining 60: MAX"
     */
    private val maxSkillTabPattern by patternGroup.pattern(
        "skill.tab.max.colorless",
        " (?<type>\\w+) (?<level>\\d+): MAX",
    )

    /**
     * WRAPPED-REGEX-TEST: " Mining 14: 22,922/75k"
     * WRAPPED-REGEX-TEST: " Combat 49: 7,678/4M"
     */
    private val skillTabNoPercentPattern by patternGroup.pattern(
        "skill.tab.nopercent.colorless",
        " (?<type>\\w+)(?: (?<level>\\d+))?: (?<current>[0-9,.]+)/(?<needed>[\\d,.]+[kMB]?+)",
    )

    /**
     * Regex-TEST: Max Skill level reached!
     */
    private val skillMaxLevelMenuPattern by patternGroup.pattern(
        "skill.menu.maxreached",
        "Max Skill level reached!"
    )

    var skillXPInfoMap = mutableMapOf<SkillType, SkillXPInfo>()
    var oldSkillInfoMap = mutableMapOf<SkillType?, SkillInfo?>()
    private val skillStorage get() = ProfileStorageData.profileSpecific?.skills
    val storage get() = skillStorage?.skillData
    var exactLevelingMap = mapOf<Int, Int>()
    var levelingMap = mapOf<Int, Int>()
    var levelArray = listOf<Int>()
    var activeSkill: SkillType? = null

    var showDisplay = false
    var lastUpdate = SimpleTimeMark.farPast()

    private var lastTabComponents: List<Component> = emptyList()
    private var lastLilySplosion = SimpleTimeMark.farPast()

    private const val JERRY_BOX_SOURCE = "chat-jerry-box"
    private const val GIFT_SOURCE = "chat-gift"
    private const val LILY_SPLOSION_SOURCE = "chat-lily-splosion"
    private const val SEASON_OF_JERRY_SHOP = "Season of Jerry"
    private const val PROFESSIONAL_GIFTER = "Professional Gifter"
    private const val PROFESSIONAL_GIFTER_TIER_ONE_BONUS = 0.05
    private const val PROFESSIONAL_GIFTER_TIER_TWO_BONUS = 0.10
    private const val SNOWMAN_MASK_BONUS = 0.10

    private val SNOWMAN_MASK = "SNOWMAN_MASK".toInternalName()
    private val GIFT_TALISMAN_BONUSES = mapOf(
        "WHITE_GIFT_TALISMAN".toInternalName() to 0.05,
        "GREEN_GIFT_TALISMAN".toInternalName() to 0.10,
        "BLUE_GIFT_TALISMAN".toInternalName() to 0.15,
        "PURPLE_GIFT_TALISMAN".toInternalName() to 0.20,
        "GOLD_GIFT_TALISMAN".toInternalName() to 0.25,
    )

    private var giftTalismanBonus: Double
        get() = skillStorage?.giftTalismanSkillXpBonus ?: 0.0
        set(value) {
            skillStorage?.giftTalismanSkillXpBonus = value
        }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListUpdate(event: TabListUpdateEvent) {
        lastTabComponents = event.tabList
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnSkyblock = true)
    fun onSecondPassed() {
        val activeSkill = activeSkill ?: return
        val info = skillXPInfoMap[activeSkill] ?: return
        if (!info.sessionTimerActive) return

        val time = when (activeSkill) {
            SkillType.FARMING -> SkillProgress.etaConfig.farmingPauseTime
            SkillType.MINING -> SkillProgress.etaConfig.miningPauseTime
            SkillType.COMBAT -> SkillProgress.etaConfig.combatPauseTime
            SkillType.FORAGING -> SkillProgress.etaConfig.foragingPauseTime
            SkillType.FISHING -> SkillProgress.etaConfig.fishingPauseTime
            else -> 0
        }
        if (info.lastUpdate.passedSince() > time.seconds) {
            info.sessionTimerActive = false
        }
        if (info.sessionTimerActive) {
            info.timeActive++
        }
    }

    @HandleEvent
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        val actionBar = event.chatComponent.string.removeColor()
        val components = SPACE_SPLITTER.splitToList(actionBar)
        for (component in components) {
            val matcher = listOf(skillPercentPattern, skillMultiplierPattern).firstOrNull {
                it.matcher(component).matches()
            }?.matcher(component)

            if (matcher?.matches() != true) continue
            val skillName = matcher.group("skillName")
            val skillType = SkillType.getByNameOrNull(skillName) ?: return
            val skillInfo = storage?.get(skillType) ?: SkillInfo()
            val skillXP = skillXPInfoMap[skillType] ?: SkillXPInfo()
            val previousTotalXp = skillInfo.totalXp.takeIf { it > 0 }?.toDouble()
            activeSkill = skillType
            when (matcher.pattern()) {
                skillPercentPattern -> handleSkillPatternPercent(matcher, skillType)
                skillMultiplierPattern -> handleSkillPatternMultiplier(matcher, skillType, skillInfo)
            }

            SkillExpGainEvent(
                skillType,
                matcher.group("gained").formatDouble(),
                totalXp = skillInfo.totalXp.toDouble(),
                previousTotalXp = previousTotalXp,
            ).post()

            showDisplay = true
            lastUpdate = SimpleTimeMark.now()
            skillXP.lastUpdate = SimpleTimeMark.now()
            skillXP.sessionTimerActive = true
            SkillProgress.updateDisplay()
            SkillProgress.hideInActionBar = listOf(component)
            return
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        for (message in event.message.removeColor().removeResets().lineSequence().map { it.trim() }) {
            if (lilySplosionStartPattern.matcher(message).matches()) {
                lastLilySplosion = SimpleTimeMark.now()
                continue
            }

            jerryBoxSkillXpPattern.matchMatcher(message) {
                postChatSkillXp(group("skillName"), group("gained").formatLong(), JERRY_BOX_SOURCE)
                return
            }

            giftSkillXpPattern.matchMatcher(message) {
                postChatSkillXp(group("skillName"), group("gained").formatLong(), GIFT_SOURCE)
                return
            }

            lilySplosionSkillXpPattern.matchMatcher(message) {
                if (lastLilySplosion.passedSince() > 5.seconds) return@matchMatcher
                postChatSkillXp(group("skillName"), group("gained").formatLong(), LILY_SPLOSION_SOURCE)
            }
        }
    }

    private fun postChatSkillXp(skillName: String, gained: Long, source: String) {
        val skillType = SkillType.getByNameOrNull(skillName) ?: return
        val effectiveGain = gained * giftXpMultiplier(source)
        val totalXp = addChatSkillXp(skillType, effectiveGain)
        SkillExpGainEvent(
            skill = skillType,
            gained = effectiveGain,
            totalXp = totalXp,
            source = source,
        ).post()

        val skillXP = skillXPInfoMap.getOrPut(skillType, ::SkillXPInfo)
        activeSkill = skillType
        showDisplay = true
        lastUpdate = SimpleTimeMark.now()
        skillXP.lastUpdate = SimpleTimeMark.now()
        skillXP.sessionTimerActive = true
        SkillProgress.updateDisplay()
    }

    private fun addChatSkillXp(skillType: SkillType, gained: Double): Double? {
        val skillInfo = storage?.getOrPut(skillType, ::SkillInfo) ?: return null
        if (skillInfo.totalXp <= 0L) return null

        val totalXp = skillInfo.totalXp + gained
        val roundedTotalXp = totalXp.roundToLong()
        val skillLevel = calculateSkillLevel(roundedTotalXp, skillType.maxLevel)
        skillInfo.apply {
            this.totalXp = roundedTotalXp
            currentXp = skillLevel.xpCurrent
            currentXpMax = skillLevel.xpForNext
            level = skillLevel.level.coerceAtMost(skillType.maxLevel)
            overflowLevel = skillLevel.level
            overflowCurrentXp = skillLevel.xpCurrent
            overflowCurrentXpMax = skillLevel.xpForNext
            overflowTotalXp = skillLevel.overflowXP
            lastGain = gained.addSeparators()
        }
        return totalXp
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.getConstant<NeuSkillLevelJson>("leveling")

        levelArray = data.levelingXP
        levelingMap = levelArray.withIndex().associate { (index, xp) -> (index + 1) to xp }
        exactLevelingMap = levelArray.withIndex().associate { (index, xp) -> xp to (index + 1) }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onAccessoryBagUpdate(event: AccessoryBagUpdateEvent) {
        updateGiftTalismanBonus(event.inventoryItems.values)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemAddInInventory(event: ItemAddInInventoryEvent) {
        val bonus = GIFT_TALISMAN_BONUSES[event.internalName] ?: return
        updateGiftTalismanBonus(bonus)
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Your Skills") return
        for (stack in event.inventoryItems.values) {
            val lore = stack.getLore()
            if (lore.none { it.contains("Click to view!") || it.contains("Not unlocked!") }) continue
            val cleanName = stack.cleanName
            val split = cleanName.split(" ")
            val skillName = split.first()
            val skill = SkillType.getByNameOrNull(skillName) ?: continue
            val skillLevel = if (split.size > 1) split.last().romanToDecimalIfNecessary() else 0
            val skillInfo = storage?.getOrPut(skill, ::SkillInfo) ?: continue

            lore@ for ((index, line) in lore.withIndex()) {
                val cleanLine = line.removeColor()
                if (!cleanLine.startsWith("                    ")) continue@lore
                val previousLine = lore.getOrNull(index - 1)?.removeColor() ?: continue@lore
                val progress = cleanLine.substring(cleanLine.lastIndexOf(' ') + 1)
                if (skillMaxLevelMenuPattern.matches(previousLine)) {
                    onUpdateMax(progress, skill, skillInfo, skillLevel)
                } else {
                    onUpdateNotMax(progress, skillLevel, skillInfo)
                }
            }
        }
    }

    private fun updateGiftTalismanBonus(items: Collection<SafeItemStack>) {
        val bestBonus = items.maxOfOrNull { GIFT_TALISMAN_BONUSES[it.getInternalName()] ?: 0.0 } ?: return
        updateGiftTalismanBonus(bestBonus)
    }

    private fun updateGiftTalismanBonus(bestBonus: Double) {
        // Item and accessory events can miss temporary inventory state, so never downgrade the best known bonus.
        if (bestBonus > giftTalismanBonus) giftTalismanBonus = bestBonus
    }

    private fun giftXpMultiplier(source: String): Double {
        if (source != GIFT_SOURCE) return 1.0

        // Gift chat says the base XP. These bonuses are applied by Hypixel after that.
        return 1.0 + giftTalismanBonus + snowmanMaskBonus() + professionalGifterBonus()
    }

    private fun snowmanMaskBonus(): Double =
        if (InventoryUtils.getHelmet()?.getInternalName() == SNOWMAN_MASK) SNOWMAN_MASK_BONUS else 0.0

    private fun professionalGifterBonus(): Double {
        val tier = ProfileStorageData.profileSpecific?.carnival?.carnivalShopProgress
            ?.entries
            ?.firstOrNull { it.key.equals(SEASON_OF_JERRY_SHOP, ignoreCase = true) }
            ?.value
            ?.entries
            ?.firstOrNull { it.key.equals(PROFESSIONAL_GIFTER, ignoreCase = true) }
            ?.value
            ?: return 0.0

        return when {
            tier <= 0 -> 0.0
            tier == 1 -> PROFESSIONAL_GIFTER_TIER_ONE_BONUS
            else -> PROFESSIONAL_GIFTER_TIER_TWO_BONUS
        }
    }

    private fun onUpdateMax(progress: String, skill: SkillType, skillInfo: SkillInfo, skillLevel: Int) {
        val totalXP = progress.formatLong()
        val cap = skill.maxLevel
        val maxXP = xpRequiredForLevel(cap)
        val currentXP = totalXP - maxXP
        val (overflowLevel, overflowCurrent, overflowNeeded, overflowTotal) = calculateSkillLevel(totalXP, cap)

        skillInfo.apply {
            this.overflowLevel = overflowLevel
            this.overflowCurrentXp = overflowCurrent
            this.overflowCurrentXpMax = overflowNeeded
            this.overflowTotalXp = overflowTotal

            this.totalXp = totalXP
            this.level = skillLevel
            this.currentXp = currentXP
            this.currentXpMax = 0L
        }
    }

    private fun onUpdateNotMax(progress: String, skillLevel: Int, skillInfo: SkillInfo) {
        val splitProgress = progress.split("/")
        val currentXP = splitProgress.first().formatLong()
        val neededXP = splitProgress.last().formatLong()
        val levelXP = calculateLevelXP(skillLevel - 1).toLong()

        skillInfo.apply {
            this.currentXp = currentXP
            this.level = skillLevel
            this.currentXpMax = neededXP
            this.totalXp = levelXP + currentXP

            this.overflowCurrentXp = currentXP
            this.overflowLevel = skillLevel
            this.overflowCurrentXpMax = neededXP
            this.overflowTotalXp = levelXP + currentXP
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Skills")
        val storage = storage
        if (storage == null) {
            event.addIrrelevant("SkillMap is empty")
            return
        }

        event.addIrrelevant {
            val activeSkill = activeSkill
            if (activeSkill == null) {
                add("activeSkill is null")
            } else {
                add("active skill:")
                storage[activeSkill]?.let { skillInfo ->
                    addDebug(activeSkill, skillInfo)
                }
                add("")
                add("")
            }

            for ((skillType, skillInfo) in storage) {
                addDebug(skillType, skillInfo)
            }
        }
    }

    private fun MutableList<String>.addDebug(skillType: SkillType, skillInfo: SkillInfo) {
        add("Name: $skillType")
        add("-  Level: ${skillInfo.level}")
        add("-  CurrentXp: ${skillInfo.currentXp}")
        add("-  CurrentXpMax: ${skillInfo.currentXpMax}")
        add("-  TotalXp: ${skillInfo.totalXp}")
        add("-  OverflowLevel: ${skillInfo.overflowLevel}")
        add("-  OverflowCurrentXp: ${skillInfo.overflowCurrentXp}")
        add("-  OverflowCurrentXpMax: ${skillInfo.overflowCurrentXpMax}")
        add("-  OverflowTotalXp: ${skillInfo.overflowTotalXp}")
        add("-  CustomGoalLevel: ${skillInfo.customGoalLevel}\n")
    }

    private fun handleSkillPatternPercent(matcher: Matcher, skillType: SkillType) {
        var current = 0L
        var needed = 0L
        var isPercentPatternFound = false
        var tablistLevel: Int? = null

        line@ for (line in lastTabComponents) {
            skillTabPattern.matchMatcher(line) {
                if (group("type") == skillType.displayName) {
                    tablistLevel = group("level").toInt()
                    isPercentPatternFound = true
                    if (group("type").lowercase() != activeSkill?.lowercaseName) tablistLevel = null
                    break@line
                }
            }

            maxSkillTabPattern.matchMatcher(line) {
                if (group("type") == skillType.displayName) {
                    tablistLevel = group("level").toInt()
                    if (group("type").lowercase() != activeSkill?.lowercaseName) tablistLevel = null
                    break@line
                }
            }

            skillTabNoPercentPattern.matchMatcher(line) {
                if (group("type") == skillType.displayName) {
                    tablistLevel = group("level").toInt()
                    current = group("current").formatLong()
                    needed = group("needed").formatLong()
                    isPercentPatternFound = false
                    break@line
                }
            }
        }

        val xpPercentage = matcher.group("progress").formatDouble()
        val existingLevel = getSkillInfo(skillType) ?: SkillInfo()
        val level = tablistLevel ?: return
        if (isPercentPatternFound) {
            val levelXP = calculateLevelXP(level - 1)
            val nextLevelDiff = levelArray.getOrNull(level)?.toDouble() ?: 7_600_000.0
            val nextLevelProgress = nextLevelDiff * xpPercentage / 100
            val totalXP = levelXP + nextLevelProgress
            updateSkillInfo(
                existingLevel,
                level,
                nextLevelProgress.toLong(),
                nextLevelDiff.toLong(),
                totalXP.toLong(),
                matcher.group("gained"),
            )
        } else {
            val levelXP = calculateLevelXP(level - 1).toLong() + current
            updateSkillInfo(existingLevel, level, current, needed, levelXP, matcher.group("gained"))
        }
        storage?.set(skillType, existingLevel)
    }

    private fun updateSkillInfo(existingLevel: SkillInfo, level: Int, currentXP: Long, maxXP: Long, totalXP: Long, gained: String) {
        val cap = activeSkill?.maxLevel
        val add = cap?.takeIf { level >= it }?.let {
            xpRequiredForLevel(it)
        } ?: 0

        val (levelOverflow, currentOverflow, currentMaxOverflow, totalOverflow) =
            calculateSkillLevel(totalXP + add, cap ?: 60)

        existingLevel.apply {
            this.totalXp = totalXP
            this.currentXp = currentXP
            this.currentXpMax = maxXP
            this.level = level

            this.overflowTotalXp = totalOverflow
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowLevel = levelOverflow

            this.lastGain = gained
        }
    }

    private fun handleSkillPatternMultiplier(matcher: Matcher, skillType: SkillType, skillInfo: SkillInfo) {
        val currentXP = matcher.group("current").formatLong()
        val maxXP = matcher.group("needed").formatLong()

        // when at overflow, we don't need to subtract one level in the logic below
        val minus = if (maxXP == 0L) 0 else 1
        val level = getLevelExact(maxXP) - minus

        val levelXP = if (maxXP == 0L) currentXP else calculateLevelXP(level - 1).toLong() + currentXP
        val (currentLevel, currentOverflow, currentMaxOverflow, totalOverflow) =
            calculateSkillLevel(levelXP, skillType.maxLevel)

        if (skillInfo.overflowLevel > skillType.maxLevel && currentLevel == skillInfo.overflowLevel + 1) {
            SkillOverflowLevelUpEvent(skillType, skillInfo.overflowLevel, currentLevel).post()
        }

        skillInfo.apply {
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowTotalXp = totalOverflow
            this.overflowLevel = currentLevel

            this.currentXp = currentXP
            this.currentXpMax = maxXP
            this.totalXp = levelXP
            this.level = level

            this.lastGain = matcher.group("gained")
        }
        storage?.set(skillType, skillInfo)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.remove(113, "#profile.skillData.null")
        event.move(138, "#profile.skillData", "#profile.skills.skillData")
        event.move(138, "#profile.giftTalismanSkillXpBonus", "#profile.skills.giftTalismanSkillXpBonus")
        event.remove(138, "#profile.skills.skillData.null")
    }

    @Suppress("ReturnCount")
    private fun onCommand(it: Array<String>) {
        if (it.isEmpty()) {
            commandHelp()
            return
        }

        val first = it.first()
        if (it.size == 1) {
            when (first) {
                "goal" -> {
                    ChatUtils.chat("§bSkill Custom Goal Level")
                    val map = storage?.filter { it.value.customGoalLevel != 0 } ?: return
                    if (map.isEmpty()) {
                        ChatUtils.userError("You haven't set any custom goals yet!")
                    }
                    map.forEach { (skill, data) ->
                        ChatUtils.chat("§e${skill.displayName}: §b${data.customGoalLevel}")
                    }
                    return
                }
            }
        }

        if (it.size == 2) {
            val second = it[1]
            when (first) {
                "levelwithxp" -> {
                    val xp = second.formatLongOrUserError() ?: return
                    val (overflowLevel, current, needed, _) = calculateSkillLevel(xp, 60)
                    ChatUtils.chat(
                        "With §b${xp.addSeparators()} §eXP you would be level §b$overflowLevel " +
                            "§ewith progress (§b${current.addSeparators()}§e/§b${needed.addSeparators()}§e) XP",
                    )
                    return
                }

                "xpforlevel" -> {
                    val level = second.toIntOrNull()
                    if (level == null) {
                        ChatUtils.userError("Not a valid number: '$second'")
                        return
                    }
                    val neededXP = xpRequiredForLevel(level)
                    ChatUtils.chat("You need §b${neededXP.addSeparators()} §eXP to reach level §b${level.toDouble()}")
                    return
                }

                "goal" -> {
                    val rawSkill = it[1].lowercase()
                    val skillType = SkillType.getByNameOrNull(rawSkill)
                    if (skillType == null) {
                        ChatUtils.userError("Unknown Skill type: $rawSkill")
                        return
                    }
                    val skill = storage?.get(skillType) ?: return
                    skill.customGoalLevel = 0
                    ChatUtils.chat("Custom goal level for §b${skillType.displayName} §ereset")
                    return
                }
            }
        }
        if (it.size == 3) {
            when (first) {
                "goal" -> {
                    val rawSkill = it[1].lowercase()
                    val skillType = SkillType.getByNameOrNull(rawSkill)
                    if (skillType == null) {
                        ChatUtils.userError("Unknown Skill type: $rawSkill")
                        return
                    }
                    val rawLevel = it[2]
                    val targetLevel = rawLevel.toIntOrNull()
                    if (targetLevel == null) {
                        ChatUtils.userError("$rawLevel is not a valid number.")
                        return
                    }
                    val skill = storage?.get(skillType) ?: return

                    if (targetLevel <= skill.overflowLevel) {
                        ChatUtils.userError(
                            "Custom goal level ($targetLevel) must be greater than your current level (${skill.overflowLevel}).",
                        )
                        return
                    }

                    skill.customGoalLevel = targetLevel
                    ChatUtils.chat("Custom goal level for §b${skillType.displayName} §eset to §b$targetLevel")
                    return
                }
            }
        }
        commandHelp()
    }

    private fun commandHelp() {
        ChatUtils.chat(
            listOf(
                "§6/shskills levelwithxp <xp> - §bGet a level with the given current XP.",
                "§6/shskills xpforlevel <desiredLevel> - §bGet how much XP you need for a desired level.",
                "§6/shskills goal - §bView your current goal",
                "§6/shskills goal <skill> <level> - §bDefine your goal for <skill>",
                "",
            ).joinToString("\n"),
            prefix = false,
        )
    }

    data class SkillInfo(
        // TODO rename all Xp -> XP
        @Expose var level: Int = 0,
        @Expose var totalXp: Long = 0,
        @Expose var currentXp: Long = 0,
        @Expose var currentXpMax: Long = 0,
        @Expose var overflowLevel: Int = 0,
        @Expose var overflowCurrentXp: Long = 0,
        @Expose var overflowTotalXp: Long = 0,
        @Expose var overflowCurrentXpMax: Long = 0,
        @Expose var lastGain: String = "",
        @Expose var customGoalLevel: Int = 0,
    )

    data class SkillXPInfo(
        var lastTotalXP: Float = 0f,
        var xpGainQueue: LinkedList<Float> = LinkedList(),
        var xpGainHour: Float = 0f,
        var xpGainLast: Float = 0f,
        var timer: Int = 3,
        var sessionTimerActive: Boolean = false,
        var isActive: Boolean = false,
        var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast(),
        var timeActive: Long = 0L,
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shskills") {
            description = "Skills XP/Level related command"
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { onCommand(it) }
            // todo auto complete
            /* autoComplete { args ->
                when (args.size) {
                    1 -> listOf("levelwithxp", "xpforlevel", "goal")
                    2 -> if (args[0].lowercase() == "goal") StringUtils.getListOfStringsMatchingLastWord(
                        args,
                        SkillType.entries.map { it.displayName },
                    ) else listOf()

                    else -> listOf()
                }
            }*/
        }
    }
}
