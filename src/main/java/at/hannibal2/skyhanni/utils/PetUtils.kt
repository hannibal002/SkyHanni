package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.AnimatedSkinJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuAnimatedSkullsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetsJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase

@SkyHanniModule
object PetUtils {
    private var petSkins = mutableMapOf<String, MutableList<NeuItemJson>>()
    private var basePetLeveling: List<Int> = listOf()
    private var customPetLeveling: Map<String, NeuPetData>? = null
    private var animatedPetSkins: Map<String, AnimatedSkinJson> = mapOf()
    private var displayNameMap: Map<String, String> = mapOf()

    var petInternalNames: Set<NeuInternalName> = setOf()
        private set
    var petItemResolution: Map<String, NeuInternalName> = mapOf()
        private set
    var petSkinVariants: Map<NeuInternalName, List<String>> = mapOf()
        private set
    var petSkinNbtNames: List<String> = listOf()
        private set

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: PET_SKIN_ENDERMAN
     * REGEX-TEST: PET_SKIN_PARROT_TOUCAN
     * REGEX-TEST: PET_SKIN_PHEONIX_FLAMINGO
     * REGEX-TEST: PET_SKIN_PHOENIX_ICE
     * REGEX-TEST: PET_SKIN_PIGMAN_LUNAR_PIG
     * REGEX-TEST: PET_SKIN_RABBIT
     * REGEX-TEST: PET_SKIN_RABBIT_AQUAMARINE
     * REGEX-TEST: PET_SKIN_RABBIT_LUNAR
     * REGEX-TEST: PET_SKIN_RABBIT_LUNAR_BABY
     * REGEX-TEST: PET_SKIN_RABBIT_PLUSHIE
     * REGEX-TEST: PET_SKIN_RABBIT_ROSE
     */
    private val petSkinNamePattern by CurrentPetApi.patternGroup.pattern(
        "neu.pet.skin",
        "PET_SKIN_(?<pet>[A-Z])_?(?<skin>[A-Z_]+)?",
    )

    /**
     * REGEX-TEST: §7§eRight-click to add this pet to
     * REGEX-TEST: §7§eRight-click to add this pet to your
     */
    private val neuPetLorePattern by CurrentPetApi.patternGroup.pattern(
        "neu.pet.lore",
        "§7§eRight-click to add this pet to(?: your)?",
    )
    // </editor-fold>

    // <editor-fold desc="Helpers">
    private fun splitInternalName(internalName: NeuInternalName): Pair<String, LorenzRarity>? {
        val parts = internalName.asString().split(";")
        if (parts.size < 2) return null
        val name = parts[0].takeIf { it.isNotBlank() } ?: return null
        val rarityId = parts[1].toIntOrNull() ?: return null
        val rarity = LorenzRarity.getById(rarityId) ?: return null
        return name to rarity
    }

    private fun NeuInternalName.getProperName() = splitInternalName(this)?.first
    fun getPetProperName(petInternalName: NeuInternalName): String? = splitInternalName(petInternalName)?.first
    fun getPetRarity(petInternalName: NeuInternalName): LorenzRarity? = splitInternalName(petInternalName)?.second

    private fun getFullLevelingTree(petInternalName: NeuInternalName): List<Int> =
        basePetLeveling + customPetLeveling?.get(petInternalName.getProperName())?.petLevels.orEmpty()

    /**
     * @param refPetInternalName The pet to compare against
     * @param opPetInternalName The pet that is being compared to the reference.
     *
     * @return An int (or null) representing the relationship between the two pets.
     *  null in the case that the pets do not share a family
     *      OR if either internal name passed is not a pet or cannot be parsed
     *  1 if opPet is a higher rarity than refPet
     *  0 if opPet is the same rarity as refPet
     *  -1 if opPet is a lesser rarity than refPet
     */
    fun comparePets(refPetInternalName: NeuInternalName, opPetInternalName: NeuInternalName): Int? {
        val (refProperName, refRarity) = splitInternalName(refPetInternalName) ?: return null
        val (opProperName, opRarity) = splitInternalName(opPetInternalName) ?: return null
        if (refProperName != opProperName) return null

        // Comparable.compareTo returns <0, 0 or >0, compareTo(0) maps that to exactly -1,0 or +1
        return opRarity.compareTo(refRarity).compareTo(0)
    }

    fun getCleanPetName(petInternalName: NeuInternalName, colored: Boolean = true): String {
        val (properPetName, rarity) = splitInternalName(petInternalName) ?: return ""
        return buildString {
            if (colored) { append(rarity.chatColorCode) }
            displayNameMap.getOrElse(properPetName) {
                properPetName.split('_').joinToString(" ") {
                    it.firstLetterUppercase()
                }
            }.let { append(it) }
        }
    }

    fun getPetSkinOrNull(petInternalName: NeuInternalName, skinColorTag: String): NeuItemJson? =
        petSkins[petInternalName.getProperName()]?.singleOrNull {
            it.displayName.startsWith(skinColorTag)
        }

    fun getMaxLevel(petInternalName: NeuInternalName): Int =
        customPetLeveling?.get(petInternalName.getProperName())?.maxLevel ?: 100

    fun petWithRarityToInternalName(petName: String, rarity: LorenzRarity) =
        "${petName.uppercase().replace(" ", "_")};${rarity.id}".toInternalName()

    fun levelToXp(level: Int, petInternalName: NeuInternalName): Double? {
        val rarityOffset = getRarityOffset(petInternalName) ?: return null
        if (level < 0 || level > getMaxLevel(petInternalName)) return null
        return getFullLevelingTree(petInternalName)
            .slice(0 + rarityOffset..<level + rarityOffset - 1)
            .sumOf { it.toDouble() }
    }

    fun xpToLevel(petInfo: SkyBlockItemModifierUtils.PetInfo): Int = PetData(petInfo).level

    /**
     * DO NOT USE THIS METHOD UNLESS YOU ARE SURE YOU HAVE A FAUX INTERNAL NAME!
     * Converts total XP to a pet level.
     * @param totalXp The total XP of the pet.
     * @param petInternalName The internal name of the pet, reflecting tier boost properly.
     * @param coerceToMax Whether to floor the calculated level to the maximum level of the pet. (Default: true)
     */
    fun xpToLevel(totalXp: Double, petInternalName: NeuInternalName, coerceToMax: Boolean = true): Int {
        var xp = totalXp.takeIf { it > 0 } ?: return 1
        val rarityOffset = getRarityOffset(petInternalName) ?: return 1
        val xpList = getFullLevelingTree(petInternalName)

        var level = 1
        val maxLevel = getMaxLevel(petInternalName)
        for (i in 0 + rarityOffset until xpList.size) {
            val xpReq = xpList[i]
            if (xp >= xpReq) {
                xp -= xpReq
                level++
            } else break
        }

        return if (coerceToMax) level.coerceAtMost(maxLevel) else level
    }

    private fun getRarityOffset(petInternalName: NeuInternalName): Int? {
        val petsData = customPetLeveling ?: return null
        val (properPetName, rarity) = splitInternalName(petInternalName) ?: return null
        return petsData[properPetName]?.rarityOffset?.get(rarity) ?: when (rarity) {
            LorenzRarity.COMMON -> 0
            LorenzRarity.UNCOMMON -> 6
            LorenzRarity.RARE -> 11
            LorenzRarity.EPIC -> 16
            LorenzRarity.LEGENDARY -> 20
            LorenzRarity.MYTHIC -> 20
            else -> ErrorManager.skyHanniError("Unknown pet rarity $rarity")
        }
    }

    private val nextTierCache: MutableMap<NeuInternalName, Boolean> = mutableMapOf()
    fun NeuInternalName.hasValidHigherTier() = nextTierCache.getOrPut(this) {
        if (!this.isPet) return@getOrPut false
        val (properPetName, rarity) = splitInternalName(this)
            ?: return@getOrPut false
        val rarityAbove = rarity.oneAbove() ?: return@getOrPut false
        val tierAboveInternalName = petWithRarityToInternalName(properPetName, rarityAbove)
        return@getOrPut tierAboveInternalName.isPet
    }
    // </editor-fold>

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val petData = event.getConstant<NeuPetsJson>("pets")
        basePetLeveling = petData.basePetLeveling
        customPetLeveling = petData.customPetLeveling
        petItemResolution = petData.petItemResolution
        displayNameMap = petData.displayNameMap

        val skinData = event.getConstant<NeuAnimatedSkullsJson>("animatedskulls")
        animatedPetSkins = skinData.skins
        petSkinVariants = skinData.petSkinVariants
        petSkinNbtNames = skinData.petSkinNbtNames

        val rawPetInternalNames = mutableSetOf<NeuInternalName>()
        val rawPetSkins = mutableMapOf<String, MutableList<NeuItemJson>>()
        NeuItems.allNeuRepoItems().forEach { (rawInternalName, jsonObject) ->
            val petItemData = ConfigManager.gson.fromJson(jsonObject, NeuItemJson::class.java)
            petSkinNamePattern.matchMatcher(rawInternalName) {
                val properPetName = group("pet") ?: return@matchMatcher
                rawPetSkins.getOrPut(properPetName) { mutableListOf() }.add(petItemData)
            }
            neuPetLorePattern.firstMatcher(petItemData.lore) {
                rawPetInternalNames.add(rawInternalName.toInternalName())
            }
        }
        petInternalNames = rawPetInternalNames
        petSkins = rawPetSkins
        nextTierCache.clear()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shtesthashigher") {
            description = "Test has higher tier"
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                ChatUtils.chat("${it[0].toInternalName().hasValidHigherTier()}")
            }
        }
    }
}
