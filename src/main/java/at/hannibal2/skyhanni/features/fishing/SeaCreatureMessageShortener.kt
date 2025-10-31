package at.hannibal2.hanni.features.fishing

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent

@HanniModule
object SeaCreatureMessageShortener {

    private val config get() = HanniMod.feature.fishing

    @HandleEvent(onlyOnSkyblock = true)
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {

        val original = event.chatEvent.chatComponent.formattedText
        var edited = original

        if (config.shortenFishingMessage) {
            val name = event.seaCreature.displayName
            val aOrAn = StringUtils.optionalAn(name.removeColor())
            edited = "§9You caught $aOrAn $name§9!"
        }

        if (config.compactDoubleHook && event.doubleHook) {
            edited = "§e§lDOUBLE HOOK! $edited"
        }

        if (original == edited) return
        event.chatEvent.chatComponent = edited.asComponent()
    }
}
