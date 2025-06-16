package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
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

@SkyHanniModule
object PetUtils {
    // Map of Proper Pet Name to the skins that pet can have
    val petSkins = mutableMapOf<String, MutableList<NeuItemJson>>()

    private var basePetLeveling: List<Int> = listOf()
    private var customPetLeveling: Map<String, NeuPetData>? = null

    var petInternalNames: Set<NeuInternalName> = setOf()
        private set
    var petItemResolution: Map<String, NeuInternalName> = mapOf()
        private set
    var animatedPetSkins: Map<String, AnimatedSkinJson> = mapOf()
        private set
    var petSkinVariants: Map<NeuInternalName, List<String>> = mapOf()
        private set
    var petSkinNbtNames: List<String> = listOf()
        private set
    var displayNameMap: Map<String, String> = mapOf()
        private set

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §e⭐ §7[Lvl 200] §6Golden Dragon§d ✦
     * REGEX-TEST: ⭐ [Lvl 100] Black Cat ✦
     */
    val petItemNamePattern by CurrentPetApi.patternGroup.pattern(
        "item.name",
        "(?<favorite>(?:§.)*⭐ )?(?:§.)*\\[Lvl (?<level>\\d+)] (?<name>.*)",
    )

    /**
     * REGEX-TEST: §7[Lvl 1➡200] §6Golden Dragon
     * REGEX-TEST: §7[Lvl {LVL}] §6Golden Dragon
     */
    private val neuRepoPetItemNamePattern by CurrentPetApi.patternGroup.pattern(
        "item.name.neu.format",
        "(?:§f§f)?§7\\[Lvl (?:1➡(?:100|200)|\\{LVL})] (?<name>.*)",
    )

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
    fun getCleanName(nameWithLevel: String): String? {
        petItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }
        neuRepoPetItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }

        return null
    }

    fun internalNameToProperPetWithRarity(internalName: NeuInternalName): Pair<String, LorenzRarity>? {
        val parts = internalName.asString().split(";")
        if (parts.size < 2) return null
        val name = parts[0].takeIf { it.isNotBlank() } ?: return null
        val rarityId = parts[1].toIntOrNull() ?: return null
        val rarity = LorenzRarity.getById(rarityId) ?: return null
        return name to rarity
    }

    fun petWithRarityToInternalName(petName: String, rarity: LorenzRarity) =
        "${petName.uppercase().replace(" ", "_")};${rarity.id}".toInternalName()

    fun levelToXp(level: Int, petInternalName: NeuInternalName): Double? {
        val rarityOffset = getRarityOffset(petInternalName) ?: return null
        if (level < 0 || level > getMaxLevel(petInternalName)) return null
        return getFullLevelingTree(petInternalName)
            .slice(0 + rarityOffset..<level + rarityOffset - 1)
            .sumOf { it.toDouble() }
    }

    fun xpToLevel(totalXp: Double, petInternalName: NeuInternalName): Int {
        var xp = totalXp.takeIf { it > 0 } ?: return 1
        val rarityOffset = getRarityOffset(petInternalName) ?: return 1
        val xpList = getFullLevelingTree(petInternalName)

        var level = 1
        for (i in 0 + rarityOffset until xpList.size) {
            val xpReq = xpList[i]
            if (xp >= xpReq) {
                xp -= xpReq
                level++
            } else break
        }

        return level
    }

    fun getMaxLevel(petInternalName: NeuInternalName): Int {
        val properPetName = petInternalName.asString().split(";").first()
        return customPetLeveling?.get(properPetName)?.maxLevel ?: 100
    }

    private fun getFullLevelingTree(petInternalName: NeuInternalName): List<Int> {
        val properPetName = petInternalName.asString().split(";").first()
        return basePetLeveling + customPetLeveling?.get(properPetName)?.petLevels.orEmpty()
    }

    private fun getRarityOffset(petInternalName: NeuInternalName): Int? {
        val petsData = customPetLeveling ?: return null
        val (properPetName, rarity) = internalNameToProperPetWithRarity(petInternalName) ?: return null
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
        val (properPetName, rarity) = internalNameToProperPetWithRarity(this)
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
        NeuItems.allNeuRepoItems().forEach { (rawInternalName, jsonObject) ->
            val petItemData = ConfigManager.gson.fromJson(jsonObject, NeuItemJson::class.java)
            petSkinNamePattern.matchMatcher(rawInternalName) {
                val properPetName = group("pet") ?: return@matchMatcher
                petSkins.getOrPut(properPetName) { mutableListOf() }.add(petItemData)
            }
            neuPetLorePattern.firstMatcher(petItemData.lore) {
                rawPetInternalNames.add(rawInternalName.toInternalName())
            }
        }
        petInternalNames = rawPetInternalNames
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
