package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.client.resources.IResourceManager

class ResourcePackReloadEvent(
    val resourceManager: IResourceManager
) : SkyHanniEvent()
