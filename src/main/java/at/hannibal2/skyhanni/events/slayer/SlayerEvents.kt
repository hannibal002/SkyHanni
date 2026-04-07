package at.hannibal2.skyhanni.events.slayer

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.SlayerApi.ActiveQuestState
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * When the type of the active slayer boss changes, e.g. from Revenant Horror 3 to Voidgloom Seraph 4.
 */
@PrimaryFunction("onSlayerChange")
class SlayerChangeEvent(val oldSlayer: String, val newSlayer: String) : SkyHanniEvent()

/**
 * When the percentage or state of the slayer quest changes.
 */
@PrimaryFunction("onSlayerProgressChange")
class SlayerProgressChangeEvent(val oldProgress: String, val newProgress: String) : SkyHanniEvent()

/**
 * When the current state of the active quest changes (e.g. BOSS_FIGHT, FAILED, SLAIN).
 */
@PrimaryFunction("onSlayerStateChange")
class SlayerStateChangeEvent(val state: ActiveQuestState) : SkyHanniEvent()

/**
 * When the active slayer quest completes.
 */
@PrimaryFunction("onSlayerQuestComplete")
object SlayerQuestCompleteEvent : SkyHanniEvent()
