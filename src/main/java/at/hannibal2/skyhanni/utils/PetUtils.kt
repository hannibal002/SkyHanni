package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetSkinJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetsJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import com.google.gson.Gson

@SkyHanniModule
object PetUtils {
    // Map of Pet Name to a Map of Skin Name to NeuPetSkinJson
    val petSkins = mutableMapOf<String, MutableList<NeuPetSkinJson>>()

    private var baseXpLevelReqs: List<Int> = listOf()
    private var customXpLevelReqs: Map<String, NeuPetData>? = null
    var petItemResolution: Map<String, NeuInternalName> = mapOf()
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
        "neu.pet",
        "PET_SKIN_(?<pet>[A-Z])_?(?<skin>[A-Z_]+)?"
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

    fun internalNameToPetWithRarity(internalName: NeuInternalName): Pair<String, LorenzRarity>? {
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
        if (level < 0 || level >= getMaxLevel(petInternalName)) return null
        return getFullLevelingTree(petInternalName)
            .slice(0 + rarityOffset..<level + rarityOffset - 1)
            .sumOf { it.toDouble() }
    }

    fun xpToLevel(totalXp: Double, petInternalName: NeuInternalName): Int {
        var xp = totalXp.takeIf { it > 0 } ?: return 0
        val rarityOffset = getRarityOffset(petInternalName) ?: return 0
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
        return customXpLevelReqs?.get(properPetName)?.maxLevel ?: 100
    }

    private fun getFullLevelingTree(petInternalName: NeuInternalName): List<Int> {
        val properPetName = petInternalName.asString().split(";").first()
        return baseXpLevelReqs + customXpLevelReqs?.get(properPetName)?.petLevels.orEmpty()
    }

    private fun getRarityOffset(petInternalName: NeuInternalName): Int? {
        val petsData = customXpLevelReqs ?: run {
            ErrorManager.skyHanniError("NEUPetsData is null")
        }
        val (properPetName, rarity) = internalNameToPetWithRarity(petInternalName) ?: return null
        return if (properPetName in petsData.keys) {
            val petData = petsData[properPetName]
            petData?.rarityOffset?.get(rarity)
        } else when (rarity) {
            LorenzRarity.COMMON -> 0
            LorenzRarity.UNCOMMON -> 6
            LorenzRarity.RARE -> 11
            LorenzRarity.EPIC -> 16
            LorenzRarity.LEGENDARY -> 20
            LorenzRarity.MYTHIC -> 20
            else -> ErrorManager.skyHanniError("Unknown pet rarity $rarity")
        }
    }
    // </editor-fold>

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.getConstant<NeuPetsJson>("pets")
        baseXpLevelReqs = data.petLevels
        customXpLevelReqs = data.customPetLeveling
        petItemResolution = data.petItemDisplayNameToInternalName

        NeuItems.allNeuRepoItems().forEach { (rawInternalName, jsonObject) ->
            petSkinNamePattern.matchMatcher(rawInternalName) {
                val petName = group("pet") ?: return@matchMatcher
                val petItemData = Gson().fromJson(jsonObject, NeuPetSkinJson::class.java)
                petSkins.getOrPut(petName) { mutableListOf() }.add(petItemData)
            }
        }
    }
}
