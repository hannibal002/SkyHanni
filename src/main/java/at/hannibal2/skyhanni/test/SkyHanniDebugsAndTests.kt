package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvents
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorColorNames
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarData
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getRawCraftCostOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.isAuctionHouseItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getRawBaseStats
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.ReflectionUtils.makeAccessible
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.addLine
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.nbt.NBTTagCompound
//#if FORGE
import net.minecraftforge.common.MinecraftForge
//#endif
import java.io.File
import java.time.LocalDate
import java.time.Month
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkyHanniDebugsAndTests {

    private val config get() = SkyHanniMod.feature.dev
    private val debugConfig get() = config.debug
    var displayLine = ""

    @Suppress("MemberVisibilityCanBePrivate")
    var displayList = emptyList<Renderable>()

    var globalRender = true

    var a = 1.0
    var b = 60.0
    var c = 0.0

    val debugLogger = LorenzLogger("debug/test")

    private fun run(compound: NBTTagCompound, text: String) {
        print("$text'$compound'")
        for (s in compound.keySet) {
            val element = compound.getCompoundTag(s)
            run(element, "$text  ")
        }
    }

    private fun print(text: String) {
        LorenzDebug.log(text)
    }

    private var previousApril = false

    val isAprilFoolsDay: Boolean
        get() {
            val itsTime = LocalDate.now().let { it.month == Month.APRIL && it.dayOfMonth == 1 }
            val always = SkyHanniMod.feature.dev.debug.alwaysFunnyTime
            val never = SkyHanniMod.feature.dev.debug.neverFunnyTime
            val result = (!never && (always || itsTime))
            previousApril = result
            return result
        }

    private var testLocation: LorenzVec? = null

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        testLocation?.let {
            event.drawWaypointFilled(it, LorenzColor.WHITE.toColor())
            event.drawDynamicText(it, "Test", 1.5)
        }
    }

    private fun waypoint(args: Array<String>) {
        SoundUtils.playBeepSound()

        if (args.isEmpty()) {
            testLocation = null
            ChatUtils.chat("reset test waypoint")
            IslandGraphs.stop()
            return
        }

        val x = args[0].toDouble()
        val y = args[1].toDouble()
        val z = args[2].toDouble()
        val location = LorenzVec(x, y, z)
        testLocation = location
        if (args.getOrNull(3) == "pathfind") {
            IslandGraphs.pathFind(location, "/shtestwaypoint", condition = { true })
        }
        ChatUtils.chat("set test waypoint")
    }

    private fun testCommand(args: Array<String>) {
        SkyHanniMod.launchCoroutine {
            asyncTest(args)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun asyncTest(args: Array<String>) {
        ChatUtils.chat("§fTest successful!")
    }

    private fun findNull(obj: Any, path: String) {
        val blockedNames = listOf(
            "TRUE",
            "FALSE",
            "SIZE",
            "MIN_VALUE",
            "MAX_VALUE",
            "BYTES",
            "POSITIVE_INFINITY",
            "NEGATIVE_INFINITY",
            "NaN",
            "MIN_NORMAL",
        )

        val javaClass = obj.javaClass
        if (javaClass.isEnum) return
        for (field in javaClass.fields) {
            val name = field.name
            if (name in blockedNames) continue

            // funny thing
            if (obj is Position) {
                if (name == "internalName") continue
            }

            val other = field.makeAccessible().get(obj)
            val newName = "$path.$name"
            if (other == null) {
                println("config null at $newName")
            } else {
                findNull(other, newName)
            }
        }
    }

    private fun resetConfig() {
        // saving old config state
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "reload config manager")
        SkyHanniMod.configManager.saveConfig(ConfigFileType.SACKS, "reload config manager")
        SkyHanniMod.configManager.saveConfig(ConfigFileType.PETS, "reload config manager")
        Thread {
            Thread.sleep(500)
            SkyHanniMod.configManager.disableSaving()

            // initializing a new config manager, calling firstLoad, and setting it as the config manager in use.
            val configManager = ConfigManager()
            configManager.firstLoad()
            SkyHanniMod::class.java.enclosingClass.getDeclaredField("configManager").makeAccessible()
                .set(SkyHanniMod, configManager)

            // resetting the MoulConfigProcessor in use
            ConfigGuiManager.editor = null
            ChatUtils.chat("Reset the config manager!")
        }.start()
    }

    private fun testGardenVisitors() {
        if (displayList.isNotEmpty()) {
            displayList = mutableListOf()
            return
        }

        var errors = 0

        displayList = buildList {
            for (item in GardenVisitorColorNames.visitorItems) {
                val name = item.key

                addLine {
                    val coloredName = GardenVisitorColorNames.getColoredName(name)
                    addString("$coloredName§7 (")

                    for (itemName in item.value) {
                        try {
                            val internalName = NeuInternalName.fromItemName(itemName)
                            addItemStack(internalName.getItemStack())
                        } catch (e: Error) {
                            ChatUtils.debug("itemName '$itemName' is invalid for visitor '$name'")
                            errors++
                        }
                    }
                    if (item.value.isEmpty()) {
                        addString("Any")
                    }
                    addString("§7) ")
                }
            }
        }

        if (errors == 0) {
            ChatUtils.debug("Test garden visitor renderer: no errors")
        } else {
            ChatUtils.debug("Test garden visitor renderer: $errors errors")
        }
    }

    private fun reloadListeners() {
        // TODO: use repo for this and implement it correctly
        val blockedFeatures = try {
            File("config/skyhanni/blocked-features.txt").readLines().toList()
        } catch (e: Exception) {
            emptyList()
        }

        val modules = SkyHanniMod.modules
        for (original in modules.toMutableList()) {
            val javaClass = original.javaClass
            val simpleName = javaClass.simpleName
            //#if FORGE
            MinecraftForge.EVENT_BUS.unregister(original)
            //#endif
            SkyHanniEvents.unregister(original)
            println("Unregistered listener $simpleName")

            if (simpleName !in blockedFeatures) {
                modules.remove(original)
                modules.add(original)
                //#if FORGE
                MinecraftForge.EVENT_BUS.register(original)
                //#endif
                SkyHanniEvents.register(original)
                println("Registered listener $simpleName")
            } else {
                println("Skipped registering listener $simpleName")
            }
        }
        ChatUtils.chat("Reloaded ${modules.size} listener classes.")
    }

    private fun stopListeners() {
        ChatUtils.clickableChat(
            "§cAre you sure you want to stop all listeners? Doing this will make most features not work.",
            onClick = {
                val modules = SkyHanniMod.modules
                for (original in modules.toMutableList()) {
                    val javaClass = original.javaClass
                    val simpleName = javaClass.simpleName
                    //#if FORGE
                    MinecraftForge.EVENT_BUS.unregister(original)
                    //#endif
                    SkyHanniEvents.unregister(original)
                    println("Unregistered listener $simpleName")
                }
                ChatUtils.clickableChat(
                    "Stopped ${modules.size} listener classes. " +
                        "If you want to re-enable them, run /shreloadlisteners or click this message.",
                    onClick = { reloadListeners() },
                )
            },
        )
    }

    private var lastManualContestDataUpdate = SimpleTimeMark.farPast()

    private fun resetContestData() {
        if (lastManualContestDataUpdate.passedSince() < 30.seconds) {
            ChatUtils.userError("§cYou already reset Jacob's Contest data recently!")
            return
        }
        lastManualContestDataUpdate = SimpleTimeMark.now()

        GardenNextJacobContest.resetContestData()
    }

    private fun copyLocation(args: Array<String>) {
        val location = LocationUtils.playerLocation()
        val x = (location.x + 0.001).roundTo(1)
        val y = (location.y + 0.001).roundTo(1)
        val z = (location.z + 0.001).roundTo(1)
        val (clipboard, format) = formatLocation(x, y, z, args.getOrNull(0))
        OSUtils.copyToClipboard(clipboard)
        ChatUtils.chat("Copied the current location to clipboard ($format format)!", replaceSameMessage = true)
    }

    private fun formatLocation(x: Double, y: Double, z: Double, parameter: String?): Pair<String, String> = when (parameter) {
        "json" -> "$x:$y:$z" to "json"
        "pathfind" -> "`/shtestwaypoint $x $y $z pathfind`" to "pathfind"
        else -> "LorenzVec($x, $y, $z)" to "LorenzVec"
    }

    @HandleEvent(GuiKeyPressEvent::class)
    fun onKeybind() {
        if (!debugConfig.copyInternalName.isKeyHeld()) return
        val stack = stackUnderCursor() ?: return
        val internalName = stack.getInternalNameOrNull() ?: return
        val rawInternalName = internalName.asString()
        OSUtils.copyToClipboard(rawInternalName)
        ChatUtils.chat("§eCopied internal name §7$rawInternalName §eto the clipboard!")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onShowInternalName(event: ToolTipEvent) {
        if (!debugConfig.showInternalName) return
        val itemStack = event.itemStack
        val internalName = itemStack.getInternalName()
        if ((internalName == NeuInternalName.NONE) && !debugConfig.showEmptyNames) return
        event.toolTip.add("Internal Name: '${internalName.asString()}'")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun showItemRarity(event: ToolTipEvent) {
        if (!debugConfig.showItemRarity) return
        val itemStack = event.itemStack

        val rarity = itemStack.getItemRarityOrNull()
        event.toolTip.add("Item rarity: $rarity")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun showItemCategory(event: ToolTipEvent) {
        if (!debugConfig.showItemCategory) return
        val itemStack = event.itemStack

        val category = itemStack.getItemCategoryOrNull()?.name ?: "UNCLASSIFIED"
        event.toolTip.add("Item category: $category")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onShowNpcPrice(event: ToolTipEvent) {
        if (!debugConfig.showNpcPrice) return
        val internalName = event.itemStack.getInternalNameOrNull() ?: return

        val npcPrice = internalName.getNpcPriceOrNull() ?: return
        event.toolTip.add("§7NPC price: ${npcPrice.addSeparators()}")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onShowBaseStats(event: ToolTipEvent) {
        if (!debugConfig.showBaseValues) return
        val internalName = event.itemStack.getInternalNameOrNull() ?: return

        val stats = internalName.getRawBaseStats()
        if (stats.isEmpty()) return

        event.toolTip.add("§7Base stats:")
        for ((name, value) in stats) {

            event.toolTip.add("§7$name: $value")
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onShowCraftPrice(event: ToolTipEvent) {
        if (!debugConfig.showCraftPrice) return
        val price = event.itemStack.getInternalNameOrNull()?.getRawCraftCostOrNull() ?: return

        event.toolTip.add("§7Craft price: ${price.addSeparators()}")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onShowBzPrice(event: ToolTipEvent) {
        if (!debugConfig.showBZPrice) return
        val internalName = event.itemStack.getInternalNameOrNull() ?: return

        val data = internalName.getBazaarData() ?: return
        val instantSellPrice = data.instantSellPrice
        val instantBuyPrice = data.instantBuyPrice

        event.toolTip.add("§7BZ instantSellPrice: ${instantSellPrice.addSeparators()}")
        event.toolTip.add("§7BZ instantBuyPrice: ${instantBuyPrice.addSeparators()}")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onShowBinPrice(event: ToolTipEvent) {
        if (!debugConfig.showBinPrice) return
        val internalName = event.itemStack.getInternalNameOrNull() ?: return
        if (!internalName.isAuctionHouseItem()) return

        val binPrice = internalName.getPrice()

        event.toolTip.add("§7Bin Price: ${binPrice.addSeparators()}")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onShowItemName(event: ToolTipEvent) {
        if (!debugConfig.showItemName) return
        val itemStack = event.itemStack
        val internalName = itemStack.getInternalName()
        if (internalName == NeuInternalName.NONE) {
            event.toolTip.add("Item name: no item.")
            return
        }
        val name = itemStack.repoItemName
        event.toolTip.add("Item name: '$name§7'")
    }

    @HandleEvent(SkyHanniChatEvent::class)
    @Suppress("EmptyFunctionBlock")
    fun onChat() {
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onRenderOverlay() {
        if (MinecraftCompat.showDebugHud) {
            if (debugConfig.currentAreaDebug) {
                val renderables = buildList {
                    addString("Current Area: ${HypixelData.skyBlockArea}")
                    addString("Graph Area: ${SkyBlockUtils.graphArea}")
                }

                config.debugLocationPos.renderRenderables(renderables, posLabel = "SkyBlock Area (Debug)")
            }

            if (debugConfig.raytracedOreblock) {
                BlockUtils.getTargetedBlockAtDistance(50.0)?.let { pos ->
                    OreBlock.getByStateOrNull(pos.getBlockStateAt())?.let { ore ->
                        config.debugOrePos.renderString(
                            "Looking at: ${ore.name} (${pos.toCleanString()})",
                            posLabel = "OreBlock",
                        )
                    }
                }
            }
        }


        if (!debugConfig.enabled) return

        if (displayLine.isNotEmpty()) {
            config.debugPos.renderString("test: $displayLine", posLabel = "Test")
        }
        config.debugPos.renderRenderables(displayList, posLabel = "Test Display")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onOreMined(event: OreMinedEvent) {
        if (!debugConfig.oreEventMessages) return
        val originalOre = event.originalOre?.let { "$it " }.orEmpty()
        val extraBlocks = event.extraBlocks.map { "${it.key.name}: ${it.value}" }
        ChatUtils.debug("Mined: $originalOre(${extraBlocks.joinToString()})")
    }

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
//        val particleType = event.type
//        val distance = LocationUtils.playerLocation().distance(event.location).roundTo(2)
//
//        println("")
//        println("particleType: $particleType")
//
//        val particleCount = event.count
//
//        println("distance: $distance")
//
//        val particleArgs = event.particleArgs
//        println("args: " + particleArgs.size)
//        for ((i, particleArg) in particleArgs.withIndex()) {
//            println("$i $particleArg")
//        }
//
//        val particleSpeed = event.speed
//        val offset = event.offset
//        println("particleCount: $particleCount")
//        println("particleSpeed: $particleSpeed")
//        println("offset: $offset")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.debugEnabled", "dev.debug.enabled")
        event.move(3, "dev.showInternalName", "dev.debug.showInternalName")
        event.move(3, "dev.showEmptyNames", "dev.debug.showEmptyNames")
        event.move(3, "dev.showItemRarity", "dev.debug.showItemRarity")
        event.move(3, "dev.copyInternalName", "dev.debug.copyInternalName")
        event.move(3, "dev.showNpcPrice", "dev.debug.showNpcPrice")
    }

    @Suppress("LongMethod")
    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetconfig") {
            description = "Reloads the config manager and rendering processors of MoulConfig. " +
                "This §cWILL RESET §7your config, but also update the config files " +
                "(names, description, orderings and stuff)."
            category = CommandCategory.DEVELOPER_TEST
            callback {
                ChatUtils.clickableChat(
                    "§cTHIS WILL RESET YOUR SkyHanni CONFIG! Click here to proceed.",
                    onClick = { resetConfig() },
                    "§eClick to confirm.",
                    prefix = false,
                    oneTimeClick = true,
                )
            }
        }
        event.registerBrigadier("shversion") {
            description = "Prints the SkyHanni version in the chat"
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                val name1 = "SkyHanni ${SkyHanniMod.VERSION} on Minecraft ${PlatformUtils.MC_VERSION}"
                ChatUtils.chat("§eYou are using $name1")
                OSUtils.copyToClipboard(name1)
            }
        }
        event.registerBrigadier("shtestgardenvisitors") {
            description = "Test the garden visitor drop statistics"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { testGardenVisitors() }
        }
        event.registerBrigadier("shcopyinternalname") {
            description = "Copies the internal name of the item in hand to the clipboard."
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                val hand = InventoryUtils.getItemInHand()
                if (hand == null) {
                    ChatUtils.userError("No item in hand!")
                } else {
                    val internalName = hand.getInternalName().asString()
                    OSUtils.copyToClipboard(internalName)
                    ChatUtils.chat("§eCopied internal name §7$internalName §eto the clipboard!")
                }
            }
        }
        event.registerBrigadier("shcopylocation") {
            description = "Copies the player location as LorenzVec format to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            legacyCallbackArgs { copyLocation(it) }
        }
        event.registerBrigadier("shtest") {
            description = "Unused test command."
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs { testCommand(it) }
        }
        event.registerBrigadier("shfindnullconfig") {
            description = "Find config elements that are null and prints them into the console"
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs {
                println("start null finder")
                findNull(SkyHanniMod.feature, "config")
                println("stop null finder")
            }
        }
        event.registerBrigadier("shtestwaypoint") {
            description = "Set a waypoint on that location"
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs { waypoint(it) }
        }
        event.registerBrigadier("shstoplisteners") {
            description = "Unregistering all loaded event listeners"
            category = CommandCategory.DEVELOPER_TEST
            callback { stopListeners() }
        }
        event.registerBrigadier("shreloadlisteners") {
            description = "Reloads all event listeners again"
            category = CommandCategory.DEVELOPER_TEST
            callback { reloadListeners() }
        }
        event.registerBrigadier("shresetcontestdata") {
            description = "Resets Jacob's Contest Data"
            category = CommandCategory.USERS_RESET
            callback { resetContestData() }
        }
        event.registerBrigadier("shwhereami") {
            description = "Print current island in chat"
            category = CommandCategory.USERS_BUG_FIX
            callback {
                if (SkyBlockUtils.inSkyBlock) {
                    ChatUtils.chat("§eYou are currently in ${SkyBlockUtils.currentIsland}.")
                } else {
                    ChatUtils.chat("§eYou are not in Skyblock.")
                }
            }
        }
        event.registerBrigadier("shrendertoggle") {
            description = "Disables/enables the rendering of all skyhanni guis."
            category = CommandCategory.USERS_BUG_FIX
            callback {
                globalRender = !globalRender
                if (globalRender) {
                    ChatUtils.chat("§aEnabled global renderer!")
                } else {
                    ChatUtils.chat("§cDisabled global renderer! Run this command again to show SkyHanni rendering again.")
                }
            }
        }
    }
}
