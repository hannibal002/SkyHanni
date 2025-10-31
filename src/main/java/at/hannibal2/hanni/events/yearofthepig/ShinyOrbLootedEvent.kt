package at.hannibal2.hanni.events.yearofthepig

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.skillprogress.SkillType
import at.hannibal2.hanni.utils.NeuInternalName

class ShinyOrbLootedEvent(
    val loot: Pair<NeuInternalName, Int>? = null,
    val coins: Int? = null,
    val skillXp: Pair<SkillType, Long>? = null,
) : HanniEvent()

