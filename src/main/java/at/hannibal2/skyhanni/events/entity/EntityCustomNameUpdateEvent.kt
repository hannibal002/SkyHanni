package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

/**
 * Event that is fired when an entity's custom name is updated.
 *
 * @param T The type of the entity.
 * @property entity The entity whose custom name was updated.
 * @property newName The new custom name of the entity, or null if the name was removed.
 */
@PrimaryFunction("onEntityNameUpdate")
data class EntityCustomNameUpdateEvent<T : Entity>(
    val entity: T,
    val newName: Component?,
) : GenericSkyHanniEvent<T>(entity.javaClass) {

    @Deprecated("Use cleanName instead", ReplaceWith("cleanName"))
    val newNameFormatted by lazy {
        newName?.formattedTextCompatLessResets()
    }

    val cleanName by lazy {
        newName?.string?.removeColor()
    }
}
