package at.hannibal2.skyhanni.events.utils

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * When the "pre init" phase of mod loading is done.
 * On Forge, this happens inside [[net.minecraftforge.fml.common.event.FMLPreInitializationEvent]].
 */
object PreInitFinishedEvent : SkyHanniEvent()
