package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.platform.InputConstants

@SkyHanniModule
object UpdateKeybinds {

    var keybinds = setOf(
        "gui.keyBindOpen",
        "gui.keyBindReset",
        "garden.keyBind.attack",
        "garden.keyBind.useItem",
        "garden.keyBind.left",
        "garden.keyBind.right",
        "garden.keyBind.forward",
        "garden.keyBind.back",
        "garden.keyBind.jump",
        "garden.keyBind.sneak",
        "garden.tooltipTweak.fortuneTooltipKeybind",
        "garden.mouseSensitivityReducer.keybind",
        "garden.gardenCommands.homeHotkey",
        "garden.gardenCommands.sethomeHotkey",
        "garden.gardenCommands.barnHotkey",
        "garden.seeThroughWindow.keybind",
        "garden.plotBorderKey",
        "garden.visitors.rewardWarning.bypassKey",
        "garden.visitors.acceptHotkey",
        "garden.pests.pestFinder.teleportHotkey",
        "garden.pests.pestTrap.releaseHotkey",
        "crimsonIsle.reputationHelper.hotkey",
        "crimsonIsle.vanquisherShare.keybindSharing",
        "fishing.trophyFishing.display.keybind",
        "mining.glaciteMineshaft.shareWaypointLocation",
        "mining.tunnelMaps.campfireKey",
        "mining.tunnelMaps.nextSpotHotkey",
        "hunting.shardTracker.selectShardKeybind",
        "hunting.fusionKeybinds.repeatFusionKeybind",
        "hunting.fusionKeybinds.confirmFusionKeybind",
        "hunting.nextShulkerKeybind",
        "combat.instanceChestProfit.keybind",
        "dungeon.spiritLeapOverlay.spiritLeapKeybindConfig.keybindOption1",
        "dungeon.spiritLeapOverlay.spiritLeapKeybindConfig.keybindOption2",
        "dungeon.spiritLeapOverlay.spiritLeapKeybindConfig.keybindOption3",
        "dungeon.spiritLeapOverlay.spiritLeapKeybindConfig.keybindOption4",
        "inventory.personalCompactor.keybind",
        "inventory.focusMode.toggleKey",
        "inventory.jacobFarmingContests.openOnElite",
        "inventory.estimatedItemValues.hotkey",
        "inventory.gfs.keybind",
        "inventory.gfs.compactorKeybind",
        "inventory.pageScrolling.bypassKey",
        "inventory.auctions.copyUnderbidKeybind",
        "inventory.helper.harp.harpKeybinds.key1",
        "inventory.helper.harp.harpKeybinds.key2",
        "inventory.helper.harp.harpKeybinds.key3",
        "inventory.helper.harp.harpKeybinds.key4",
        "inventory.helper.harp.harpKeybinds.key5",
        "inventory.helper.harp.harpKeybinds.key6",
        "inventory.helper.harp.harpKeybinds.key7",
        "inventory.customWardrobe.tooltipKeybind",
        "inventory.customWardrobe.keybinds.slot1",
        "inventory.customWardrobe.keybinds.slot2",
        "inventory.customWardrobe.keybinds.slot3",
        "inventory.customWardrobe.keybinds.slot4",
        "inventory.customWardrobe.keybinds.slot5",
        "inventory.customWardrobe.keybinds.slot6",
        "inventory.customWardrobe.keybinds.slot7",
        "inventory.customWardrobe.keybinds.slot8",
        "inventory.customWardrobe.keybinds.slot9",
        "inventory.customLoadout.keybinds.slot1",
        "inventory.customLoadout.keybinds.slot2",
        "inventory.customLoadout.keybinds.slot3",
        "inventory.customLoadout.keybinds.slot4",
        "inventory.customLoadout.keybinds.slot5",
        "inventory.customLoadout.keybinds.slot6",
        "inventory.customLoadout.keybinds.slot7",
        "inventory.customLoadout.keybinds.slot8",
        "inventory.customLoadout.keybinds.slot9",
        "inventory.customLoadout.keybinds.slot10",
        "inventory.customLoadout.keybinds.slot11",
        "inventory.customLoadout.keybinds.slot12",
        "inventory.chocolateFactory.keybinds.key1",
        "inventory.chocolateFactory.keybinds.key2",
        "inventory.chocolateFactory.keybinds.key3",
        "inventory.chocolateFactory.keybinds.key4",
        "inventory.chocolateFactory.keybinds.key5",
        "inventory.chocolateFactory.keybinds.key6",
        "inventory.chocolateFactory.keybinds.key7",
        "event.diana.keyBindWarp",
        "event.diana.rareMobsSharing.keyBindShare",
        "event.hoppityEggs.eventSummary.liveDisplay.toggleKeybind",
        "chat.peekChat",
        "misc.trevorTheTrapper.keyBind",
        "misc.abiphoneAcceptKey",
        "misc.commands.betterWiki.wikiKeybind",
        "dev.debug.copyInternalName",
        "dev.debug.copyCosmeticsSkullData",
        "dev.debug.copyItemData",
        "dev.debug.copyItemDataCompressed",
        "dev.debug.bypassAdvancedPlayerTabList",
        "dev.debug.trackSound.toggleKeybind",
        "dev.debug.trackParticle.toggleKeybind",
        "dev.showSlotNumberKey",
        "dev.waypoint.saveKey",
        "dev.waypoint.deleteKey",
        "dev.devTool.graph.placeKey",
        "dev.devTool.graph.selectKey",
        "dev.devTool.graph.selectRaycastKey",
        "dev.devTool.graph.connectKey",
        "dev.devTool.graph.exitKey",
        "dev.devTool.graph.editKey",
        "dev.devTool.graph.textKey",
        "dev.devTool.graph.dijkstraKey",
        "dev.devTool.graph.saveKey",
        "dev.devTool.graph.loadKey",
        "dev.devTool.graph.clearKey",
        "dev.devTool.graph.throughBlocksKey",
        "dev.devTool.graph.tutorialKey",
        "dev.devTool.graph.splitKey",
        "dev.devTool.graph.dissolveKey",
        "dev.devTool.graph.edgeCycle",
    )

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        for (keybind in keybinds) {
            event.transform(147, keybind) { element ->
                val oldCode = element.asInt
                val type =
                    if (oldCode in 0 until MouseCompat.NUMBER_OF_MOUSE_BUTTONS) InputConstants.Type.MOUSE else InputConstants.Type.KEYSYM
                val newStringName = type.getOrCreate(oldCode).name
                JsonPrimitive(newStringName)
            }
        }
    }

    private fun resetKeybind(key: String) {
        SkyHanniConfigSearchResetCommand.resetCommand(arrayOf("reset", "config.$key"))
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetkeybinds") {
            category = CommandCategory.USERS_RESET
            description = "Resets all of your skyhanni keybinds"
            aliases = listOf("shkeybindreset")
            simpleCallback {
                for (keybind in keybinds) {
                    resetKeybind(keybind)
                }
                ChatUtils.chat("§aSuccessfully reset all SkyHanni Keybinds")
            }
        }
    }
}
