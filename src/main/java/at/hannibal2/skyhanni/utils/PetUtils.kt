package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.CurrentPetApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.PetData.Companion.internalNameToPetName
import at.hannibal2.skyhanni.data.PetData.Companion.petNameToInternalName
import at.hannibal2.skyhanni.data.jsonobjects.repo.NEUPetData
import at.hannibal2.skyhanni.data.jsonobjects.repo.NEUPetsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetSkinJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.Gson
import net.minecraft.item.ItemStack

@SkyHanniModule
object PetUtils {
    private val patternGroup = RepoPattern.group("misc.pet")
    private const val FORGE_BACK_SLOT = 48
    // Map of Pet Name to a Map of InternalName to NeuPetSkinJson
    private val petSkins = mutableMapOf<String, MutableMap<String, NeuPetSkinJson>>()

    private var baseXpLevelReqs: List<Int> = listOf()
    private var baseRarityOffsets: Map<LorenzRarity, Int> = mapOf()
    private var customXpLevelReqs: Map<NeuInternalName, NEUPetData>? = null
    private fun getXpList(petInternalName: NeuInternalName): List<Int> =
        baseXpLevelReqs + customXpLevelReqs?.get(petInternalName)?.petLevels.orEmpty()

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
     * REGEX-TEST: Pets (1/3)
     * REGEX-TEST: Pets
     * REGEX-TEST: Pets (1/4)
     * REGEX-TEST: Pets (1/2)
     */
    private val petMenuPattern by patternGroup.pattern(
        "menu.title",
        "Pets(?: \\(\\d+/\\d+\\) )?",
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
     * REGEX-TEST: §7To Select Process (Slot #2)
     * REGEX-TEST: §7To Select Process (Slot #4)
     * REGEX-TEST: §7To Select Process (Slot #7)
     * REGEX-TEST: §7To Select Process
     */
    private val forgeBackMenuPattern by CurrentPetApi.patternGroup.pattern(
        "menu.forge.goback",
        "§7To Select Process(?: \\(Slot #\\d\\))?",
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
    fun isPetMenu(inventoryTitle: String, inventoryItems: Map<Int, ItemStack>): Boolean {
        if (!petMenuPattern.matches(inventoryTitle)) return false

        // Otherwise make sure they're not in the Forge menu looking at pets
        return inventoryItems[FORGE_BACK_SLOT]?.getLore().orEmpty().none {
            forgeBackMenuPattern.matches(it)
        }
    }

    fun getCleanName(nameWithLevel: String): String? {
        petItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }
        neuRepoPetItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }

        return null
    }

    fun rarityByColorGroup(color: String): LorenzRarity = LorenzRarity.getByColorCode(color[0])
        ?: ErrorManager.skyHanniError(
            "Unknown rarity",
            Pair("rarity", color),
        )

    private fun levelToXPCommand(input: Array<String>) {
        if (input.size < 3) {
            ChatUtils.userError("Usage: /shcalcpetxp <level> <rarity> <pet>")
            return
        }

        val level = input[0].toIntOrNull()
        if (level == null) {
            ChatUtils.userError("Invalid level '${input[0]}'.")
            return
        }
        val rarity = LorenzRarity.getByName(input[1])
        if (rarity == null) {
            ChatUtils.userError("Invalid rarity '${input[1]}'.")
            return
        }

        val petName = input.slice(2..<input.size).joinToString(" ")
        val petInternalName = petNameToInternalName(petName, rarity)

        val xp: Double = levelToXp(level, petInternalName) ?: run {
            ChatUtils.userError("Invalid level or rarity.")
            return
        }
        ChatUtils.chat(xp.addSeparators())
        return
    }

    fun levelToXp(level: Int, petInternalName: NeuInternalName): Double? {
        val rarityOffset = getRarityOffset(petInternalName)
        if (!isValidLevel(level, petInternalName)) return null
        val sliceIndices = rarityOffset until level + rarityOffset - 1

        return getXpList(petInternalName).slice(sliceIndices).sumOf { it.toDouble() }
    }

    fun xpToLevel(totalXp: Double, petInternalName: NeuInternalName): Int? {
        val rarityOffset = getRarityOffset(petInternalName)

        val xpList = getXpList(petInternalName)
        var xp = totalXp.takeIf { it > 0 } ?: return null
        var level = 0
        for (i in 0 + rarityOffset until xpList.size) {
            val xpReq = xpList[i]
            if (xp >= xpReq) {
                xp -= xpReq
                level++
            } else break
        }

        return level
    }

    fun isValidLevel(level: Int, petInternalName: NeuInternalName): Boolean =
        level <= (customXpLevelReqs?.get(petInternalName)?.maxLevel ?: 100)

    private fun getRarityOffset(petInternalName: NeuInternalName): Int {
        val rarityOffset = customXpLevelReqs?.get(petInternalName)?.rarityOffset ?: baseRarityOffsets
        val (_, rarity) = internalNameToPetName(petInternalName) ?: return 0
        return rarityOffset[rarity] ?: 0
    }
    // </editor-fold>

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.getConstant<NEUPetsJson>("pets")
        baseXpLevelReqs = data.petLevels
        customXpLevelReqs = data.customPetLeveling.mapKeys { it.key.toInternalName() }
        baseRarityOffsets = data.petRarityOffset

        NeuItems.allNeuRepoItems().forEach { (rawInternalName, jsonObject) ->
            petSkinNamePattern.matchMatcher(rawInternalName) {
                val petName = group("pet") ?: return@matchMatcher
                // Use GSON to reflect the JSON into a NeuPetSkinJson object
                val petItemData = Gson().fromJson(jsonObject, NeuPetSkinJson::class.java)

                petSkins.getOrPut(petName) { mutableMapOf() }[rawInternalName] = petItemData
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shpetxp") {
            description = "Calculates the pet xp from a given level and rarity."
            category = CommandCategory.DEVELOPER_TEST
            callback { levelToXPCommand(it) }
        }
    }

    fun PetData.getSkinOrNull(): NeuPetSkinJson? {
        if (skinSymbolColor == null && skinInternalName == null) return null

        val cleanPetName = cleanName ?: return null
        val possiblePetSkins = petSkins[cleanPetName] ?: return null
        if (possiblePetSkins.size == 1) return possiblePetSkins.values.first()

        skinInternalName?.let { return possiblePetSkins[it.asString()] }

        val possibleSymbolSkins = possiblePetSkins.filter {
            val cosmeticRarity = it.value.rarity ?: return@filter false
            cosmeticRarity.color == skinSymbolColor
        }

        return possibleSymbolSkins.values.firstOrNull()
    }
}
