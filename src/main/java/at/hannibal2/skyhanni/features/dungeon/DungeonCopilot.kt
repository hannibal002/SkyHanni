package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.DungeonEnterEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonCopilot {

    private val config get() = SkyHanniMod.feature.dungeon.dungeonCopilot

    private val countdownPattern =
        "(.*) has started the dungeon countdown. The dungeon will begin in 1 minute.".toPattern()
    private val keyPatternsList = listOf(
        "§eA §r§a§r§[6c]§r§[8c](?<key>Wither|Blood) Key§r§e was picked up!".toPattern(),
        "(.*) §r§ehas obtained §r§a§r§[6c]§r§[8c](?<key>Wither|Blood) Key§r§e!".toPattern()
    )
    private val witherDoorPattern = "(.*) opened a §r§8§lWITHER §r§adoor!".toPattern()
    private val bloodDoorPattern = "§cThe §r§c§lBLOOD DOOR§r§c has been opened!".toPattern()

    private var nextStep = ""
    private var searchForKey = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        copilot(event.message)?.let {
            event.blockedReason = it
        }
    }

    private fun copilot(message: String): String? {
        countdownPattern.matchMatcher(message) {
            changeNextStep("Ready up")
        }

        if (message.endsWith("§a is now ready!") && message.contains(LorenzUtils.getPlayerName())) {
            changeNextStep("Wait for the dungeon to start!")
        }

        // Key Pickup
        var foundKeyOrDoor = false
        keyPatternsList.any {
            it.matchMatcher(message) {
                val key = group("key")
                changeNextStep("Open $key Door")
                foundKeyOrDoor = true
            } != null
        }

        witherDoorPattern.matchMatcher(message) {
            changeNextStep("Clear next room")
            searchForKey = true
            foundKeyOrDoor = true
        }

        bloodDoorPattern.matchMatcher(message) {
            changeNextStep("Wait for Blood Room to fully spawn")
            foundKeyOrDoor = true
        }

        if (foundKeyOrDoor && SkyHanniMod.feature.dungeon.messageFilter.keysAndDoors) return "dungeon_keys_and_doors"

        if (message == "§c[BOSS] The Watcher§r§f: That will be enough for now.") changeNextStep("Clear Blood Room")

        if (message == "§c[BOSS] The Watcher§r§f: You have proven yourself. You may pass.") {
            changeNextStep("Enter Boss Room")
            return "dungeon_copilot"
        }
        return null
    }

    private fun changeNextStep(step: String) {
        nextStep = step
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inDungeons) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return

        if (!searchForKey) return

        if (entity.name == "§6§8Wither Key") {
            changeNextStep("Pick up Wither Key")
            searchForKey = false
        }
        if (entity.name == "§c§cBlood Key") {
            changeNextStep("Pick up Blood Key")
            searchForKey = false
        }
    }

    @SubscribeEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        changeNextStep("Clear first room")
        searchForKey = true
    }

    @SubscribeEvent
    fun onDungeonStart(event: DungeonEnterEvent) {
        changeNextStep("Talk to Mort")
        searchForKey = true
    }

    @SubscribeEvent
    fun onDungeonBossRoomEnter(event: DungeonBossRoomEnterEvent) {
        changeNextStep("Defeat the boss! Good luck :)")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        changeNextStep("")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inDungeons && config.enabled
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.pos.renderString(nextStep, posLabel = "Dungeon Copilot")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dungeon.messageFilterKeysAndDoors", "dungeon.messageFilter.keysAndDoors")
        event.move(3, "dungeon.copilotEnabled", "dungeon.dungeonCopilot.enabled")
        event.move(3, "dungeon.copilotPos", "dungeon.dungeonCopilot.pos")
    }
}
