package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when SkyHanni's config and profile storage are loaded and ready to use.
 *
 * Fired on the main thread from `ProfileStorageData`.
 *
 * This event fires multiple times per session: once when player-specific storage is initialized
 * on Hypixel join, and again each time a SkyBlock profile is loaded or switched.
 * Use [firstLoad] to distinguish the initial load from subsequent profile switches.
 *
 * @param firstLoad true only the first time this event fires in the current game session
 */
@PrimaryFunction("onConfigLoad")
class ConfigLoadEvent(val firstLoad: Boolean) : SkyHanniEvent()
