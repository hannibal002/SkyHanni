package at.hannibal2.skyhanni.events.utils

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * When the "init" phase of mod loading is done.
 * On Forge, this happens inside [[net.minecraftforge.fml.common.event.FMLInitializationEvent]].
 */
object InitFinishedEvent : SkyHanniEvent()
