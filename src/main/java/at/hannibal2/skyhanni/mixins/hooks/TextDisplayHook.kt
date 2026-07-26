package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.entity.DisplayTextUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.events.entity.EntityTextRemovedEvent
import at.hannibal2.skyhanni.events.entity.EntityTextUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object TextDisplayHook {
    @JvmStatic
    fun onTextDisplayUpdate(entity: Display.TextDisplay, newText: Component?) {
        DisplayTextUpdateEvent(entity, newText).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        EntityTextUpdateEvent(event.entity, event.newName).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onDisplayTextUpdate(event: DisplayTextUpdateEvent) {
        EntityTextUpdateEvent(event.entity, event.newText).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onArmorStandRemoved(event: EntityRemovedEvent<ArmorStand>) {
        EntityTextRemovedEvent(event.entity).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTextDisplayRemoved(event: EntityRemovedEvent<Display.TextDisplay>) {
        EntityTextRemovedEvent(event.entity).post()
    }
}
