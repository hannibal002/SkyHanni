package at.hannibal2.hanni.events.mining

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.NeuInternalName

class CrystalNucleusLootEvent(val loot: Map<NeuInternalName, Int>) : HanniEvent()
