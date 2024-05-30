package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object PetAPI {
    private val patternGroup = RepoPattern.group("misc.pet")
    private val petMenuPattern by patternGroup.pattern(
        "menu.title",
        "Pets(?: \\(\\d+/\\d+\\) )?"
    )
    private val petItemNamePattern by patternGroup.pattern(
        "item.name",
        "(?:§.)*\\[Lvl (?<level>\\d+)] (?<name>.*)"
    )
    private val neuRepoPetItemNamePattern by patternGroup.pattern(
        "item.name.neu.format",
        "(§f§f)?§7\\[Lvl 1➡(100|200)] (?<name>.*)"
    )

    private val ignoredPetStrings = listOf(
        "Archer",
        "Berserk",
        "Mage",
        "Tank",
        "Healer",
        "➡",
    )

    fun isPetMenu(inventoryTitle: String): Boolean = petMenuPattern.matches(inventoryTitle)

    // Contains color code + name and for older SkyHanni users maybe also the pet level
    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet?.takeIf { it.isNotEmpty() }
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.contains(petName) ?: false

    fun getCleanName(nameWithLevel: String): String? {
        petItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }
        neuRepoPetItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }

        return null
    }

    fun getPetLevel(nameWithLevel: String): Int? = petItemNamePattern.matchMatcher(nameWithLevel) {
        group("level").toInt()
    }

    fun hasPetName(name: String): Boolean =
        petItemNamePattern.matches(name) && !ignoredPetStrings.any { name.contains(it) }
}
