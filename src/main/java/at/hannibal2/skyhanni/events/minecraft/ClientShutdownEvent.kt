package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the client starts shutting down.
 *
 * Posted from `ClientLifecycleEvents.CLIENT_STOPPING`, which Fabric injects at the top of
 * `Minecraft.exitWorldAndClose`. The render loop has already stopped, but the level is still loaded
 * and `Minecraft.close()` has not run yet, so the GPU device and all render resources are still
 * valid. Closing GPU resources any later is a use-after-free on some backends.
 *
 * Runs on the game thread, which is also the render thread.
 *
 * Do not use this to react to leaving a world, use [ClientDisconnectEvent] instead.
 */
@PrimaryFunction("onClientShutdown")
object ClientShutdownEvent : SkyHanniEvent()
