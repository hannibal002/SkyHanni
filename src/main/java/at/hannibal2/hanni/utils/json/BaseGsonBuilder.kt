package at.hannibal2.hanni.utils.json

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.jsonobjects.other.NbtBoolean
import at.hannibal2.hanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.hanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.hanni.utils.LorenzRarity
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.Stopwatch
import at.hannibal2.hanni.utils.system.ModVersion
import at.hannibal2.hanni.utils.system.PlatformUtils
import at.hannibal2.hanni.utils.tracker.HanniTracker
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
        .registerTypeAdapter(UUID::class.java, HanniTypeAdapters.UUID.nullSafe())
        .registerTypeAdapter(NbtBoolean::class.java, HanniTypeAdapters.NBT_BOOLEAN.nullSafe())
        .registerTypeAdapter(LorenzVec::class.java, HanniTypeAdapters.VEC_STRING.nullSafe())
        .registerTypeAdapter(TrophyRarity::class.java, HanniTypeAdapters.TROPHY_RARITY.nullSafe())
        .registerTypeAdapter(ItemStack::class.java, HanniTypeAdapters.NEU_ITEMSTACK.nullSafe())
        .registerTypeAdapter(NeuInternalName::class.java, HanniTypeAdapters.INTERNAL_NAME.nullSafe())
        .registerTypeAdapter(LorenzRarity::class.java, HanniTypeAdapters.RARITY.nullSafe())
        .registerTypeAdapter(IslandType::class.java, HanniTypeAdapters.ISLAND_TYPE.nullSafe())
        .registerTypeAdapter(ModVersion::class.java, HanniTypeAdapters.MOD_VERSION.nullSafe())
        .registerTypeAdapter(ChromaColour::class.java, LegacyStringChromaColourTypeAdapter(true).nullSafe())
        .registerTypeAdapter(
            HanniTracker.DefaultDisplayMode::class.java,
            HanniTypeAdapters.TRACKER_DISPLAY_MODE.nullSafe(),
        )
        .registerTypeAdapter(SimpleTimeMark::class.java, HanniTypeAdapters.TIME_MARK.nullSafe())
        .registerTypeAdapter(Duration::class.java, HanniTypeAdapters.DURATION.nullSafe())
        .registerTypeAdapter(Stopwatch::class.java, HanniTypeAdapters.STOPWATCH.nullSafe())
        .registerTypeAdapter(LocalDate::class.java, HanniTypeAdapters.LOCALE_DATE.nullSafe())
        .enableComplexMapKeySerialization()

    fun lenientGson(): GsonBuilder {
        if (PlatformUtils.isDevEnvironment) return gson()
        return gson()
            .registerTypeAdapterFactory(SkippingTypeAdapterFactory)
            .registerTypeAdapterFactory(ListEnumSkippingTypeAdapterFactory)
    }
}
