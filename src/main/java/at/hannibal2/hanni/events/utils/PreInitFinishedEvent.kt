package at.hannibal2.hanni.events.utils

import at.hannibal2.hanni.api.event.HanniEvent

/**
 * When the "pre init" phase of mod loading is done.
 * On Forge, this happens inside [[net.minecraftforge.fml.common.event.FMLPreInitializationEvent]].
 */
object PreInitFinishedEvent : HanniEvent()
