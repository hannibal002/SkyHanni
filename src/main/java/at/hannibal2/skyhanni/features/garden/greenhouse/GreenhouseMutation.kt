package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.SafeItemStack

internal enum class GreenhouseMutation(
    val internalId: String,
    val displayName: String,
    private val armorStandName: String = displayName,
    val size: Int = 1,
) {
    ALL_IN_ALOE("ALL_IN_ALOE", "All-in Aloe", "allinaloe"),
    ASHWREATH("ASHWREATH", "Ashwreath", "ashwreath"),
    BLASTBERRY("BLASTBERRY", "Blastberry", "blastberry"),
    CHEESEBITE("CHEESEBITE", "Cheesebite", "cheesebite"),
    CHLORONITE("CHLORONITE", "Chloronite", "chloronite"),
    CHOCOBERRY("CHOCOBERRY", "Chocoberry"),
    CHOCONUT("CHOCONUT", "Choconut"),
    CHORUS_FRUIT("CHORUS_FRUIT", "Chorus Fruit", "chorusFruit"),
    CINDERSHADE("CINDERSHADE", "Cindershade"),
    COALROOT("COALROOT", "Coalroot"),
    CREAMBLOOM("CREAMBLOOM", "Creambloom"),
    DEVOURER("DEVOURER", "Devourer", "devourer"),
    DO_NOT_EAT_SHROOM("DO_NOT_EAT_SHROOM", "Do-not-eat-shroom", "donoteatshroom"),
    DUSKBLOOM("DUSKBLOOM", "Duskbloom", "duskbloom"),
    DUSTGRAIN("DUSTGRAIN", "Dustgrain"),
    FLESHTRAP("FLESHTRAP", "Fleshtrap", "fleshtrap"),
    GLASSCORN("GLASSCORN", "Glasscorn", "glasscorn", size = 2),
    GLOOMGOURD("GLOOMGOURD", "Gloomgourd"),
    GODSEED("GODSEED", "Godseed", "godseed", size = 3),
    JERRYFLOWER("JERRYFLOWER", "Jerryflower", "jerryseed"),
    LONELILY("LONELILY", "Lonelily", "Lonelilly"),
    MAGIC_JELLYBEAN("MAGIC_JELLYBEAN", "Magic Jellybean", "magicjellybean"),
    NOCTILUME("NOCTILUME", "Noctilume", "noctilume", size = 2),
    PHANTOMLEAF("PHANTOMLEAF", "Phantomleaf", "phantomleaf"),
    PLANTBOY_ADVANCE("PLANTBOY_ADVANCE", "PlantBoy Advance", "Plantboy", size = 2),
    PUFFERCLOUD("PUFFERCLOUD", "Puffercloud", "puffercloud"),
    SCOURROOT("SCOURROOT", "Scourroot"),
    SHADEVINE("SHADEVINE", "Shadevine", "shadevine"),
    SHELLFRUIT("SHELLFRUIT", "Shellfruit", "shellfruit"),
    SNOOZLING("SNOOZLING", "Snoozling", "snoozlingFlower", size = 3),
    SOGGYBUD("SOGGYBUD", "Soggybud", "soggybud"),
    STARTLEVINE("STARTLEVINE", "Startlevine", "startlevine"),
    STOPLIGHT_PETAL("STOPLIGHT_PETAL", "Stoplight Petal", "stoplightpetal"),
    THORNSHADE("THORNSHADE", "Thornshade", "thornshade"),
    THUNDERLING("THUNDERLING", "Thunderling", "thunderling"),
    TIMESTALK("TIMESTALK", "Timestalk", "timestalk"),
    TURTLELLINI("TURTLELLINI", "Turtlellini", "turtlellini"),
    VEILSHROOM("VEILSHROOM", "Veilshroom"),
    WITHERBLOOM("WITHERBLOOM", "Witherbloom"),
    ZOMBUD("ZOMBUD", "Zombud", "zombud"),
    ;

    private val normalizedNames = setOf(normalizeMutationName(displayName), normalizeMutationName(armorStandName))

    val spawnRequirements: Map<String, Int>
        get() = requirementsByMutation[this].orEmpty()

    val providesYieldBuff: Boolean
        get() = this in yieldBuffMutations

    companion object {
        private val byId = entries.associateBy { it.internalId }
        private val bySkyShardsId = entries.associateBy { it.internalId.lowercase() }
        private val ignoredArmorStandNames = setOf("plantboyroots", "godseedpillar")
        private val mutationByTexture = mutableMapOf<String, GreenhouseMutation>()
        private val mutationBySkullOwner = mutableMapOf<String, GreenhouseMutation>()

        fun fromInternalId(id: String): GreenhouseMutation? = byId[id]

        fun fromSkyShardsId(id: String): GreenhouseMutation? = bySkyShardsId[id.lowercase()]

        fun fromItem(stack: SafeItemStack?): GreenhouseMutation? {
            stack ?: return null
            return fromName(stack.cleanName)
                ?: stack.getSkullTexture()?.let(::fromTexture)
                ?: stack.getSkullOwner()?.let(::fromSkullOwner)
        }

        fun fromTexture(texture: String): GreenhouseMutation? = mutationByTexture[texture]
            ?: entries.firstOrNull { it.repoStack()?.getSkullTexture() == texture }
                ?.also { mutationByTexture[texture] = it }

        fun fromSkullOwner(owner: String): GreenhouseMutation? = mutationBySkullOwner[owner]
            ?: entries.firstOrNull { it.repoStack()?.getSkullOwner() == owner }
                ?.also { mutationBySkullOwner[owner] = it }

        fun fromName(name: String): GreenhouseMutation? {
            val normalizedName = normalizeMutationName(name)
            if (normalizedName in ignoredArmorStandNames) return null
            return entries.firstOrNull { mutation ->
                mutation.normalizedNames.any { candidate -> normalizedName.contains(candidate) }
            }
        }

        fun fromQuery(query: String): GreenhouseMutation? {
            val normalizedQuery = normalizeMutationName(query)
            if (normalizedQuery.isEmpty()) return null
            return entries.singleOrNull { mutation ->
                mutation.normalizedNames.any { candidate ->
                    candidate == normalizedQuery || candidate.startsWith(normalizedQuery)
                }
            }
        }

        private val requirementsByMutation = mapOf(
            ASHWREATH to mapOf("nether_wart" to 2, "fire" to 2),
            CHOCONUT to mapOf("cocoa_beans" to 2),
            DUSTGRAIN to mapOf("wheat" to 2),
            GLOOMGOURD to mapOf("pumpkin" to 1, "melon" to 1),
            SCOURROOT to mapOf("potato" to 1, "carrot" to 1),
            SHADEVINE to mapOf("cactus" to 1, "sugar_cane" to 1),
            VEILSHROOM to mapOf("red_mushroom" to 1, "brown_mushroom" to 1),
            WITHERBLOOM to mapOf("dead_plant" to 4),
            CHOCOBERRY to mapOf("choconut" to 6, "gloomgourd" to 2),
            CINDERSHADE to mapOf("ashwreath" to 4, "witherbloom" to 4),
            COALROOT to mapOf("ashwreath" to 5, "scourroot" to 3),
            CREAMBLOOM to mapOf("choconut" to 8),
            DUSKBLOOM to mapOf("moonflower" to 2, "shadevine" to 2, "sunflower" to 2, "dustgrain" to 2),
            THORNSHADE to mapOf("wild_rose" to 4, "veilshroom" to 4),
            BLASTBERRY to mapOf("chocoberry" to 5, "ashwreath" to 3),
            CHEESEBITE to mapOf("creambloom" to 4, "fermento" to 4),
            CHLORONITE to mapOf("coalroot" to 6, "thornshade" to 2),
            DO_NOT_EAT_SHROOM to mapOf("veilshroom" to 4, "scourroot" to 4),
            FLESHTRAP to mapOf("cindershade" to 4, "lonelily" to 4),
            MAGIC_JELLYBEAN to mapOf("sugar_cane" to 5, "duskbloom" to 3),
            NOCTILUME to mapOf("duskbloom" to 6, "lonelily" to 6),
            SNOOZLING to mapOf(
                "creambloom" to 4,
                "dustgrain" to 3,
                "witherbloom" to 3,
                "duskbloom" to 3,
                "thornshade" to 3,
            ),
            SOGGYBUD to mapOf("melon" to 2, "gloomgourd" to 2),
            CHORUS_FRUIT to mapOf("chloronite" to 5, "magic_jellybean" to 3),
            PLANTBOY_ADVANCE to mapOf("snoozling" to 6, "thunderling" to 6),
            PUFFERCLOUD to mapOf("snoozling" to 2, "do_not_eat_shroom" to 6),
            STARTLEVINE to mapOf("blastberry" to 4, "cheesebite" to 4),
            STOPLIGHT_PETAL to mapOf("snoozling" to 4, "noctilume" to 4),
            THUNDERLING to mapOf("soggybud" to 5, "noctilume" to 3),
            TURTLELLINI to mapOf("soggybud" to 4, "choconut" to 4),
            ZOMBUD to mapOf("dead_plant" to 4, "cindershade" to 2, "fleshtrap" to 2),
            ALL_IN_ALOE to mapOf("magic_jellybean" to 6, "plantboy_advance" to 2),
            DEVOURER to mapOf("puffercloud" to 4, "zombud" to 4),
            GLASSCORN to mapOf("startlevine" to 6, "chloronite" to 6),
            PHANTOMLEAF to mapOf("chorus_fruit" to 4, "shellfruit" to 4),
            TIMESTALK to mapOf("stoplight_petal" to 4, "chorus_fruit" to 2, "shellfruit" to 2),
        )

        private val yieldBuffMutations = setOf(
            ALL_IN_ALOE,
            ASHWREATH,
            BLASTBERRY,
            CINDERSHADE,
            DEVOURER,
            DO_NOT_EAT_SHROOM,
            DUSTGRAIN,
            DUSKBLOOM,
            FLESHTRAP,
            GODSEED,
            GLOOMGOURD,
            LONELILY,
            PLANTBOY_ADVANCE,
            PUFFERCLOUD,
            SNOOZLING,
            VEILSHROOM,
            ZOMBUD,
        )

        private fun GreenhouseMutation.repoStack(): SafeItemStack? = internalId.toInternalName().getItemStackOrNull()
    }
}

private fun normalizeMutationName(name: String): String = name.lowercase().filter(Char::isLetterOrDigit)
