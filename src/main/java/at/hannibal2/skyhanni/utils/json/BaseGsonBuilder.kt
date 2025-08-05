package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.other.NbtBoolean
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NEURaritySpecificPetNums
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuAbstractRecipe
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeComponent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.LegacyStringChromaColourTypeAdapter
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory
import net.minecraft.item.ItemStack
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration

object BaseGsonBuilder {
    fun gson(): GsonBuilder = GsonBuilder().setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeSpecialFloatingPointValues()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
        .registerTypeAdapter(UUID::class.java, SkyHanniTypeAdapters.UUID.nullSafe())
        .registerTypeAdapter(NbtBoolean::class.java, SkyHanniTypeAdapters.NBT_BOOLEAN.nullSafe())
        .registerTypeAdapter(LorenzVec::class.java, SkyHanniTypeAdapters.VEC_STRING.nullSafe())
        .registerTypeAdapter(TrophyRarity::class.java, SkyHanniTypeAdapters.TROPHY_RARITY.nullSafe())
        .registerTypeAdapter(NeuRecipeComponent::class.java, SkyHanniTypeAdapters.NEU_RECIPE_COMPONENT.nullSafe())
        .registerTypeAdapter(NeuAbstractRecipe::class.java, SkyHanniTypeAdapters.NEU_ABSTRACT_RECIPE.nullSafe())
        .registerTypeAdapter(NeuRecipeType::class.java, SkyHanniTypeAdapters.NEU_RECIPE_TYPE.nullSafe())
        .registerTypeAdapter(NEURaritySpecificPetNums::class.java, SkyHanniTypeAdapters.NEU_RARITY_SPECIFIC_PET_NUMS.nullSafe())
        .registerTypeAdapter(ItemStack::class.java, SkyHanniTypeAdapters.NEU_ITEMSTACK.nullSafe())
        .registerTypeAdapter(NeuInternalName::class.java, SkyHanniTypeAdapters.INTERNAL_NAME.nullSafe())
        .registerTypeAdapter(LorenzRarity::class.java, SkyHanniTypeAdapters.RARITY.nullSafe())
        .registerTypeAdapter(IslandType::class.java, SkyHanniTypeAdapters.ISLAND_TYPE.nullSafe())
        .registerTypeAdapter(ModVersion::class.java, SkyHanniTypeAdapters.MOD_VERSION.nullSafe())
        .registerTypeAdapter(ChromaColour::class.java, LegacyStringChromaColourTypeAdapter(true).nullSafe())
        .registerTypeAdapter(
            SkyHanniTracker.DefaultDisplayMode::class.java,
            SkyHanniTypeAdapters.TRACKER_DISPLAY_MODE.nullSafe(),
        )
        .registerTypeAdapter(SimpleTimeMark::class.java, SkyHanniTypeAdapters.TIME_MARK.nullSafe())
        .registerTypeAdapter(Duration::class.java, SkyHanniTypeAdapters.DURATION.nullSafe())
        .registerTypeAdapter(LocalDate::class.java, SkyHanniTypeAdapters.LOCALE_DATE.nullSafe())
        .enableComplexMapKeySerialization()

    fun lenientGson(): GsonBuilder = gson()
        .registerTypeAdapterFactory(SkippingTypeAdapterFactory)
        .registerTypeAdapterFactory(ListEnumSkippingTypeAdapterFactory)
}
