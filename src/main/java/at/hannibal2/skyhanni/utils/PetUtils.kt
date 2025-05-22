package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetSkinJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetsJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.Gson
import net.minecraft.item.ItemStack

@SkyHanniModule
object PetUtils {
    private val patternGroup = RepoPattern.group("misc.pet")
    private const val FORGE_BACK_SLOT = 48
    // Map of Pet Name to a Map of Skin Name to NeuPetSkinJson
    val petSkins = mutableMapOf<String, MutableList<NeuPetSkinJson>>()

    private var baseXpLevelReqs: List<Int> = listOf()
    private var customXpLevelReqs: Map<String, NeuPetData>? = null

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

    private fun xpToLevelCommand(input: Array<String>) {
        if (input.size < 3) {
            ChatUtils.userError("Usage: /shpetlevel <xp> <rarity> <pet>")
            return
        }

        val xp = input[0].toDoubleOrNull()
        if (xp == null) {
            ChatUtils.userError("Invalid xp '${input[0]}'.")
            return
        }
        val rarity = LorenzRarity.getByName(input[1])
        if (rarity == null) {
            ChatUtils.userError("Invalid rarity '${input[1]}'.")
            return
        }

        val petName = input.slice(2..<input.size).joinToString(" ")
        val level: Int = xpToLevel(xp, rarity, petName)
        ChatUtils.chat(level.addSeparators())
    }

    private fun levelToXPCommand(input: Array<String>) {
        if (input.size < 3) {
            ChatUtils.userError("Usage: /shpetxp <level> <rarity> <pet>")
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
        val xp: Double = levelToXp(level, rarity, petName) ?: run {
            ChatUtils.userError("Invalid level or rarity.")
            return
        }
        ChatUtils.chat(xp.addSeparators())
    }

    fun levelToXp(level: Int, rarity: LorenzRarity, petName: String): Double? {
        val newPetName = petName.toInternalName().toString()

        val rarityOffset = getRarityOffset(rarity, newPetName) ?: return null
        if (!isValidLevel(level, newPetName)) return null

        val xpList = baseXpLevelReqs + getCustomLeveling(newPetName)

        return xpList.slice(0 + rarityOffset..<level + rarityOffset - 1).sum().toDouble()
    }

    fun petNameToInternalName(name: String): NeuInternalName? {
        val rarity = LorenzRarity.getByColorCode(
            name.substring(1, 2)[0]
        ) ?: return null
        return petNameAndRarityToInternalName(name.substring(2), rarity)
    }

    fun petNameAndRarityToInternalName(name: String, rarity: LorenzRarity): NeuInternalName =
        "${name.removeColor()};${rarity.id}".toInternalName()

    fun internalNameToPetWithRarity(internalName: NeuInternalName): Pair<String, LorenzRarity>? {
        val (name, rarityStr) = internalName.asString().split(";")
        val rarity = LorenzRarity.getById(rarityStr.toInt()) ?: return null
        return Pair(name, rarity)
    }

    fun xpToLevel(totalXp: Double, petInternalName: NeuInternalName): Int {
        val (petName, rarity) = internalNameToPetWithRarity(petInternalName) ?: return 0
        return xpToLevel(totalXp, rarity, petName)
    }

    private fun xpToLevel(totalXp: Double, rarity: LorenzRarity, petName: String): Int {
        var xp = totalXp.takeIf { it > 0 } ?: return 0
        val newPetName = petName.toInternalName().toString()
        val rarityOffset = getRarityOffset(rarity, newPetName) ?: return 0
        val xpList = baseXpLevelReqs + getCustomLeveling(newPetName)

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

    fun getMaxLevel(petName: String): Int {
        return customXpLevelReqs?.get(petName)?.maxLevel ?: 100
    }

    private fun isValidLevel(level: Int, petName: String): Boolean {
        val petsData = customXpLevelReqs ?: run {
            ErrorManager.skyHanniError("NEUPetsData is null")
        }

        val maxLevel = petsData[petName]?.maxLevel ?: return false
        return maxLevel >= level
    }

    private fun getCustomLeveling(petName: String): List<Int> {
        return customXpLevelReqs?.get(petName)?.petLevels.orEmpty()
    }

    private fun getRarityOffset(rarity: LorenzRarity, petName: String): Int? {
        val petsData = customXpLevelReqs ?: run {
            ErrorManager.skyHanniError("NEUPetsData is null")
        }

        return if (petName in petsData.keys) {
            val petData = petsData[petName]
            petData?.rarityOffset?.get(rarity)
        } else {
            when (rarity) {
                LorenzRarity.COMMON -> 0
                LorenzRarity.UNCOMMON -> 6
                LorenzRarity.RARE -> 11
                LorenzRarity.EPIC -> 16
                LorenzRarity.LEGENDARY -> 20
                LorenzRarity.MYTHIC -> 20
                else -> {
                    ChatUtils.userError("Invalid Rarity \"${rarity.name}\"")
                    null
                }
            }
        }
    }
    // </editor-fold>

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.getConstant<NeuPetsJson>("pets")
        baseXpLevelReqs = data.petLevels
        customXpLevelReqs = data.customPetLeveling

        NeuItems.allNeuRepoItems().forEach { (rawInternalName, jsonObject) ->
            petSkinNamePattern.matchMatcher(rawInternalName) {
                val petName = group("pet") ?: return@matchMatcher

                // Use GSON to reflect the JSON into a NeuPetSkinJson object
                val petItemData = Gson().fromJson(jsonObject, NeuPetSkinJson::class.java)

                petSkins.getOrPut(petName) { mutableListOf() }.add(petItemData)
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

        event.register("shpetlevel") {
            description = "Calculates the pet level from a given xp and rarity."
            category = CommandCategory.DEVELOPER_TEST
            callback { xpToLevelCommand(it) }
        }
    }
}
