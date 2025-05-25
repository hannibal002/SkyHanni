package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.AnimatedSkinJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuAnimatedSkullsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetData
//#if TODO
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetSkinJson
//#endif
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetsJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher

@SkyHanniModule
object PetUtils {
    // Map of Pet Name to a Map of Skin Name to NeuPetSkinJson
    val petSkins = mutableMapOf<String, MutableList<NeuPetSkinJson>>()

    private var baseXpLevelReqs: List<Int> = listOf()
    private var customXpLevelReqs: Map<String, NeuPetData>? = null
    var petItemInternalNames: Set<NeuInternalName> = emptySet()
        private set
    var petItemResolution: Map<String, NeuInternalName> = mapOf()
        private set
    var animatedPetSkins: Map<String, AnimatedSkinJson> = mapOf()
        private set
    private val petSkinVariantIndexMap: MutableMap<NeuInternalName, MutableMap<Int, String>> = mutableMapOf()

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
        if (level < 0 || level > getMaxLevel(petInternalName)) return null
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
        val petsData = customXpLevelReqs ?: return null
        val (properPetName, rarity) = internalNameToPetWithRarity(petInternalName) ?: return null
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

    fun getSkinVariantIdentifier(skinInternalName: NeuInternalName, variantIndex: Int): String? =
        petSkinVariantIndexMap[skinInternalName]?.get(variantIndex)

    private val nextTierCache: MutableMap<NeuInternalName, Boolean> = mutableMapOf()
    fun NeuInternalName.hasValidHigherTier() = nextTierCache.getOrPut(this) {
        if (!this.isPet) return@getOrPut false
        val (properPetName, rarity) = internalNameToPetWithRarity(this)
            ?: return@getOrPut false
        val rarityAbove = rarity.oneAbove() ?: return@getOrPut false
        val tierAboveInternalName = petWithRarityToInternalName(properPetName, rarityAbove)
        return@getOrPut tierAboveInternalName.isPet
    }
    // </editor-fold>

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val petData = event.getConstant<NeuPetsJson>("pets")
        baseXpLevelReqs = petData.petLevels
        customXpLevelReqs = petData.customPetLeveling
        petItemResolution = petData.petItemDisplayNameToInternalName

        val rawPetSkinInternalNames = mutableSetOf<String>()
        NeuItems.allNeuRepoItems().forEach { (rawInternalName, jsonObject) ->
            petSkinNamePattern.matchMatcher(rawInternalName) {
                val petName = group("pet") ?: return@matchMatcher
                val petItemData = ConfigManager.gson.fromJson(jsonObject, NeuPetSkinJson::class.java)
                rawPetSkinInternalNames.add(rawInternalName)
                petSkins.getOrPut(petName) { mutableListOf() }.add(petItemData)
            }
        }

        val baseResolverCache: MutableMap<NeuInternalName, String> = mutableMapOf()
        fun tryResolveIdentifier(fullSkinIdentifier: String): Pair<NeuInternalName, String>? {
            // Cached lookup
            baseResolverCache.entries.forEach { (skinInternalName, lookupKey) ->
                if (fullSkinIdentifier.startsWith(lookupKey))
                    return Pair(skinInternalName, fullSkinIdentifier.replace(lookupKey, ""))
            }
            val identifierSplits = fullSkinIdentifier.split("_")
            var splitsToJoin = identifierSplits.size + 2
            while (splitsToJoin-- > 0) {
                val filteredSplits = identifierSplits.subList(0, splitsToJoin)
                val joinedSplits = filteredSplits.joinToString("_")
                if (joinedSplits in rawPetSkinInternalNames) {
                    // Skin is the joined splits, variant is the non-used splits
                    val variant = identifierSplits.subList(splitsToJoin, identifierSplits.size).joinToString("_")
                    val skinInternalName = joinedSplits.toInternalName()
                    baseResolverCache[skinInternalName] = "${joinedSplits}_"
                    return Pair(joinedSplits.toInternalName(), variant)
                }
            }
            return null
        }

        val loadedVariants: MutableMap<NeuInternalName, MutableMap<String, AnimatedSkinJson>> = mutableMapOf()
        val skinData = event.getConstant<NeuAnimatedSkullsJson>("animatedskulls")
        animatedPetSkins = skinData.skins
        // Because hypixel stores which variant of the skin you've selected as an index (int/double) in the
        // extraData of the pet info, we have to make sure we map these skins in the correct order
        skinData.skins.filterKeys { it.startsWith("PET_SKIN_") }.forEach { (identifier, skin) ->
            val (skinInternalName, variant) = tryResolveIdentifier(identifier) ?: return@forEach

            val variantMap = loadedVariants.getOrPut(skinInternalName) { mutableMapOf() }
            val variantIndex = variantMap.size
            variantMap[variant] = skin

            val variantIndexMap = petSkinVariantIndexMap.getOrPut(skinInternalName) { mutableMapOf() }
            variantIndexMap[variantIndex] = variant
        }
    }
}
