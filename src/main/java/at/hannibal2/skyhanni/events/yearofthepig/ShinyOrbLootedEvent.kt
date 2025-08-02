package at.hannibal2.skyhanni.events.yearofthepig

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.utils.NeuInternalName

class ShinyOrbLootedEvent(
    val loot: Pair<NeuInternalName, Int>? = null,
    val coins: Int? = null,
    val skillXp: Pair<SkillType, Long>? = null,
) : SkyHanniEvent()

