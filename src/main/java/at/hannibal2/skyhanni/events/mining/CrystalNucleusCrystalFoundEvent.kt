package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.NucleusCrystalType

class CrystalNucleusCrystalFoundEvent(val crystalType: NucleusCrystalType) : SkyHanniEvent()
