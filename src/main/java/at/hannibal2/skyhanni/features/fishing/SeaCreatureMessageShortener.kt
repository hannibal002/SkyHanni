package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object SeaCreatureMessageShortener {

    private val config get() = SkyHanniMod.feature.fishing

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
