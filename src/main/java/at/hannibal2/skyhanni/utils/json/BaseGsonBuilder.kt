package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.LegacyStringChromaColourTypeAdapter
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory

object BaseGsonBuilder {
    fun gson(): GsonBuilder = GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeSpecialFloatingPointValues()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
        .registerTypeAdapter(ChromaColour::class.java, LegacyStringChromaColourTypeAdapter(true).nullSafe())
        .registerSkyHanniAdapters()
        .enableComplexMapKeySerialization()

    fun lenientGson(): GsonBuilder {
        if (PlatformUtils.isDevEnvironment) return gson()
        return gson()
            .registerTypeAdapterFactory(SkippingTypeAdapterFactory)
            .registerTypeAdapterFactory(ListEnumSkippingTypeAdapterFactory)
    }
}

