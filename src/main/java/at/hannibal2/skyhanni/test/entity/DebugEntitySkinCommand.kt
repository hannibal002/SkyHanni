package at.hannibal2.hanni.test.entity

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.holdingSkullTexture
import at.hannibal2.hanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object DebugEntitySkinCommand {

    private var skinToFind: String? = null
    private var foundEntities = setOf<EntityArmorStand>()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebugentityskin") {
            description = "Highlights armor stands in the world that hold or wear a skull with the given skin texture."
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { toggleSkin() }
        }
    }

    private fun toggleSkin() {
        skinToFind?.let {
            skinToFind = null
            ChatUtils.chat("Disabled Debug Entity Skin Highlighter.")
            foundEntities = emptySet()
            return
        }
        HanniMod.launchIOCoroutine("debug entity skin read clipboard") {
            val skin = OSUtils.readFromClipboard() ?: error("no string in clipboard")
            skinToFind = skin
            ChatUtils.chat("Enabled Debug Entity Skin Highlighter and set clipboard as skin texture.")
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
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        for (location in foundEntities.map { it.getLorenzVec() }) {
            event.drawWaypointFilled(location, LorenzColor.YELLOW.toColor())
            event.drawDynamicText(location, "Skin", 1.5)
        }
    }
}

