package at.hannibal2.hanni.events.slayer

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.SlayerApi.ActiveQuestState

/**
 * When the type of active slayer changes. e.g. from rev3 to eman4
 */
class SlayerChangeEvent(val oldSlayer: String, val newSlayer: String) : HanniEvent()

/**
 * When the percentage or state changes, as string
 */
class SlayerProgressChangeEvent(val oldProgress: String, val newProgress: String) : HanniEvent()

/**
 * When the current state of the active quest changes: start, failed, slain
 */
class SlayerStateChangeEvent(val state: ActiveQuestState) : HanniEvent()
