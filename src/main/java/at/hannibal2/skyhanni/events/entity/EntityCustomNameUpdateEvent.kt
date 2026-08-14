package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

data class EntityCustomNameUpdateEvent<T : Entity>(
    val entity: T,
    val newName: Component?,
) : GenericSkyHanniEvent<T>(entity.javaClass) {

    @Deprecated("Use cleanName instead", ReplaceWith("cleanName"))
    val newNameFormatted = newName?.formattedTextCompatLessResets()

    val cleanName = newName?.string?.removeColor()
}
