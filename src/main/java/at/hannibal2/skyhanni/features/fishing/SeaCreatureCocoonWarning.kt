package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils

@SkyHanniModule
object SeaCreatureCocoonWarning {

    private val config get() = SkyHanniMod.feature.fishing.cocoonSettings
    private val scSpecificConfig get() = SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage

    @HandleEvent
    fun onCocoon(event: CocoonSpawnEvent) {
        val mob = event.cocoonMob
        if (mob.seaCreature == null) return
        if (!mob.seaCreature.isOwn) return
        val name = mob.seaCreature.name
        if (scSpecificConfig[name]?.shouldWarnWhenCocooned == true && config.shareInPartyChat) {
            TitleManager.sendTitle("§c$name Has Been Cocooned")
            SoundUtils.repeatSound(
                1,
                repeat = 5,
                sound = SoundUtils.plingSound,
            )
        }
        if (scSpecificConfig[name]?.shouldShareCocoonInChat == true && config.warnWhenCocooned) {
            if (PartyApi.isInParty()) {
                HypixelCommands.partyChat("I Cocooned ${StringUtils.optionalAn(name)} $name!")
            }
        }
    }
}
