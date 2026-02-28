package at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.features.fishing.LivingSeaCreatureData
import at.hannibal2.skyhanni.features.fishing.SeaCreature
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi.seaCreature

object SpecificSeaCreatureSettingsUtils {

    private val scSpecificConfig get() = SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage

    fun getSeaCreatureConfig(mob: Mob): SpecificSeaCreatureSettings? {
        return getSeaCreatureConfig(mob.seaCreature ?: return null)
    }

    fun getSeaCreatureConfig(seaCreatureData: LivingSeaCreatureData): SpecificSeaCreatureSettings? = getSeaCreatureConfig(seaCreatureData.seaCreature)

    fun getSeaCreatureConfig(seaCreature: SeaCreature): SpecificSeaCreatureSettings? = getSeaCreatureConfig(seaCreature.name)

    fun getSeaCreatureConfig(name: String): SpecificSeaCreatureSettings? = scSpecificConfig[name]
}
