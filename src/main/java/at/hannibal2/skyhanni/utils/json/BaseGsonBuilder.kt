package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.other.NbtBoolean
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.LegacyStringChromaColourTypeAdapter
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory
import net.minecraft.world.item.ItemStack
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
        .registerTypeAdapter(ItemStack::class.java, SkyHanniTypeAdapters.NEU_ITEMSTACK.nullSafe())
        .registerTypeAdapter(NeuInternalName::class.java, SkyHanniTypeAdapters.INTERNAL_NAME.nullSafe())
        .registerTypeAdapter(LorenzRarity::class.java, SkyHanniTypeAdapters.RARITY.nullSafe())
        .registerTypeAdapter(IslandType::class.java, SkyHanniTypeAdapters.ISLAND_TYPE.nullSafe())
        .registerTypeAdapter(CropType::class.java, SkyHanniTypeAdapters.CROP_TYPE.nullSafe())
        .registerTypeAdapter(PestType::class.java, SkyHanniTypeAdapters.PEST_TYPE.nullSafe())
        .registerTypeAdapter(ModVersion::class.java, SkyHanniTypeAdapters.MOD_VERSION.nullSafe())
        .registerTypeAdapter(ChromaColour::class.java, LegacyStringChromaColourTypeAdapter(true).nullSafe())
        .registerTypeAdapter(EliteLeaderboardType::class.java, SkyHanniTypeAdapters.ELITE_LEADERBOARD_TYPE.nullSafe())
        .registerTypeAdapter(
            SkyHanniTracker.DefaultDisplayMode::class.java,
            SkyHanniTypeAdapters.TRACKER_DISPLAY_MODE.nullSafe(),
        )
        .registerTypeAdapter(SimpleTimeMark::class.java, SkyHanniTypeAdapters.TIME_MARK.nullSafe())
        .registerTypeAdapter(Duration::class.java, SkyHanniTypeAdapters.DURATION.nullSafe())
        .registerTypeAdapter(Stopwatch::class.java, SkyHanniTypeAdapters.STOPWATCH.nullSafe())
        .registerTypeAdapter(LocalDate::class.java, SkyHanniTypeAdapters.LOCALE_DATE.nullSafe())
        .registerTypeAdapter(SessionUptime::class.java, SkyHanniTypeAdapters.SESSION_UPTIME.nullSafe())
        .enableComplexMapKeySerialization()

    fun lenientGson(): GsonBuilder {
        if (PlatformUtils.isDevEnvironment) return gson()
        return gson()
            .registerTypeAdapterFactory(SkippingTypeAdapterFactory)
            .registerTypeAdapterFactory(ListEnumSkippingTypeAdapterFactory)
    }
}
