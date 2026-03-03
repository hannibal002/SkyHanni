package at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.features.fishing.LivingSeaCreatureData
import at.hannibal2.skyhanni.features.fishing.SeaCreature
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi.seaCreature

object SeaCreatureSettings {

    private val config get() = SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage

    fun getConfig(mob: Mob): SpecificSeaCreatureSettings? = mob.seaCreature?.let {
        getConfig(it)
    }

    fun getConfig(seaCreatureData: LivingSeaCreatureData): SpecificSeaCreatureSettings? =
        getConfig(seaCreatureData.seaCreature)

    fun getConfig(seaCreature: SeaCreature): SpecificSeaCreatureSettings? = getConfig(seaCreature.name)

    fun getConfig(name: String): SpecificSeaCreatureSettings? = config[name]
}
