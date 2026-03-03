package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SpecificSeaCreatureSettingsUtils.getSeaCreatureConfig
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils

@SkyHanniModule
object SeaCreatureCocoonWarning {

    private val config get() = SkyHanniMod.feature.fishing.cocoonSettings

    @HandleEvent
    fun onCocoon(event: CocoonSpawnEvent) {
        val mob = event.cocoonMob
        if (mob.seaCreature == null) return
        if (!mob.seaCreature.isOwn) return
        val name = mob.seaCreature.name
        if (config.warnWhenCocooned) {
            if (getSeaCreatureConfig(name)?.shouldWarnWhenCocooned == true) {
                val msg = "§c$name Has Been Cocooned"
                ChatUtils.notifyOrDisable(msg, config::warnWhenCocooned)
                TitleManager.sendTitle(msg)
                SoundUtils.repeatSound(
                    1,
                    repeat = 5,
                    sound = SoundUtils.plingSound,
                )
            }
        }
        if (config.shareInPartyChat) {
            if (getSeaCreatureConfig(name)?.shouldShareCocoonInChat == true) {
                if (PartyApi.isInParty()) {
                    HypixelCommands.partyChat("I Cocooned ${StringUtils.optionalAn(name)} $name!")
                }
            }
        }
    }
}
