package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
//#if TODO
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventType
//#endif
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object SkullTextureHolder {

    private var skullTextures = mutableMapOf<String, String>()
    // I just took this from the Skulls.json
    @Suppress("MaxLineLength")
    private val ALEX_SKIN_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxMTY1OTI2NDg1NSwKICAicHJvZmlsZUlkIiA6ICI2YWI0MzE3ODg5ZmQ0OTA1OTdmNjBmNjdkOWQ3NmZkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfQWxleCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84M2NlZTVjYTZhZmNkYjE3MTI4NWFhMDBlODA0OWMyOTdiMmRiZWJhMGVmYjhmZjk3MGE1Njc3YTFiNjQ0MDMyIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        skullTextures = event.getConstant<Map<String, String>>("Skulls").toMutableMap()
        fixLateInits()
    }

    fun getTexture(name: String): String {
        return skullTextures[name] ?: ALEX_SKIN_TEXTURE
    }

    // Any classes that rely on textures that cannot make use of by lazy or other late initializers
    private fun fixLateInits() {
        //#if TODO
        MiningEventType.fixGoblinItemStack()
        //#endif
    }
}
