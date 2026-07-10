package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.AccessoryBagUpdateEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.skyblock.SkyblockEquipmentDataUpdateEvent
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
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong
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
    private const val VISIBLE_GAIN_EPSILON = 1.0000001
    private const val UNKNOWN_TOTAL_FRACTION = 0.5
    private const val MAX_TOTAL_FRACTION = 0.999

    private const val MAX_GAIN_QUANTA = 12
    private const val MAX_QUANTA_PARTS = 12
    private val GAIN_QUANTA_MAX_AGE = 10.minutes
    private val SPAWN_AUTOPET_MAX_AGE = 5.seconds
    private val NON_SKILL_PET_TYPES = setOf("GABAGOOL", "FRACTURED_SOUL")

    private val skillSamples = mutableMapOf<SkillType, SkillProgressSample>()
    private val recentSkillEstimates = mutableMapOf<SkillType, RecentSkillEstimate>()
    private val recentEstimatePetUuids = mutableMapOf<UUID, SimpleTimeMark>()
    private val recentGainQuanta = mutableMapOf<SkillType, MutableMap<Double, SimpleTimeMark>>()
    private var beastmasterMultiplierCache: Double? = null
    private var pendingLevelUp: PendingPetLevelUp? = null
    private var pendingSpawnAutopetSwap: SpawnAutopetContext? = null

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

        val petXp = estimatePetXp(targetPet, event.skill, skillGain) ?: return

        targetPet.uuid?.let { recentEstimatePetUuids[it] = SimpleTimeMark.now() }
        rememberRecentSkillEstimate(event, targetPet, skillRead)
        val targetExp = targetPet.targetExpAfterGain(targetPetExp, petXp)
        val updatedPet = updateTargetPetExp(targetPet, targetExp)
        val expShareUpdated = updateExpSharePets(event.skill, petXp, targetPet.uuid)
        if (updatedPet != null || expShareUpdated) PetStorageApi.markDirty()
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

            if (CurrentPetApi.updateCurrentPetExp(levelExp) != null) PetStorageApi.markDirty()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onAccessoryBagUpdate(event: AccessoryBagUpdateEvent) {
        val currentBeastmasterMultiplier = event.inventoryItems.values
            .mapNotNull { it.readBeastmasterMultiplierOrNull() }
            .maxOrNull()

        val petStorage = ProfileStorageData.petProfiles ?: return
        if (currentBeastmasterMultiplier == petStorage.beastmasterPetXpMultiplier) return

        petStorage.beastmasterPetXpMultiplier = currentBeastmasterMultiplier
        clearBeastmasterMultiplierCache()
        PetStorageApi.markDirty()
    }

    @HandleEvent(OwnInventoryItemUpdateEvent::class)
    fun onOwnInventoryItemUpdate() {
        clearBeastmasterMultiplierCache()
    }

    @HandleEvent(SkyblockEquipmentDataUpdateEvent::class)
    fun onSkyblockEquipmentDataUpdate() {
        clearBeastmasterMultiplierCache()
    }

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        resetProfileState()
    }

    @HandleEvent
    fun onWorldChange() {
        resetWorldState()
    }

    private fun resetProfileState() {
        skillSamples.clear()
        recentGainQuanta.clear()
        clearBeastmasterMultiplierCache()
        resetWorldState()
    }

    private fun resetWorldState() {
        recentSkillEstimates.clear()
        recentEstimatePetUuids.clear()
        pendingLevelUp = null
        pendingSpawnAutopetSwap = null
    }

    fun recordPetDataRead(
        petData: PetData,
        exact: Boolean,
        previousExp: Double? = null,
        appliedExp: Double? = null,
    ) {
        val appliedDelta = appliedExp?.let { applied -> previousExp?.let { applied - it } }
        if (exact) petData.resyncSkillFractionFromExactRead(appliedDelta)
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

    fun shouldRecordPetMenuRead(uuid: UUID?): Boolean {
        val lastEstimate = recentEstimatePetUuids[uuid] ?: return false
        return lastEstimate.passedSince() <= 60.seconds
    }

    fun recordAutopetSwap(petData: PetData, previousPet: PetData?, trigger: String?) {
        pendingSpawnAutopetSwap = if (trigger?.contains("spawn", ignoreCase = true) == true) {
            SpawnAutopetContext(petData.uuid, previousPet?.uuid)
        } else null
    }

    private fun readSkillGains(event: SkillExpGainEvent, currentPetUuid: UUID?): List<SkillGainRead> {
        val totalXp = event.totalXp?.takeIf { it > 0.0 }
        if (event.source == ACTIONBAR_SKILL_SOURCE && event.gained > 0.0) rememberGainQuantum(event.skill, event.gained)
        val previous = skillSamples[event.skill]
        val seededPreviousTotalXp = event.previousTotalXp?.takeIf { previous == null && totalXp != null && it < totalXp }
        val previousTotalXp = previous?.totalXp ?: seededPreviousTotalXp

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
                singleSkillGainRead(
                    event.gained,
                    inferredTotalXp,
                    previousTotalXp,
                    currentPetUuid,
                ),
            )
        }

        when {
            totalXp != null && previousTotalXp != null -> when {
                totalXp > previousTotalXp -> {
                    val previousPreciseTotalXp = previous?.preciseTotalXp ?: previousTotalXp
                    val displayedGain = totalXp - previousTotalXp
                    val useVisibleGain = displayedGain.isRoundedVisibleGain(event.gained)
                    val decomposedGain = if (useVisibleGain) null else decomposeDisplayedGain(event.skill, displayedGain)
                    val unclampedTotalXp =
                        previousPreciseTotalXp + (decomposedGain ?: if (useVisibleGain) event.gained else displayedGain)
                    val preciseTotalXp = unclampedTotalXp.coerceIn(totalXp, totalXp + MAX_TOTAL_FRACTION)
                    val reads = readIncreasingActionbarSkillGains(
                        event,
                        preciseTotalXp - previousPreciseTotalXp,
                        totalXp,
                        previousTotalXp,
                        previous,
                        currentPetUuid,
                    )
                    return rememberSkillSampleAndReturn(event.skill, totalXp, preciseTotalXp, currentPetUuid, reads)
                }

                totalXp == previousTotalXp -> return rememberSkillSampleAndReturn(
                    event.skill,
                    totalXp,
                    previous?.preciseTotalXp ?: totalXp,
                    currentPetUuid,
                    singleSkillGainRead(0.0, totalXp, previousTotalXp, currentPetUuid),
                )

                else -> return rememberSkillSampleAndReturn(
                    event.skill,
                    totalXp,
                    totalXp + UNKNOWN_TOTAL_FRACTION,
                    currentPetUuid,
                    singleSkillGainRead(
                        event.gained,
                        totalXp,
                        previousTotalXp,
                        currentPetUuid,
                    ),
                )
            }

            else -> return rememberSkillSampleAndReturn(
                event.skill,
                totalXp,
                totalXp?.plus(UNKNOWN_TOTAL_FRACTION),
                currentPetUuid,
                singleSkillGainRead(event.gained, totalXp, previousTotalXp, currentPetUuid),
            )
        }
    }

    private fun readIncreasingActionbarSkillGains(
        event: SkillExpGainEvent,
        totalGain: Double,
        totalXp: Double,
        previousTotalXp: Double,
        previous: SkillProgressSample?,
        currentPetUuid: UUID?,
    ): List<SkillGainRead> {
        val previousPetUuid = previous?.petUuid ?: currentPetUuid
        val swappedPet = previousPetUuid != null && previousPetUuid != currentPetUuid
        val pendingSpawn = pendingSpawnAutopetSwap
        val spawnAutopetSwap = pendingSpawn?.takeIf {
            event.skill == SkillType.COMBAT &&
                swappedPet &&
                it.petUuid == currentPetUuid &&
                it.matchesPreviousPet(previousPetUuid) &&
                it.createdAt.passedSince() <= SPAWN_AUTOPET_MAX_AGE
        }
        if (pendingSpawn != null && spawnAutopetSwap == null) {
            if (pendingSpawn.createdAt.passedSince() > SPAWN_AUTOPET_MAX_AGE) pendingSpawnAutopetSwap = null
        }
        return spawnAutopetSwap?.let {
            pendingSpawnAutopetSwap = null
            singleSkillGainRead(totalGain, totalXp, previousTotalXp, previousPetUuid)
        } ?: singleSkillGainRead(
            totalGain,
            totalXp,
            previousTotalXp,
            currentPetUuid,
        )
    }

    private fun singleSkillGainRead(
        gain: Double,
        totalXp: Double?,
        previousTotalXp: Double?,
        petUuid: UUID?,
    ) = listOf(SkillGainRead(gain, totalXp, previousTotalXp, petUuid))

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

    private fun SpawnAutopetContext.matchesPreviousPet(samplePreviousPetUuid: UUID?) =
        previousPetUuid == null || previousPetUuid == samplePreviousPetUuid

    private fun PetData.resyncSkillFractionFromExactRead(appliedDelta: Double?): Boolean {
        val petUuid = uuid ?: return false

        val skill = recentSkillEstimates.asSequence()
            .filter { it.value.petUuid == petUuid && it.value.createdAt.passedSince() <= 10.minutes }
            .maxByOrNull { it.value.createdAt }
            ?.key ?: return false
        val sample = skillSamples[skill] ?: return false
        val totalXp = sample.totalXp ?: return false
        val precise = sample.preciseTotalXp ?: return false
        val multiplier = estimatePetXp(this, skill, 1.0)?.takeIf { it > 0.0 } ?: return false
        val skillDelta = appliedDelta?.takeIf { it != 0.0 }?.let { it / multiplier } ?: return false
        if (abs(skillDelta) >= 1.0) return false

        val resynced = (precise + skillDelta).coerceIn(totalXp, totalXp + MAX_TOTAL_FRACTION)
        if (resynced == precise) return false
        val updatedSample = SkillProgressSample(totalXp, resynced, sample.petUuid)
        skillSamples[skill] = updatedSample
        return true
    }

    private fun estimatePetXp(petData: PetData, skill: SkillType, skillXp: Double): Double? {
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

        return skillXp * totalMultiplier
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
        beastmasterMultiplierCache ?: readBeastmasterMultiplier().also { beastmasterMultiplierCache = it }

    private fun readBeastmasterMultiplier(): Double =
        (InventoryUtils.getItemsInOwnInventory() + EquipmentApi.getAll())
            .asSequence()
            .mapNotNull { it.readBeastmasterMultiplierOrNull() }
            .maxOrNull()
            ?: ProfileStorageData.petProfiles?.beastmasterPetXpMultiplier
            ?: 1.0

    private fun clearBeastmasterMultiplierCache() {
        beastmasterMultiplierCache = null
    }

    private fun SafeItemStack.readBeastmasterMultiplierOrNull(): Double? {
        val internalName = getInternalNameOrNull()?.asString() ?: return null
        if (!internalName.startsWith(BEASTMASTER_CREST_PREFIX)) return null
        val amount = beastmasterPetXpPattern.firstMatcher(getLore().map { it.removeColor() }) {
            group("amount").formatDouble()
        } ?: return null
        return 1.0 + amount / 100.0
    }

    private fun updateExpSharePets(skill: SkillType, sourcePetXp: Double, currentPetUuid: UUID?): Boolean {
        val baseRate = expShareBaseRate()
        val whyNotMoreRate = whyNotMoreExpShareRate()
        var updated = false
        PetStorageExpShare.getActivePets().forEach { petData ->
            val currentExp = petData.exp ?: return@forEach
            if (petData.uuid == currentPetUuid) return@forEach

            val itemRate = if (petData.heldItemInternalName == EXP_SHARE) 0.15 else 0.0
            // Server quirk: Why Not More is extra share rate, not a final multiplier.
            val rate = baseRate + whyNotMoreRate + itemRate
            if (rate <= 0.0) return@forEach

            val petType = PetUtils.getPetType(petData.fauxInternalName) ?: return@forEach
            // EXP Share uses the equipped pet XP, then only the shared pet's base skill type.
            val sharedPetBaseMultiplier = skillBaseMultiplier(petType, skill) ?: return@forEach
            val effectiveRate = rate * sharedPetBaseMultiplier
            val gain = sourcePetXp * effectiveRate
            petData.exp = currentExp + gain
            petData.uuid?.let { recentEstimatePetUuids[it] = SimpleTimeMark.now() }
            updated = true
        }
        return updated
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

    private data class SkillGainRead(
        val gain: Double,
        val totalXp: Double?,
        val previousTotalXp: Double?,
        val petUuid: UUID?,
    )

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

    private data class SpawnAutopetContext(
        val petUuid: UUID?,
        val previousPetUuid: UUID?,
        val createdAt: SimpleTimeMark = SimpleTimeMark.now(),
    )

}
