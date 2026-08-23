package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import kotlin.properties.ReadOnlyProperty
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkullTextureHolder {

    @Suppress("MaxLineLength", "SkullTexturesUseRepo")
    private const val ALEX_SKIN_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxMTY1OTI2NDg1NSwKICAicHJvZmlsZUlkIiA6ICI2YWI0MzE3ODg5ZmQ0OTA1OTdmNjBmNjdkOWQ3NmZkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfQWxleCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84M2NlZTVjYTZhZmNkYjE3MTI4NWFhMDBlODA0OWMyOTdiMmRiZWJhMGVmYjhmZjk3MGE1Njc3YTFiNjQ0MDMyIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="

    private var skullTextures = mutableMapOf<String, String>()
    private val cachedTextures = mutableListOf<StableOrTransientValue<*>>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        skullTextures = event.getConstant<Map<String, String>>("Skulls").toMutableMap()
        cachedTextures.forEach { it.reset() }
    }

    fun getTexture(name: String): String? = skullTextures[name]

    fun getTextureOrFallback(name: String): String = getTexture(name) ?: ALEX_SKIN_TEXTURE

    fun texture(name: String): ReadOnlyProperty<Any?, String?> = StableOrTransientValue(1.seconds) {
        getTexture(name)?.let { StableOrTransientValue.stable(it) } ?: StableOrTransientValue.transient(null)
    }.also {
        cachedTextures.add(it)
    }
}
