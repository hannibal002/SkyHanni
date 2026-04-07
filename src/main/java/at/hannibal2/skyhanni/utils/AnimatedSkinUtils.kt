package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.AnimatedSkinJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuAnimatedSkullsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuAnimatedDyeJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import com.google.gson.JsonObject
import net.minecraft.nbt.CompoundTag

/**
 * Central store for data loaded from the NEU `animatedskulls` repository entry (as well as `dyes`)
 *
 * Provides the shared skin-variant-index lookup used by both armor and pet skin rendering,
 * and exposes the pre-split skin maps consumed by [at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeApi]
 * and [PetUtils].
 */
@SkyHanniModule
object AnimatedSkinUtils {

    private val repoReloadCoroutine = CoroutineSettings("animated skin utils repo reload")

    /** Animated dye entries for armor, with hex strings pre-parsed into color integers. */
    var animatedDyes: Map<NeuInternalName, List<Int>> = mapOf()
        private set

    /** Animated skin entries for armor (keys do not start with `PET_SKIN`). */
    var armorSkins: Map<String, AnimatedSkinJson> = mapOf()
        private set

    /** Animated skin entries for pet skins (keys start with `PET_SKIN`). */
    var petSkins: Map<String, AnimatedSkinJson> = mapOf()
        private set

    /**
     * Maps a pet skin's internal name to an ordered list of variant-skin identifiers,
     * each of which is itself a key in [petSkins].
     */
    var petSkinVariants: Map<NeuInternalName, List<String>> = mapOf()
        private set

    private var petSkinNbtNames: Set<String> = setOf()

    /**
     * NBT keys used to store the variant index on armor items with variant skins.
     * Defaults to `{"favorite_crop"}` (used by the Helianthus Helmet) until overridden by repo data.
     */
    var armorSkinNbtNames: Set<String> = setOf("favorite_crop")
        private set

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) = repoReloadCoroutine.launch {
        val dyeData = event.getConstantAsync<NeuAnimatedDyeJson>("dyes")
        animatedDyes = dyeData.animated.mapValues { (_, colors) -> colors.map { it.trimStart('#').toLong(16).toInt() } }

        val skinData = event.getConstantAsync<NeuAnimatedSkullsJson>("animatedskulls")
        armorSkins = skinData.skins.filterKeys { !it.startsWith("PET_SKIN") }
        petSkins = skinData.skins.filter { it.key.startsWith("PET_SKIN") }
        petSkinVariants = skinData.petSkinVariants
        petSkinNbtNames = skinData.petSkinNbtNames
        armorSkinNbtNames = skinData.armorSkinNbtNames
    }

    /**
     * Reads the skin variant index stored in the given GSON extra-attributes object.
     * Returns null when no variant NBT key is present.
     *
     * @param extraData The `ExtraAttributes` JSON object (e.g. from deserialized pet info).
     */
    fun getVariantIndexOrNull(extraData: JsonObject): Int? = petSkinNbtNames.firstNotNullOfOrNull {
        extraData.get(it)?.asInt
    }

    /**
     * Reads the skin variant index stored in the given NBT extra-attributes compound.
     * Returns null when no variant NBT key is present.
     *
     * @param extraData The `ExtraAttributes` [CompoundTag] from an armor item stack.
     */
    fun getVariantIndexOrNull(extraData: CompoundTag): Int? = armorSkinNbtNames.firstNotNullOfOrNull { key ->
        extraData.getInt(key).takeIf { extraData.contains(key) }?.get()
    }
}
