package at.hannibal2.skyhanni.test.entity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.holdingSkullTexture
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.entity.item.EntityArmorStand

@SkyHanniModule
class DebugEntitySkinCommand {

    private var skinToFind: String? = null
    private var foundEntities = setOf<EntityArmorStand>()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebugentityskin") {
            description = "Highlights entities in world that have the given skin texture."
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { toggleSkin() }
        }
    }

    private fun toggleSkin() {
        skinToFind?.let {
            skinToFind = null
            ChatUtils.chat("Disabled debug entity skin highlighter.")
            foundEntities = emptySet()
            return
        }
        SkyHanniMod.launchIOCoroutine {
            val skin = OSUtils.readFromClipboard() ?: error("no string in clipboard")
            skinToFind = skin
            ChatUtils.chat("Enabled and set clipboard to skin texture for entity skin highlighter.")
            updateSkinEntities(skin)
        }
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        skinToFind?.let { updateSkinEntities(it) }
    }

    private fun updateSkinEntities(skin: String) {
        foundEntities = EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(30.0)
            .filter { it.holdingSkullTexture(skin) || it.wearingSkullTexture(skin) }
            .toSet()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        for (location in foundEntities.map { it.getLorenzVec() }) {
            event.drawWaypointFilled(location, LorenzColor.YELLOW.toColor())
            event.drawDynamicText(location, "Skin", 1.5)
        }
    }
}

