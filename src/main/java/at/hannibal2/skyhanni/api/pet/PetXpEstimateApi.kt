package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.getSkillInfo
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PetXpEstimateApi {

    private val patternGroup = RepoPattern.group("misc.pet.xp-estimator")

    /**
     * REGEX-TEST: Your Squid leveled up to level 19!
     */
    private val petLevelUpPattern by patternGroup.pattern(
        "chat.level-up.clean",
        "Your (?<pet>.+) leveled up to level (?<level>\\d+)!",
    )

    /**
     * REGEX-TEST: Accessory Bag
     * REGEX-TEST: Accessory Bag (1/2)
     */
    private val accessoryBagNamePattern by patternGroup.pattern(
        "inventory.accessory-bag",
        "Accessory Bag(?: \\(\\d+\\/\\d+\\))?",
    )

    /**
     * REGEX-TEST: Pet Exp Boost: +0.5%
     */
    private val beastmasterPetXpPattern by patternGroup.pattern(
        "inventory.beastmaster.petxp",
        "Pet Exp Boost: \\+(?<amount>[\\d.]+)%",
    )

    private val EXP_SHARE = "PET_ITEM_EXP_SHARE".toInternalName()
    private val ALL_SKILLS_BOOST = "PET_ITEM_ALL_SKILLS_BOOST_COMMON".toInternalName()
    private val ALL_SKILLS_SUPER_BOOST = "ALL_SKILLS_SUPER_BOOST".toInternalName()
    private const val BEASTMASTER_CREST_PREFIX = "BEASTMASTER_CREST_"
    private const val BATTLE_EXPERIENCE_ATTRIBUTE = "Battle Experience"
    private const val WHY_NOT_MORE_ATTRIBUTE = "Why Not More"
    private const val ACTIONBAR_SKILL_SOURCE = "actionbar"
    private const val MAX_DEBUG_ENTRIES = 500
    private const val VISIBLE_GAIN_EPSILON = 1.0000001
    private const val UNKNOWN_TOTAL_FRACTION = 0.5
    private const val MAX_TOTAL_FRACTION = 0.999

    private const val UNSURFACED_PET_XP_THRESHOLD = 5.0

    private const val MAX_GAIN_QUANTA = 12
    private const val MAX_QUANTA_PARTS = 12
    private val GAIN_QUANTA_MAX_AGE = 10.minutes
    private val NON_SKILL_PET_TYPES = setOf("GABAGOOL", "FRACTURED_SOUL")

    private val skillSamples = mutableMapOf<SkillType, SkillProgressSample>()
    private val recentSkillEstimates = mutableMapOf<SkillType, RecentSkillEstimate>()
    private val recentEstimatePetUuids = mutableMapOf<UUID, SimpleTimeMark>()
    private val unsurfacedPetXp = mutableMapOf<UUID, PendingPetXp>()
    private val exactReadDeficits = mutableMapOf<UUID, PendingPetXp>()
    private val recentGainQuanta = mutableMapOf<SkillType, MutableMap<Double, SimpleTimeMark>>()
    private val debugEntries = ArrayDeque<String>()
    private var pendingLevelUp: PendingPetLevelUp? = null
    private var lastAutopetSwap: AutopetSwapContext? = null
    private var debugEntryCounter = 0

    @HandleEvent(onlyOnSkyblock = true)
    fun onSkillExpGain(event: SkillExpGainEvent) {
        val currentPet = CurrentPetApi.currentPet
        val skillReads = readSkillGains(event, currentPet?.uuid)
        skillReads.forEach { updatePetExp(event, it, currentPet) }
    }

    private fun updatePetExp(event: SkillExpGainEvent, skillRead: SkillGainRead, currentPet: PetData?) {
        val targetPet = skillRead.petUuid?.let(::getPetByUuid)
            ?: currentPet.takeIf { skillRead.petUuid == null }
            ?: return
        val targetPetExp = targetPet.exp ?: return
        val skillGain = skillRead.gain.takeIf { it > 0.0 } ?: return

        val estimate = estimatePetXp(targetPet, event.skill, skillGain)
        if (estimate == null) {
            recordSkillDebug(event, skillRead, "skip=pet-not-eligible target=${targetPet.debugPet()}")
            return
        }
        val petXp = estimate.exp

        targetPet.uuid?.let { recentEstimatePetUuids[it] = SimpleTimeMark.now() }
        rememberRecentSkillEstimate(event, targetPet, skillRead)
        val targetExp = targetPet.targetExpAfterGain(targetPetExp, petXp)
        val updatedPet = updateTargetPetExp(targetPet, targetExp)
        val expShareUpdates = updateExpSharePets(event.skill, petXp, targetPet.uuid)
        recordSkillDebug(
            event,
            skillRead,
            "petXp=${petXp.debugFormat()} before=${targetPetExp.debugFormat()} " +
                "target=${targetExp.debugFormat()} updated=${updatedPet != null} " +
                "expShareUpdated=${expShareUpdates.size}${expShareUpdates.debugString()} " +
                "${estimate.debugString()} targetPet=${targetPet.debugPet()}",
        )
        if (updatedPet != null || expShareUpdates.isNotEmpty()) PetStorageApi.markDirty()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val cleanMessage = event.message.removeColor().removeResets()
        petLevelUpPattern.matchMatcher(cleanMessage) {
            val currentPet = CurrentPetApi.currentPet ?: return
            val petName = group("pet")
            if (!petName.equals(currentPet.cleanName, ignoreCase = true)) return

            val level = group("level").toInt()
            val levelExp = PetUtils.levelToXp(level, currentPet.fauxInternalName) ?: return
            val currentExp = currentPet.exp ?: 0.0
            val storedPending = levelExp > currentExp
            if (storedPending) {
                val previousExp = pendingLevelUp
                    ?.takeIf { it.matches(currentPet) }
                    ?.previousExp
                    ?: currentExp
                pendingLevelUp = PendingPetLevelUp(
                    currentPet.uuid,
                    currentPet.fauxInternalName,
                    previousExp,
                    levelExp,
                )
            }

            val updatedPet = CurrentPetApi.updateCurrentPetExp(levelExp)
            recordDebug(
                "level-up pet='$petName' level=$level before=${currentExp.debugFormat()} " +
                    "levelExp=${levelExp.debugFormat()} pending=$storedPending updated=${updatedPet != null} " +
                    currentPet.debugPet(),
            )
            if (updatedPet != null) PetStorageApi.markDirty()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!accessoryBagNamePattern.matches(event.inventoryName)) return
        val bestBeastmasterMultiplier = event.inventoryItems.values
            .mapNotNull { it.readBeastmasterMultiplierOrNull() }
            .maxOrNull()
            ?: return

        val petStorage = ProfileStorageData.petProfiles ?: return
        if (bestBeastmasterMultiplier <= (petStorage.beastmasterPetXpMultiplier ?: 1.0)) return

        petStorage.beastmasterPetXpMultiplier = bestBeastmasterMultiplier
        PetStorageApi.markDirty()
    }

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        resetSessionState()
    }

    @HandleEvent
    fun onWorldChange() {
        resetSessionState()
    }

    private fun resetSessionState() {
        skillSamples.clear()
        recentSkillEstimates.clear()
        recentEstimatePetUuids.clear()
        unsurfacedPetXp.clear()
        exactReadDeficits.clear()
        recentGainQuanta.clear()
        pendingLevelUp = null
    }

    fun recordPetDataRead(
        source: String,
        petData: PetData,
        exact: Boolean,
        previousExp: Double? = null,
        appliedExp: Double? = null,
    ) {
        val appliedDelta = appliedExp?.let { applied -> previousExp?.let { applied - it } }
        val resynced = exact && petData.resyncSkillFractionFromExactRead(appliedDelta)
        if (exact && !resynced) {
            petData.reconcileExactReadDelta(appliedDelta)
        }
        recordDebug(
            "pet-read source=$source exact=$exact previous=${previousExp.debugFormat()} " +
                "applied=${appliedExp.debugFormat()} delta=${appliedDelta.debugFormat()} " +
                "resynced=$resynced ${petData.debugPet()}",
        )
    }

    private fun PetData.reconcileExactReadDelta(appliedDelta: Double?) {
        val petUuid = uuid ?: return
        if (appliedDelta == null) return
        if (recentEstimatePetUuids[petUuid]?.let { it.passedSince() <= 10.minutes } != true) return
        unsurfacedPetXp.prune(10.minutes)
        exactReadDeficits.prune(60.seconds)
        when {
            appliedDelta > UNSURFACED_PET_XP_THRESHOLD -> {
                val surplus = cancelPendingPetXp(exactReadDeficits, this, appliedDelta)
                if (surplus <= UNSURFACED_PET_XP_THRESHOLD) return
                val pendingPetXp = (unsurfacedPetXp[petUuid]?.petXp ?: 0.0) + surplus
                unsurfacedPetXp[petUuid] = PendingPetXp(pendingPetXp)
                recordDebug("unsurfaced-record petXp=${surplus.debugFormat()} pending=${pendingPetXp.debugFormat()} ${debugPet()}")
            }
            appliedDelta < -UNSURFACED_PET_XP_THRESHOLD -> {
                val deficit = cancelPendingPetXp(unsurfacedPetXp, this, -appliedDelta)
                if (deficit <= UNSURFACED_PET_XP_THRESHOLD) return
                val pendingPetXp = (exactReadDeficits[petUuid]?.petXp ?: 0.0) + deficit
                exactReadDeficits[petUuid] = PendingPetXp(pendingPetXp)
                recordDebug("deficit-record petXp=${deficit.debugFormat()} pending=${pendingPetXp.debugFormat()} ${debugPet()}")
            }
        }
    }

    private fun cancelPendingPetXp(
        pendingMap: MutableMap<UUID, PendingPetXp>,
        sourcePet: PetData,
        sourcePetXp: Double,
    ): Double {
        if (pendingMap.isEmpty()) return sourcePetXp
        val skill = recentSkillEstimates.maxByOrNull { it.value.createdAt }?.key ?: return sourcePetXp
        val sourceMultiplier = estimatePetXp(sourcePet, skill, 1.0)?.exp?.takeIf { it > 0.0 } ?: return sourcePetXp
        var remainingSkillXp = sourcePetXp / sourceMultiplier
        val iterator = pendingMap.iterator()
        while (iterator.hasNext() && remainingSkillXp > 0.0) {
            val (petUuid, pending) = iterator.next()
            val petData = getPetByUuid(petUuid) ?: continue
            val multiplier = estimatePetXp(petData, skill, 1.0)?.exp?.takeIf { it > 0.0 } ?: continue
            val cancelledSkillXp = (pending.petXp / multiplier).coerceAtMost(remainingSkillXp)
            remainingSkillXp -= cancelledSkillXp
            pending.petXp -= cancelledSkillXp * multiplier
            recordDebug(
                "pending-cancel skill=${skill.displayName} skillXp=${cancelledSkillXp.debugFormat()} " +
                    "remainingPetXp=${pending.petXp.debugFormat()} ${petData.debugPet()}",
            )
            if (pending.petXp <= UNSURFACED_PET_XP_THRESHOLD) iterator.remove()
        }
        return remainingSkillXp * sourceMultiplier
    }

    private fun MutableMap<UUID, PendingPetXp>.prune(maxAge: Duration) {
        values.removeIf { it.createdAt.passedSince() > maxAge }
    }

    private fun rememberGainQuantum(skill: SkillType, gained: Double) {
        val quanta = recentGainQuanta.getOrPut(skill) { mutableMapOf() }
        quanta[gained] = SimpleTimeMark.now()
        if (quanta.size > MAX_GAIN_QUANTA) quanta.minByOrNull { it.value }?.let { quanta.remove(it.key) }
    }

    private fun decomposeDisplayedGain(skill: SkillType, displayedGain: Double): Double? {
        val quantaMap = recentGainQuanta[skill] ?: return null
        quantaMap.values.removeIf { it.passedSince() > GAIN_QUANTA_MAX_AGE }
        val quanta = quantaMap.keys.sortedDescending()
        if (quanta.isEmpty()) return null

        val minQuantum = quanta.last()
        val candidateTenths = mutableSetOf<Long>()

        fun search(startIndex: Int, sum: Double, parts: Int) {
            if (candidateTenths.size > 1) return
            if (sum > displayedGain - 1.0) candidateTenths.add((sum * 10.0).roundToLong())
            if (parts >= MAX_QUANTA_PARTS || sum + minQuantum >= displayedGain + 1.0) return
            for (index in startIndex until quanta.size) {
                val next = sum + quanta[index]
                if (next >= displayedGain + 1.0) continue
                search(index, next, parts + 1)
            }
        }
        search(0, 0.0, 0)

        return candidateTenths.singleOrNull()?.div(10.0)
    }

    private fun consumeUnsurfacedXp(skill: SkillType, availableGain: Double): Double {
        var remaining = availableGain
        var discarded = 0.0
        val iterator = unsurfacedPetXp.iterator()
        while (iterator.hasNext() && remaining > 0.0) {
            val (petUuid, surplus) = iterator.next()
            if (surplus.createdAt.passedSince() > 10.minutes) {
                iterator.remove()
                continue
            }
            val petData = getPetByUuid(petUuid) ?: continue
            val multiplier = estimatePetXp(petData, skill, 1.0)?.exp?.takeIf { it > 0.0 } ?: continue
            val skillXp = (surplus.petXp / multiplier).coerceAtMost(remaining)
            remaining -= skillXp
            discarded += skillXp
            surplus.petXp -= skillXp * multiplier
            recordDebug(
                "unsurfaced-discard skill=${skill.displayName} skillXp=${skillXp.debugFormat()} " +
                    "remainingPetXp=${surplus.petXp.debugFormat()} ${petData.debugPet()}",
            )
            if (surplus.petXp <= UNSURFACED_PET_XP_THRESHOLD) iterator.remove()
        }
        return discarded
    }

    fun shouldRecordPetMenuRead(uuid: UUID?): Boolean {
        val lastEstimate = recentEstimatePetUuids[uuid] ?: return false
        return lastEstimate.passedSince() <= 60.seconds
    }

    fun recordAutopetSwap(petUuid: UUID?, trigger: String?) {
        lastAutopetSwap = AutopetSwapContext(petUuid, trigger)
        recordDebug("autopet-swap petUuid=$petUuid trigger='${trigger ?: "<unknown>"}'")
    }

    private fun readSkillGains(event: SkillExpGainEvent, currentPetUuid: UUID?): List<SkillGainRead> {
        val totalXp = event.totalXp?.takeIf { it > 0.0 }
        if (event.source == ACTIONBAR_SKILL_SOURCE && event.gained > 0.0) rememberGainQuantum(event.skill, event.gained)
        val previous = skillSamples[event.skill]
        val previousTotalXp = previous?.totalXp
        fun read(gain: Double, petUuid: UUID?, source: String) =
            SkillGainRead(gain, totalXp, previousTotalXp, petUuid, source)
        fun singleRead(gain: Double, petUuid: UUID?, source: String) = listOf(read(gain, petUuid, source))

        if (event.source != ACTIONBAR_SKILL_SOURCE) {
            val expectedTotalXp = previousTotalXp?.plus(event.gained)
            val inferredTotalXp = listOfNotNull(totalXp, expectedTotalXp).maxOrNull()
            val inferredPreciseTotalXp = inferredTotalXp?.let { total ->
                if (
                    previous?.preciseTotalXp != null &&
                    expectedTotalXp != null &&
                    abs(total - expectedTotalXp) <= VISIBLE_GAIN_EPSILON
                ) previous.preciseTotalXp + event.gained else total + UNKNOWN_TOTAL_FRACTION
            }
            return rememberSkillSampleAndReturn(
                event.skill,
                inferredTotalXp,
                inferredPreciseTotalXp,
                currentPetUuid,
                listOf(SkillGainRead(event.gained, inferredTotalXp, previousTotalXp, currentPetUuid, event.source)),
            )
        }

        when {
            totalXp != null && previousTotalXp != null -> when {
                totalXp > previousTotalXp -> {
                    val previousPreciseTotalXp = previous.preciseTotalXp ?: previousTotalXp
                    val displayedGain = totalXp - previousTotalXp
                    val useVisibleGain = displayedGain.isRoundedVisibleGain(event.gained)
                    val decomposedGain = if (useVisibleGain) null else decomposeDisplayedGain(event.skill, displayedGain)
                    val unclampedTotalXp =
                        previousPreciseTotalXp + (decomposedGain ?: if (useVisibleGain) event.gained else displayedGain)
                    val preciseTotalXp = unclampedTotalXp.coerceIn(totalXp, totalXp + MAX_TOTAL_FRACTION)
                    val preciseGain = preciseTotalXp - previousPreciseTotalXp
                    val totalGain = preciseGain - consumeUnsurfacedXp(event.skill, preciseGain)
                    val previousPetUuid = previous.petUuid
                    val swappedPet = previousPetUuid != null && previousPetUuid != currentPetUuid
                    val autopetSwap = lastAutopetSwap?.takeIf {
                        it.petUuid == currentPetUuid && it.createdAt.passedSince() <= 5.seconds
                    }
                    val reads = if (swappedPet && autopetSwap != null) {
                        val visibleGain = event.gained.coerceIn(0.0, totalGain)
                        val hiddenGain = totalGain - visibleGain
                        when {
                            !autopetSwap.isSpawnTrigger -> singleRead(
                                totalGain,
                                currentPetUuid,
                                "autopet-boundary-current",
                            )

                            hiddenGain > visibleGain + VISIBLE_GAIN_EPSILON -> listOf(
                                read(
                                    hiddenGain,
                                    previousPetUuid,
                                    "autopet-boundary-hidden-previous",
                                ),
                                read(
                                    visibleGain,
                                    currentPetUuid,
                                    "autopet-boundary-visible-current",
                                ),
                            )

                            else -> singleRead(
                                totalGain,
                                previousPetUuid,
                                "autopet-boundary-previous",
                            )
                        }
                    } else singleRead(
                        totalGain,
                        currentPetUuid,
                        when {
                            useVisibleGain -> "visible-gain"
                            swappedPet -> "pet-swap-current-gain"
                            decomposedGain != null -> "total-diff-quanta"
                            else -> "total-diff"
                        },
                    )
                    return rememberSkillSampleAndReturn(event.skill, totalXp, preciseTotalXp, currentPetUuid, reads)
                }

                totalXp == previousTotalXp -> return rememberSkillSampleAndReturn(
                    event.skill,
                    totalXp,
                    previous.preciseTotalXp ?: totalXp,
                    currentPetUuid,
                    singleRead(0.0, currentPetUuid, "same-total"),
                )

                else -> return rememberSkillSampleAndReturn(
                    event.skill,
                    totalXp,
                    totalXp + UNKNOWN_TOTAL_FRACTION,
                    currentPetUuid,
                    singleRead(event.gained, currentPetUuid, "backwards-event-gain"),
                )
            }

            else -> return rememberSkillSampleAndReturn(
                event.skill,
                totalXp,
                totalXp?.plus(UNKNOWN_TOTAL_FRACTION),
                currentPetUuid,
                singleRead(event.gained, currentPetUuid, "event-gain"),
            )
        }
    }

    private fun rememberSkillSampleAndReturn(
        skill: SkillType,
        totalXp: Double?,
        preciseTotalXp: Double?,
        currentPetUuid: UUID?,
        result: List<SkillGainRead>,
    ): List<SkillGainRead> {
        val sample = SkillProgressSample(totalXp, preciseTotalXp, currentPetUuid)
        skillSamples[skill] = sample
        return result
    }

    private fun Double.isRoundedVisibleGain(visibleGain: Double): Boolean =
        visibleGain > 0.0 && abs(this - visibleGain) <= VISIBLE_GAIN_EPSILON

    private fun rememberRecentSkillEstimate(event: SkillExpGainEvent, targetPet: PetData, skillRead: SkillGainRead) {
        if (skillRead.gain <= 0.0 || targetPet.uuid == null || skillRead.petUuid != targetPet.uuid) return
        estimatePetXp(targetPet, event.skill, skillRead.gain) ?: return
        recentSkillEstimates[event.skill] = RecentSkillEstimate(targetPet.uuid)
    }

    private fun PetData.resyncSkillFractionFromExactRead(appliedDelta: Double?): Boolean {
        val petUuid = uuid ?: return false

        val skill = recentSkillEstimates.asSequence()
            .filter { it.value.petUuid == petUuid && it.value.createdAt.passedSince() <= 10.minutes }
            .maxByOrNull { it.value.createdAt }
            ?.key ?: return false
        val sample = skillSamples[skill] ?: return false
        val totalXp = sample.totalXp ?: return false
        val precise = sample.preciseTotalXp ?: return false
        val multiplier = estimatePetXp(this, skill, 1.0)?.exp?.takeIf { it > 0.0 } ?: return false
        val skillDelta = appliedDelta?.takeIf { it != 0.0 }?.let { it / multiplier } ?: return false
        if (abs(skillDelta) >= 1.0) return false

        val resynced = (precise + skillDelta).coerceIn(totalXp, totalXp + MAX_TOTAL_FRACTION)
        if (resynced == precise) return false
        val updatedSample = SkillProgressSample(totalXp, resynced, sample.petUuid)
        skillSamples[skill] = updatedSample
        return true
    }

    private fun estimatePetXp(petData: PetData, skill: SkillType, skillXp: Double): PetXpEstimate? {
        val petType = PetUtils.getPetType(petData.fauxInternalName) ?: return null
        val baseMultiplier = skillBaseMultiplier(petType, skill) ?: return null
        val tamingLevel = getSkillInfo(SkillType.TAMING)?.level?.coerceIn(0, SkillType.TAMING.maxLevel) ?: 0
        val tamingMultiplier = 1.0 + tamingLevel / 100.0
        val dianaMultiplier = if (Perk.PET_XP_BUFF.isActive) 1.35 else 1.0
        val beastmasterMultiplier = getBeastmasterMultiplier()
        val itemMultiplier = petData.heldItemInternalName.petItemMultiplier(skill)
        val battleExperienceMultiplier = battleExperienceMultiplier(skill)
        val customMultiplier = PetUtils.getPetXpMultiplier(petData.fauxInternalName)
        val totalMultiplier = baseMultiplier * tamingMultiplier * dianaMultiplier *
            beastmasterMultiplier * itemMultiplier * battleExperienceMultiplier * customMultiplier

        return PetXpEstimate(
            exp = skillXp * totalMultiplier,
            multiplier = totalMultiplier,
            petType = petType,
            baseMultiplier = baseMultiplier,
            tamingLevel = tamingLevel,
            tamingMultiplier = tamingMultiplier,
            dianaMultiplier = dianaMultiplier,
            beastmasterMultiplier = beastmasterMultiplier,
            itemMultiplier = itemMultiplier,
            battleExperienceMultiplier = battleExperienceMultiplier,
            customMultiplier = customMultiplier,
        )
    }

    private fun skillBaseMultiplier(petType: String, skill: SkillType): Double? = when (skill) {
        SkillType.TAMING,
        SkillType.CARPENTRY,
        -> null

        else -> when {
            petType.replace(" ", "_") in NON_SKILL_PET_TYPES -> null
            petType == "ALL" -> 1.0
            petType == skill.uppercaseName -> matchingSkillMultiplier(skill)
            else -> nonMatchingSkillMultiplier(skill)
        }
    }

    private fun matchingSkillMultiplier(skill: SkillType): Double = when (skill) {
        SkillType.MINING,
        SkillType.FISHING,
        -> 1.5

        else -> 1.0
    }

    private fun nonMatchingSkillMultiplier(skill: SkillType): Double = when (skill) {
        SkillType.MINING,
        SkillType.FISHING,
        -> 0.5

        SkillType.ENCHANTING,
        SkillType.ALCHEMY,
        -> 1.0 / 12.0

        else -> 1.0 / 3.0
    }

    private fun NeuInternalName?.petItemMultiplier(skill: SkillType): Double {
        val internalName = this ?: return 1.0
        if (internalName == ALL_SKILLS_BOOST) return 1.1
        if (internalName == ALL_SKILLS_SUPER_BOOST) return 1.2

        val rawName = internalName.asString()
        val prefix = "PET_ITEM_${skill.uppercaseName}_SKILL_BOOST_"
        if (!rawName.startsWith(prefix)) return 1.0
        return when (rawName.removePrefix(prefix)) {
            "COMMON" -> 1.2
            "UNCOMMON" -> 1.3
            "RARE" -> 1.4
            "EPIC" -> 1.5
            else -> 1.0
        }
    }

    private fun getBeastmasterMultiplier(): Double =
        (InventoryUtils.getItemsInOwnInventory() + EquipmentApi.getAll())
            .asSequence()
            .mapNotNull { it.readBeastmasterMultiplierOrNull() }
            .maxOrNull()
            ?: ProfileStorageData.petProfiles?.beastmasterPetXpMultiplier
            ?: 1.0

    private fun SafeItemStack.readBeastmasterMultiplierOrNull(): Double? {
        val internalName = getInternalNameOrNull()?.asString() ?: return null
        if (!internalName.startsWith(BEASTMASTER_CREST_PREFIX)) return null
        val amount = beastmasterPetXpPattern.firstMatcher(getLore().map { it.removeColor() }) {
            group("amount").formatDouble()
        } ?: return null
        return 1.0 + amount / 100.0
    }

    private fun updateExpSharePets(skill: SkillType, sourcePetXp: Double, currentPetUuid: UUID?): List<ExpShareUpdate> {
        val baseRate = expShareBaseRate()
        val whyNotMoreRate = whyNotMoreExpShareRate()
        return PetStorageApi.getActiveExpSharePets().mapNotNull { petData ->
            val currentExp = petData.exp ?: return@mapNotNull null
            if (petData.uuid == currentPetUuid) return@mapNotNull null

            val itemRate = if (petData.heldItemInternalName == EXP_SHARE) 0.15 else 0.0
            // Server quirk: Why Not More is extra share rate, not a final multiplier.
            val rate = baseRate + whyNotMoreRate + itemRate
            if (rate <= 0.0) return@mapNotNull null

            val petType = PetUtils.getPetType(petData.fauxInternalName) ?: return@mapNotNull null
            // EXP Share uses the equipped pet XP, then only the shared pet's base skill type.
            val sharedPetBaseMultiplier = skillBaseMultiplier(petType, skill) ?: return@mapNotNull null
            val effectiveRate = rate * sharedPetBaseMultiplier
            val gain = sourcePetXp * effectiveRate
            val targetExp = currentExp + gain
            petData.exp = targetExp
            petData.uuid?.let { recentEstimatePetUuids[it] = SimpleTimeMark.now() }
            ExpShareUpdate(
                petName = petData.getUserFriendlyName().removeColor().removeResets(),
                uuid = petData.uuid,
                sourcePetXp = sourcePetXp,
                rate = rate,
                petType = petType,
                sharedPetBaseMultiplier = sharedPetBaseMultiplier,
                effectiveRate = effectiveRate,
                whyNotMoreRate = whyNotMoreRate,
                itemRate = itemRate,
                gain = gain,
                before = currentExp,
                target = targetExp,
            )
        }
    }

    private fun getPetByUuid(uuid: UUID): PetData? =
        ProfileStorageData.petProfiles?.pets?.firstOrNull { it.uuid == uuid }

    private fun updateTargetPetExp(petData: PetData, exp: Double): PetData? {
        if (petData.uuid == CurrentPetApi.currentPet?.uuid) {
            return CurrentPetApi.updateCurrentPetExp(exp)
        }

        val currentExp = petData.exp ?: 0.0
        if (exp <= currentExp) return null
        petData.exp = exp
        return petData
    }

    private fun expShareBaseRate(): Double {
        val tamingLevel = getSkillInfo(SkillType.TAMING)?.level?.coerceIn(0, SkillType.TAMING.maxLevel) ?: 0
        val dianaRate = if (Perk.SHARING_IS_CARING.isActive) 0.10 else 0.0
        return tamingLevel * 0.002 + dianaRate
    }

    private fun whyNotMoreExpShareRate(): Double =
        AttributeShardsData.getActiveLevelByAbilityName(WHY_NOT_MORE_ATTRIBUTE) / 100.0

    private fun battleExperienceMultiplier(skill: SkillType): Double =
        if (skill == SkillType.COMBAT) {
            1.0 + AttributeShardsData.getActiveLevelByAbilityName(BATTLE_EXPERIENCE_ATTRIBUTE) / 100.0
        } else 1.0

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shpetxpdebug") {
            description = "Copies pet XP estimator debug data to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { copyDebugEntries() }
        }
        event.registerBrigadier("shclearpetxpdebug") {
            description = "Clears pet XP estimator debug data"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { clearDebugEntries() }
        }
    }

    private fun recordSkillDebug(event: SkillExpGainEvent, skillRead: SkillGainRead, message: String) {
        if (skillRead.gain <= 0.0) return
        val actionBar = ActionBarData.getActionBar().removeColor().removeResets()
        val totalGain = skillRead.totalXp?.let { total ->
            skillRead.previousTotalXp?.let { total - it }
        }
        val visibleDelta = totalGain?.minus(event.gained)
        recordDebug(
            "skill=${event.skill.displayName} source=${skillRead.source} " +
                "eventGain=${event.gained.debugFormat()} readGain=${skillRead.gain.debugFormat()} " +
                "total=${skillRead.totalXp.debugFormat()} previous=${skillRead.previousTotalXp.debugFormat()} " +
                "preciseTotal=${skillSamples[event.skill]?.preciseTotalXp.debugFormat()} " +
                "totalGain=${totalGain.debugFormat()} visibleDelta=${visibleDelta.debugFormat()} " +
                "actionbar='$actionBar' $message",
        )
    }

    private fun recordDebug(message: String) {
        debugEntries.addLast("#${++debugEntryCounter} $message")
        while (debugEntries.size > MAX_DEBUG_ENTRIES) debugEntries.removeFirst()
    }

    private fun copyDebugEntries() {
        val output = buildString {
            appendLine("Pet XP Estimator Debug entries=${debugEntries.size}")
            debugEntries.forEach { appendLine(it) }
        }
        OSUtils.copyToClipboard(output)
        ChatUtils.chat("Copied ${debugEntries.size} pet XP estimator debug entries to clipboard.")
    }

    private fun clearDebugEntries() {
        debugEntries.clear()
        debugEntryCounter = 0
        resetSessionState()
        ChatUtils.chat("Cleared pet XP estimator debug entries and reset estimator state.")
    }

    private fun PetData.debugPet() =
        "pet='${getUserFriendlyName().removeColor().removeResets()}' uuid=$uuid level=$level " +
            "exp=${exp.debugFormat()} held=${heldItemInternalName?.asString() ?: "<none>"}"

    private fun Double?.debugFormat() = this?.let { String.format(Locale.US, "%.3f", it) } ?: "null"

    private fun Double.preciseDebugFormat() = String.format(Locale.US, "%.6f", this)

    private fun PetData.targetExpAfterGain(currentExp: Double, petXp: Double): Double {
        val pending = pendingLevelUp ?: return currentExp + petXp
        if (!pending.matches(this) || pending.createdAt.passedSince() > 5.seconds) {
            pendingLevelUp = null
            return currentExp + petXp
        }

        pendingLevelUp = null
        if (currentExp > pending.levelExp) return currentExp + petXp
        return maxOf(pending.previousExp + petXp, pending.levelExp)
    }

    private data class SkillProgressSample(
        val totalXp: Double?,
        val preciseTotalXp: Double?,
        val petUuid: UUID?,
    )

    private data class RecentSkillEstimate(
        val petUuid: UUID?,
        val createdAt: SimpleTimeMark = SimpleTimeMark.now(),
    )

    private data class PendingPetXp(
        var petXp: Double,
        val createdAt: SimpleTimeMark = SimpleTimeMark.now(),
    )

    private data class SkillGainRead(
        val gain: Double,
        val totalXp: Double?,
        val previousTotalXp: Double?,
        val petUuid: UUID?,
        val source: String,
    )

    private data class PetXpEstimate(
        val exp: Double,
        val multiplier: Double,
        val petType: String,
        val baseMultiplier: Double,
        val tamingLevel: Int,
        val tamingMultiplier: Double,
        val dianaMultiplier: Double,
        val beastmasterMultiplier: Double,
        val itemMultiplier: Double,
        val battleExperienceMultiplier: Double,
        val customMultiplier: Double,
    ) {
        fun debugString() =
            "multiplier=${multiplier.preciseDebugFormat()} petType='$petType' " +
                "base=${baseMultiplier.preciseDebugFormat()} tamingLevel=$tamingLevel " +
                "taming=${tamingMultiplier.preciseDebugFormat()} diana=${dianaMultiplier.preciseDebugFormat()} " +
                "beastmaster=${beastmasterMultiplier.preciseDebugFormat()} item=${itemMultiplier.preciseDebugFormat()} " +
                "battle=${battleExperienceMultiplier.preciseDebugFormat()} custom=${customMultiplier.preciseDebugFormat()}"
    }

    private fun List<ExpShareUpdate>.debugString() =
        takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = " expShare=[", postfix = "]", separator = "; ") { it.debugString() }
            .orEmpty()

    private data class ExpShareUpdate(
        val petName: String,
        val uuid: UUID?,
        val sourcePetXp: Double,
        val rate: Double,
        val petType: String,
        val sharedPetBaseMultiplier: Double,
        val effectiveRate: Double,
        val whyNotMoreRate: Double,
        val itemRate: Double,
        val gain: Double,
        val before: Double,
        val target: Double,
    ) {
        fun debugString() =
            "pet='$petName' uuid=$uuid sourcePetXp=${sourcePetXp.debugFormat()} " +
                "rate=${rate.preciseDebugFormat()} petType='$petType' " +
                "base=${sharedPetBaseMultiplier.preciseDebugFormat()} effectiveRate=${effectiveRate.preciseDebugFormat()} " +
                "whyNotMoreRate=${whyNotMoreRate.preciseDebugFormat()} " +
                "itemRate=${itemRate.preciseDebugFormat()} gain=${gain.debugFormat()} " +
                "before=${before.debugFormat()} target=${target.debugFormat()}"
    }

    private data class PendingPetLevelUp(
        val uuid: UUID?,
        val internalName: NeuInternalName,
        val previousExp: Double,
        val levelExp: Double,
        val createdAt: SimpleTimeMark = SimpleTimeMark.now(),
    ) {
        fun matches(petData: PetData): Boolean =
            uuid?.let { petData.uuid == it } ?: (petData.fauxInternalName == internalName)
    }

    private data class AutopetSwapContext(
        val petUuid: UUID?,
        val trigger: String?,
        val createdAt: SimpleTimeMark = SimpleTimeMark.now(),
    ) {
        val isSpawnTrigger get() = trigger?.contains("spawn", ignoreCase = true) == true
    }
}
