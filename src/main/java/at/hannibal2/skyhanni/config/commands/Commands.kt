package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.commands.SimpleCommand.ProcessCommandRunnable
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.features.bingo.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.BingoNextStepHelper
import at.hannibal2.skyhanni.features.commands.PartyCommands
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.features.event.diana.InquisitorWaypointShare
import at.hannibal2.skyhanni.features.fame.AccountUpgradeReminder
import at.hannibal2.skyhanni.features.fame.CityProjectFeatures
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenCropTimeCommand
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.features.garden.farming.GardenStartLocation
import at.hannibal2.skyhanni.features.garden.fortuneguide.CaptureFarmingGear
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.misc.CollectionTracker
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostUtil
import at.hannibal2.skyhanni.features.misc.massconfiguration.DefaultConfigFeatures
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.features.slayer.SlayerItemProfitTracker
import at.hannibal2.skyhanni.test.PacketTest
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.test.command.CopyItemCommand
import at.hannibal2.skyhanni.test.command.CopyNearbyEntitiesCommand
import at.hannibal2.skyhanni.test.command.CopyNearbyParticlesCommand
import at.hannibal2.skyhanni.test.command.CopyScoreboardCommand
import at.hannibal2.skyhanni.test.command.CopyTabListCommand
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.TestChatCommand
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.Minecraft
import net.minecraft.command.ICommandSender
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.ClientCommandHandler

object Commands {

    private val openMainMenu: (Array<String>) -> Unit = {
        if (it.isNotEmpty()) {
            if (it[0].lowercase() == "gui") {
                GuiEditManager.openGuiPositionEditor()
            } else {
                ConfigGuiManager.openConfigGui(it.joinToString(" "))
            }
        } else {
            val arr = mutableListOf<String>()
            ConfigGuiManager.openConfigGui()
        }
    }

    // command -> description
    private val commands = mutableListOf<CommandInfo>()

    enum class CommandCategory(val color: String, val categoryName: String, val description: String) {
        MAIN("§6", "Main Command", "Most useful commands of SkyHanni"),
        USERS_NORMAL("§e", "Normal Command", "Normal Command for everyone to use"),
        USERS_BUG_FIX("§f", "User Bug Fix", "A Command to fix small bugs"),
        DEVELOPER_CODING_HELP(
            "§5", "Developer Coding Help",
            "A Command that can help with developing new features. §cIntended for developers only!"
        ),
        DEVELOPER_DEBUG_FEATURES(
            "§9", "Developer Debug Features",
            "A Command that is useful for monitoring/debugging existing features. §cIntended for developers only!"
        ),
        INTERNAL("§8", "Internal Command", "A Command that should §cnever §7be called manually!"),
        SHORTENED_COMMANDS("§b", "Shortened Commands", "Commands that shorten or improve existing Hypixel commands!")
    }

    class CommandInfo(val name: String, val description: String, val category: CommandCategory)

    private var currentCategory = CommandCategory.MAIN

    fun init() {
        currentCategory = CommandCategory.MAIN
        usersMain()

        currentCategory = CommandCategory.USERS_NORMAL
        usersNormal()

        currentCategory = CommandCategory.USERS_BUG_FIX
        usersBugFix()

        currentCategory = CommandCategory.DEVELOPER_CODING_HELP
        developersCodingHelp()

        currentCategory = CommandCategory.DEVELOPER_DEBUG_FEATURES
        developersDebugFeatures()

        currentCategory = CommandCategory.INTERNAL
        internalCommands()

        currentCategory = CommandCategory.SHORTENED_COMMANDS
        shortenedCommands()
    }

    private fun usersMain() {
        registerCommand("sh", "Opens the main SkyHanni config", openMainMenu)
        registerCommand("skyhanni", "Opens the main SkyHanni config", openMainMenu)
        registerCommand("ff", "Opens the Farming Fortune Guide") { openFortuneGuide() }
        registerCommand("shcommands", "Shows this list") { commandHelp(it) }
        registerCommand0("shdefaultoptions", "Select default options", {
            DefaultConfigFeatures.onCommand(
                it.getOrNull(0) ?: "null", it.getOrNull(1) ?: "null"
            )
        }, DefaultConfigFeatures::onComplete)
    }

    private fun usersNormal() {
        registerCommand(
            "shmarkplayer",
            "Add a highlight effect to a player for better visibility"
        ) { MarkedPlayerManager.command(it) }
        registerCommand("shtrackcollection", "Tracks your collection gain over time") { CollectionTracker.command(it) }
        registerCommand(
            "shcroptime",
            "Calculates with your current crop per second speed how long you need to farm a crop to collect this amount of items"
        ) { GardenCropTimeCommand.onCommand(it) }
        registerCommand(
            "shrpcstart",
            "Manually starts the Discord Rich Presence feature"
        ) { DiscordRPCManager.startCommand() }
        registerCommand(
            "shcropstartlocation",
            "Manually sets the crop start location"
        ) { GardenStartLocation.setLocationCommand() }
        registerCommand(
            "shclearslayerprofits",
            "Clearing the total slayer profit for the current slayer type"
        ) { SlayerItemProfitTracker.clearProfitCommand(it) }
        registerCommand(
            "shimportghostcounterdata",
            "Manually importing the ghost counter data from GhostCounterV3"
        ) { GhostUtil.importCTGhostCounterData() }
        registerCommand(
            "shclearfarmingitems",
            "Clear farming items saved for the Farming Fortune Guide"
        ) { clearFarmingItems() }
        registerCommand("shresetghostcounter", "Resets the ghost counter stats") { GhostUtil.reset() }
        registerCommand("shbingotoggle", "Toggle the bingo card display mode") { BingoCardDisplay.toggleCommand() }
        registerCommand(
            "shfarmingprofile",
            "Look up the farming profile from yourself or another player on elitebot.dev"
        ) { FarmingWeightDisplay.lookUpCommand(it) }
//        registerCommand(
//            "shcopytranslation",
//            "<language code (2 letters)> <messsage to translate>\n" +
//                    "Requires the Chat > Translator feature to be enabled.\n" +
//                    "Copies the translation for a given message to your clipboard. " +
//                    "Language codes are at the end of the translation when you click on a message."
//        ) { Translator.fromEnglish(it) }
    }

    private fun usersBugFix() {
        registerCommand("shupdaterepo", "Download the SkyHanni repo again") { SkyHanniMod.repo.updateRepo() }
        registerCommand(
            "shresetburrowwarps",
            "Manually resetting disabled diana burrow warp points"
        ) { BurrowWarpHelper.resetDisabledWarps() }
        registerCommand(
            "shtogglehypixelapierrors",
            "Show/hide hypixel api error messages in chat"
        ) { APIUtil.toggleApiErrorMessages() }
        registerCommand(
            "shclearcropspeed",
            "Reset garden crop speed data and best crop time data"
        ) { GardenAPI.clearCropSpeed() }
        registerCommand(
            "shclearminiondata",
            "Reset data about minion profit and the name display on the private island"
        ) { MinionFeatures.clearMinionData() }
        registerCommand(
            "shconfig",
            "Search or reset config elements §c(warning, dangerous!)"
        ) { SkyHanniConfigSearchResetCommand.command(it) }
        registerCommand(
            "shdebugdata",
            "Prints debug data in the clipboard"
        ) { SkyHanniDebugsAndTests.debugData(it) }
        registerCommand(
            "shversion",
            "Prints the SkyHanni version in the chat"
        ) { SkyHanniDebugsAndTests.debugVersion() }
        registerCommand(
            "shrendertoggle",
            "Disables/enables the rendering of all skyhanni guis."
        ) { SkyHanniDebugsAndTests.toggleRender() }
        registerCommand(
            "shcarrot",
            "Toggles receiving the 12 fortune from carrots"
        ) { CaptureFarmingGear.reverseCarrotFortune() }
    }

    private fun developersDebugFeatures() {
        registerCommand("shtestbingo", "dev command") { TestBingo.toggle() }
        registerCommand("shprintbingohelper", "dev command") { BingoNextStepHelper.command() }
        registerCommand("shreloadbingodata", "dev command") { BingoCardDisplay.command() }
        registerCommand("shtestgardenvisitors", "dev command") { SkyHanniDebugsAndTests.testGardenVisitors() }
        registerCommand("shtestcomposter", "dev command") { ComposterOverlay.onCommand(it) }
        registerCommand("shtestinquisitor", "dev command") { InquisitorWaypointShare.test() }
        registerCommand("shshowcropmoneycalculation", "dev command") { CropMoneyDisplay.toggleShowCalculation() }
        registerCommand("shcropspeedmeter", "Debugs how many crops you collect over time") { CropSpeedMeter.toggle() }
        registerCommand(
            "shconfigsave",
            "Manually saving the config"
        ) { SkyHanniMod.configManager.saveConfig("manual-command") }
    }

    private fun developersCodingHelp() {
        registerCommand("shtest", "Unused test command.") { SkyHanniDebugsAndTests.testCommand(it) }
        registerCommand("shreloadlocalrepo", "Reloading the local repo data") { SkyHanniMod.repo.reloadLocalRepo() }
        registerCommand("shchathistory", "Show the unfiltered chat history") { ChatManager.openChatFilterGUI() }
        registerCommand(
            "shstoplisteners",
            "Unregistering all loaded forge event listeners"
        ) { SkyHanniDebugsAndTests.stopListeners() }
        registerCommand(
            "shreloadlisteners",
            "Trying to load all forge event listeners again. Might not work at all"
        ) { SkyHanniDebugsAndTests.reloadListeners() }
        registerCommand(
            "shcopylocation",
            "Copies the player location as LorenzVec format to the clipboard"
        ) { SkyHanniDebugsAndTests.copyLocation(it) }
        registerCommand(
            "shcopyentities",
            "Copies entities in the specified radius around the player to the clipboard"
        ) { CopyNearbyEntitiesCommand.command(it) }
        registerCommand("shcopytablist", "Copies the tab list data to the clipboard") { CopyTabListCommand.command(it) }
        registerCommand(
            "shcopyscoreboard",
            "Copies the scoreboard data to the clipboard"
        ) { CopyScoreboardCommand.command(it) }
        registerCommand(
            "shcopyitem",
            "Copies information about the item in hand to the clipboard"
        ) { CopyItemCommand.command() }
        registerCommand(
            "shcopyparticles",
            "Copied information about the particles that spawn in the next 50ms to the clipboard"
        ) { CopyNearbyParticlesCommand.command(it) }
        registerCommand("shtestpacket", "Logs incoming and outgoing packets to the console") { PacketTest.toggle() }
        registerCommand(
            "shtestmessage",
            "Sends a custom chat message client side in the chat"
        ) { TestChatCommand.command(it) }
        registerCommand(
            "shcopyinternalname",
            "Copies the internal name of the item in hand to the clipboard."
        ) { SkyHanniDebugsAndTests.copyItemInternalName() }
        registerCommand(
            "shpartydebug",
            "List persons into the chat SkyHanni thinks are in your party."
        ) { PartyAPI.listMembers() }
        registerCommand(
                "shplaysound",
                "Play the specified sound effect at the given pitch and volume."
        ) { SoundUtils.command(it) }
    }

    private fun internalCommands() {
        registerCommand("shshareinquis", "") { InquisitorWaypointShare.sendInquisitor() }
        registerCommand("shcopyerror", "") { ErrorManager.command(it) }
        registerCommand("shstopcityprojectreminder", "") { CityProjectFeatures.disable() }
        registerCommand("shsendcontests", "") { GardenNextJacobContest.shareContestConfirmed(it) }
        registerCommand("shstopaccountupgradereminder", "") { AccountUpgradeReminder.disable() }
//        registerCommand(
//            "shsendtranslation",
//            "Respond with a translation of the message that the user clicks"
//        ) { Translator.toEnglish(it) }
        registerCommand("shwords", "Opens the config list for modifying visual words") { openVisualWords() }
    }

    private fun shortenedCommands() {
        registerCommand("pko", "Kicks offline party members") { PartyCommands.kickOffline() }
        registerCommand("pw", "Warps your party") { PartyCommands.warp() }
        registerCommand("pk", "Kick a specific party member") { PartyCommands.kick(it) }
        registerCommand("pt", "Transfer the party to another party member") { PartyCommands.transfer(it) }
        registerCommand("pp", "Promote a specific party member") { PartyCommands.promote(it) }
    }

    private fun commandHelp(args: Array<String>) {
        var filter: (String) -> Boolean = { true }
        val title: String
        if (args.size == 1) {
            val searchTerm = args[0].lowercase()
            filter = { it.lowercase().contains(searchTerm) }
            title = "SkyHanni commands with '§e$searchTerm§7'"
        } else {
            title = "All SkyHanni commands"
        }
        val base = ChatComponentText(" \n§7$title:\n")
        for (command in commands) {
            if (!filter(command.name) && !filter(command.description)) continue
            val category = command.category
            val name = command.name
            val color = category.color
            val text = ChatComponentText("$color/$name")
            text.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/$name")

            val hoverText = buildList {
                add("§e/$name")
                add(" §7${command.description}")
                add("")
                add("$color${category.categoryName}")
                add("  §7${category.description}")
            }

            text.chatStyle.chatHoverEvent =
                HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText.joinToString("\n")))
            base.appendSibling(text)
            base.appendSibling(ChatComponentText("§7, "))
        }
        base.appendSibling(ChatComponentText("\n "))
        Minecraft.getMinecraft().thePlayer.addChatMessage(base)
    }

    @JvmStatic
    fun openFortuneGuide() {
        if (!LorenzUtils.inSkyBlock) {
            LorenzUtils.chat("§cJoin SkyBlock to open the fortune guide!")
        } else {
            CaptureFarmingGear.captureFarmingGear()
            SkyHanniMod.screenToOpen = FFGuideGUI()
        }
    }

    @JvmStatic
    fun openVisualWords() {
        if (!LorenzUtils.onHypixel) {
            LorenzUtils.chat("§cYou need to join Hypixel to use this feature!")
        } else {
            SkyHanniMod.screenToOpen = VisualWordGui()
        }
    }

    private fun clearFarmingItems() {
        val config = GardenAPI.config?.fortune ?: return
        LorenzUtils.chat("§e[SkyHanni] clearing farming items")
        config.farmingItems.clear()
        config.outdatedItems.clear()
    }

    private fun registerCommand(
        name: String,
        description: String,
        function: (Array<String>) -> Unit
    ) = registerCommand0(name, description, function)

    private fun registerCommand0(
        name: String,
        description: String,
        function: (Array<String>) -> Unit,
        autoComplete: ((Array<String>) -> List<String>) = { listOf() }
    ) {
        ClientCommandHandler.instance.registerCommand(
            SimpleCommand(
                name,
                createCommand(function)
            ) { _, b, _ -> autoComplete(b) }
        )
        commands.add(CommandInfo(name, description, currentCategory))
    }

    private fun createCommand(function: (Array<String>) -> Unit) =
        object : ProcessCommandRunnable() {
            override fun processCommand(sender: ICommandSender?, args: Array<out String>) {
                function(args.asList().toTypedArray())
            }
        }
}