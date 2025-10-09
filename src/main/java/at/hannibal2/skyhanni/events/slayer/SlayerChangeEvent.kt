package at.hannibal2.skyhanni.events.slayer

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.SlayerApi.ActiveQuestState

/**
 * When the type of active slayer changes. e.g. from rev3 to eman4
 */
class SlayerChangeEvent(val oldSlayer: String, val newSlayer: String) : SkyHanniEvent()

/**
 * When the percentage or state changes, as string
 */
class SlayerProgressChangeEvent(val oldProgress: String, val newProgress: String) : SkyHanniEvent()

/**
 * When the current state of the active quest changes: start, failed, slain
 */
class SlayerStateChangeEvent(val state: ActiveQuestState) : SkyHanniEvent()
