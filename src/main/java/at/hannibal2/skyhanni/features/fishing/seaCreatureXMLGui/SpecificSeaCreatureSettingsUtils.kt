package at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.features.fishing.LivingSeaCreatureData
import at.hannibal2.skyhanni.features.fishing.SeaCreature
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi.seaCreature

object SpecificSeaCreatureSettingsUtils {

    private val scSpecificConfig get() = SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage

    fun getSeaCreatureConfig(mob: Mob): SpecificSeaCreatureSettings? = scSpecificConfig[mob.seaCreature?.seaCreature?.name]

    fun getSeaCreatureConfig(seaCreatureData: LivingSeaCreatureData): SpecificSeaCreatureSettings? = scSpecificConfig[seaCreatureData.seaCreature.name]

    fun getSeaCreatureConfig(name: String): SpecificSeaCreatureSettings? = scSpecificConfig[name]

    fun getSeaCreatureConfig(seaCreature: SeaCreature): SpecificSeaCreatureSettings? = scSpecificConfig[seaCreature.name]
}
