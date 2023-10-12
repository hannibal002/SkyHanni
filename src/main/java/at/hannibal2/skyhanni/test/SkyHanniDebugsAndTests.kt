package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorColorNames
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.File

class SkyHanniDebugsAndTests {

    companion object {
        private val config get() = SkyHanniMod.feature.dev
        var displayLine = ""
        var displayList = emptyList<List<Any>>()

        var globalRenderToggle = true

        var a = 1.0
        var b = 60.0
        var c = 0.0

        val debugLogger = LorenzLogger("debug/test")

        fun runn(compound: NBTTagCompound, text: String) {
            print("$text'$compound'")
            for (s in compound.keySet) {
                val element = compound.getCompoundTag(s)
                runn(element, "$text  ")
            }
        }

        private fun print(text: String) {
            LorenzDebug.log(text)
        }

        fun testCommand(args: Array<String>) {
            SoundUtils.playBeepSound()

//            val a = Thread { OSUtils.copyToClipboard("123") }
//            val b = Thread { OSUtils.copyToClipboard("456") }
//            a.start()
//            b.start()


//            for ((i, s) in ScoreboardData.siedebarLinesFormatted().withIndex()) {
//                println("$i: '$s'")
//            }

//            val name = args[0]
//            val pitch = args[1].toFloat()
//            val sound = SoundUtils.createSound("note.harp", 1.35f)
//            val sound = SoundUtils.createSound("random.orb", 11.2f)
//            SoundUtils.createSound(name, pitch).playSound()


//            a = args[0].toDouble()
//            b = args[1].toDouble()
//            c = args[2].toDouble()

//            for (line in (Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay).footer.unformattedText
//                .split("\n")) {
//                println("footer: '$line'")
//            }
//
//
//            for (line in TabListUtils.getTabList()) {
//                println("tablist: '$line'")
//            }
        }

        fun testGardenVisitors() {
            if (displayList.isNotEmpty()) {
                displayList = mutableListOf()
                return
            }

            val bigList = mutableListOf<List<Any>>()
            var list = mutableListOf<Any>()
            var i = 0
            var errors = 0
            for (item in GardenVisitorColorNames.visitorItems) {
                val name = item.key
                i++
                if (i == 5) {
                    i = 0
                    bigList.add(list)
                    list = mutableListOf()
                }

                val coloredName = GardenVisitorColorNames.getColoredName(name)
                list.add("$coloredName§7 (")
                for (itemName in item.value) {
                    try {
                        val internalName = NEUItems.getRawInternalName(itemName)
                        list.add(NEUItems.getItemStack(internalName))
                    } catch (e: Error) {
                        LorenzUtils.debug("itemName '$itemName' is invalid for visitor '$name'")
                        errors++
                    }
                }
                if (item.value.isEmpty()) {
                    list.add("Any")
                }
                list.add("§7) ")
            }
            bigList.add(list)
            displayList = bigList
            if (errors == 0) {
                LorenzUtils.debug("Test garden visitor renderer: no errors")
            } else {
                LorenzUtils.debug("Test garden visitor renderer: $errors errors")
            }
        }

        fun reloadListeners() {
            val blockedFeatures = try {
                File("config/skyhanni/blocked-features.txt").readLines().toList()
            } catch (e: Exception) {
                emptyList()
            }

            val modules = SkyHanniMod.modules
            for (original in modules.toMutableList()) {
                val javaClass = original.javaClass
                val simpleName = javaClass.simpleName
                MinecraftForge.EVENT_BUS.unregister(original)
                println("Unregistered listener $simpleName")

                if (simpleName !in blockedFeatures) {
                    modules.remove(original)
                    val module = javaClass.newInstance()
                    modules.add(module)

                    MinecraftForge.EVENT_BUS.register(module)
                    println("Registered listener $simpleName")
                } else {
                    println("Skipped registering listener $simpleName")
                }
            }
            LorenzUtils.chat("§e[SkyHanni] reloaded ${modules.size} listener classes.")
        }

        fun stopListeners() {
            val modules = SkyHanniMod.modules
            for (original in modules.toMutableList()) {
                val javaClass = original.javaClass
                val simpleName = javaClass.simpleName
                MinecraftForge.EVENT_BUS.unregister(original)
                println("Unregistered listener $simpleName")
            }
            LorenzUtils.chat("§e[SkyHanni] stopped ${modules.size} listener classes.")
        }

        fun copyLocation(args: Array<String>) {
            val location = LocationUtils.playerLocation()
            val x = LorenzUtils.formatDouble(location.x + 0.001).replace(",", ".")
            val y = LorenzUtils.formatDouble(location.y + 0.001).replace(",", ".")
            val z = LorenzUtils.formatDouble(location.z + 0.001).replace(",", ".")
            if (args.size == 1 && args[0].equals("json", false)) {
                OSUtils.copyToClipboard("\"$x:$y:$z\"")
                return
            }

            OSUtils.copyToClipboard("LorenzVec($x, $y, $z)")
        }

        fun debugVersion() {
            LorenzUtils.chat("§eYou are using SkyHanni ${SkyHanniMod.version}")
        }

        fun debugData(args: Array<String>) {
            if (args.size == 2 && args[0] == "profileName") {
                HypixelData.profileName = args[1].lowercase()
                LorenzUtils.chat("§eManually set profileName to '${HypixelData.profileName}'")
                return
            }
            val builder = StringBuilder()
            builder.append("```\n")
            builder.append("= Debug Information = \n")
            builder.append("\n")
            builder.append("SkyHanni ${SkyHanniMod.version}\n")
            builder.append("\n")
            builder.append("player name: '${LorenzUtils.getPlayerName()}'\n")
            builder.append("player uuid: '${LorenzUtils.getPlayerUuid()}'\n")
            builder.append("repoAutoUpdate: ${config.repoAutoUpdate}\n")
            builder.append("globalRenderToggle: ${globalRenderToggle}\n")
            builder.append("\n")

            builder.append("onHypixel: ${LorenzUtils.onHypixel}\n")
            val inSkyBlock = LorenzUtils.inSkyBlock
            builder.append("inSkyBlock: $inSkyBlock\n")

            if (inSkyBlock) {
                builder.append("\n")
                builder.append("skyBlockIsland: ${LorenzUtils.skyBlockIsland}\n")
                builder.append("skyBlockArea: '${LorenzUtils.skyBlockArea}'\n")
                builder.append("profileName: '${HypixelData.profileName}'\n")
                builder.append("\n")
                builder.append("ironman: ${HypixelData.ironman}\n")
                builder.append("stranded: ${HypixelData.stranded}\n")
                builder.append("bingo: ${HypixelData.bingo}\n")

                if (LorenzUtils.inDungeons) {
                    builder.append("\n")
                    builder.append("In dungeon!\n")
                    builder.append(" dungeonFloor: ${DungeonAPI.dungeonFloor}\n")
                    builder.append(" started: ${DungeonAPI.started}\n")
                    builder.append(" inBossRoom: ${DungeonAPI.inBossRoom}\n")
                }
                if (SlayerAPI.hasActiveSlayerQuest()) {
                    builder.append("\n")
                    builder.append("Doing slayer!\n")
                    builder.append(" activeSlayer: ${SlayerAPI.getActiveSlayer()}\n")
                    builder.append(" isInCorrectArea: ${SlayerAPI.isInCorrectArea}\n")
                }

            }
            builder.append("```")
            OSUtils.copyToClipboard(builder.toString())
            LorenzUtils.chat("§eCopied SkyHanni debug data to clipboard.")
        }

        fun copyItemInternalName() {
            val hand = InventoryUtils.getItemInHand()
            if (hand == null) {
                LorenzUtils.chat("§cNo item in hand!")
                return
            }

            val internalName = hand.getInternalNameOrNull()
            if (internalName == null) {
                LorenzUtils.chat("§cInternal name is null for item ${hand.name}")
                return
            }

            val rawInternalName = internalName.asString()
            OSUtils.copyToClipboard(rawInternalName)
            LorenzUtils.chat("§eCopied internal name §7$rawInternalName §eto the clipboard!")
        }

        fun toggleRender() {
            globalRenderToggle = !globalRenderToggle
            if (globalRenderToggle) {
                LorenzUtils.chat("§e[SkyHanni] §cEnabled global render toggle! Run this command again to show SkyHanni elements again.")
            } else {
                LorenzUtils.chat("§e[SkyHanni] §aDisabled global render toggle!")
            }
        }
    }

    @SubscribeEvent
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
        if (!SkyHanniMod.feature.dev.copyInternalName.isKeyHeld()) return
        val gui = event.gui as? GuiContainer ?: return
        val focussedSlot = gui.slotUnderMouse ?: return
        val stack = focussedSlot.stack ?: return
        val internalName = stack.getInternalNameOrNull() ?: return
        val rawInternalName = internalName.asString()
        OSUtils.copyToClipboard(rawInternalName)
        LorenzUtils.chat("§eCopied internal name §7$rawInternalName §eto the clipboard!")
    }

    @SubscribeEvent
    fun onShowInternalName(event: ItemTooltipEvent) {
        if (!config.showInternalName) return
        val itemStack = event.itemStack ?: return
        val internalName = itemStack.getInternalName()
        if ((internalName == NEUInternalName.NONE) && !config.showEmptyNames) return
        event.toolTip.add("Internal Name: '${internalName.asString()}'")

    }

    @SubscribeEvent
    fun showItemRarity(event: ItemTooltipEvent) {
        if (!config.showItemRarity) return
        val itemStack = event.itemStack ?: return

        val rarity = itemStack.getItemRarityOrNull(logError = false)
        event.toolTip.add("Item rarity: $rarity")
    }

    @SubscribeEvent
    fun onSHowNpcPrice(event: ItemTooltipEvent) {
        if (!config.showNpcPrice) return
        val itemStack = event.itemStack ?: return
        val internalName = itemStack.getInternalNameOrNull() ?: return

        val npcPrice = internalName.getNpcPriceOrNull() ?: return
        event.toolTip.add("§7Npc price: §6${npcPrice.addSeparators()}")
    }

    @SubscribeEvent
    fun onRenderLocation(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (LorenzUtils.inSkyBlock && Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            config.debugLocationPos.renderString(
                "Current Area: ${HypixelData.skyBlockArea}",
                posLabel = "SkyBlock Area (Debug)"
            )
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {

    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.debugEnabled) return

        if (displayLine.isNotEmpty()) {
            config.debugPos.renderString("test: $displayLine", posLabel = "Test")
        }
        config.debugPos.renderStringsAndItems(displayList, posLabel = "Test Display")
    }

    @SubscribeEvent
    fun onSoundPlay(event: PlaySoundEvent) {
//        val location = event.location
//        val distance = location.distanceToPlayer()
//        val soundName = event.soundName
//        val pitch = event.pitch
//        val volume = event.volume

        //background music
//        if (soundName == "note.harp") {
////                if (distance < 2) {
//
//
//            //Wilderness
//            val list = mutableListOf<Float>()
////                list.add(0.4920635)
////                list.add(0.74603176)
////                list.add(0.8888889)
////                list.add(1.1746032)
////                list.add(1.7777778)
////                list.add(0.5873016)
////                list.add(1f)
////                list.add(1.4920635)
////                list.add(0.4920635)
////                list.add(1.8730159)
////                list.add(0.82539684)
////                list.add(1.1111112)
////                list.add(1.6666666)
////                list.add(0.5555556)
////                list.add(0.6984127)
////                list.add(0.93650794)
////                list.add(1.4126984)
////                list.add(1.3333334)
////                list.add(1.5873016)
//
//            if (pitch in list) {
//                if (Minecraft.getMinecraft().thePlayer.isSneaking) {
//                    event.isCanceled = true
//                }
//                return
//            }
//        }


        //diana ancestral spade
//        if (soundName == "note.harp") {
//            val list = mutableListOf<Float>()
//            list.add(0.52380955f)
//            list.add(0.5555556f)
//            list.add(0.6031746f)
//            list.add(0.63492066f)
//            list.add(0.6825397f)
//            list.add(0.71428573f)
//            list.add(0.7619048f)
//            list.add(0.7936508f)
//            list.add(0.84126985f)
//            list.add(0.8888889f)
//            list.add(0.9206349f)
//            list.add(0.96825397f)
//            list.add(1.476191f)
//            list.add(1.476191f)
//            list.add(0.50793654f)
//            list.add(0.6507937f)
//            list.add(0.6984127f)
//            list.add(0.74603176f)
//            list.add(0.93650794f)
//            list.add(0.984127f)
//            list.add(1.968254f)
//            list.add(0.4920635f)
//            list.add(1.1587307f)
//            list.add(1.1587301f)
//            list.add(1.2857143f)
//            list.add(1.4126984f)
//            list.add(1.6825397f)
//            list.add(1.8095238f)
//            list.add(1.9365079f)
//            list.add(1.4920635f)
//            list.add(1.5396825f)
//            list.add(0.8730159f)
//            list.add(1.2539682f)
//            list.add(1.4285715f)
//            list.add(1.6190476f)
//            list.add(1.4920635f)
//            list.add(0.9047619f)
//            list.add(1.1111112f)
//            list.add(1.3174603f)
//            list.add(1.5238096f)
//            list.add(1.7301587f)
//
//            list.add(0.5873016f)
//            list.add(0.61904764f)
//            list.add(0.6666667f)
//            list.add(0.73015875f)
//            list.add(0.7777778f)
//            list.add(0.8095238f)
//            list.add(0.8095238f)
//            list.add(0.82539684f)
//
//            list.add(0.5714286f)
//            list.add(0.85714287f)
//            list.add(1.3174603f)
//            list.add(1.9523809f)
//            list.add(1.1428572f)
//            list.add(1.2063493f)
//            list.add(1.2698413f)
//            list.add(1.6349206f)
//            list.add(1.2380953f)
//            list.add(1.7936507f)
//            list.add(1.9841269f)
//            list.add(1.1746032f)
//            list.add(1.3492063f)
//            list.add(1.6984127f)
//            list.add(1.8571428f)
//
//            if (pitch in list) {
//                return
//            }
//        }

        //use ancestral spade
//        if (soundName == "mob.zombie.infect") {
//            if (pitch == 1.968254f) {
//                if (volume == 0.3f) {
//                    LorenzUtils.chat("used ancestral spade!")
//                    return
//                }
//            }
//        }

        //wither shield activated
//        if (soundName == "mob.zombie.remedy") {
//            if (pitch == 0.6984127f) {
//                if (volume == 1f) {
//                    return
//                }
//            }
//        }

        //wither shield cooldown over
//        if (soundName == "random.levelup") {
//            if (pitch == 3f) {
//                if (volume == 1f) {
//                    return
//                }
//            }
//        }

        //teleport (hyp or aote)
//        if (soundName == "mob.endermen.portal") {
//            if (pitch == 1f && volume == 1f) {
//                return
//            }
//        }

        //hyp wither impact
//        if (soundName == "random.explode") {
//            if (pitch == 1f && volume == 1f) {
//                return
//            }
//        }

        //pick coins up
//        if (soundName == "random.orb") {
//            if (pitch == 1.4920635f && volume == 1f) {
//                return
//            }
//        }


//        if (soundName == "game.player.hurt") return
//        if (soundName.startsWith("step.")) return

//        if (soundName != "mob.chicken.plop") return

//        println("")
//        println("PlaySoundEvent")
//        println("soundName: $soundName")
//        println("distance: $distance")
//        println("pitch: ${pitch}f")
//        println("volume: ${volume}f")
    }

    @SubscribeEvent
    fun onParticlePlay(event: ReceiveParticleEvent) {
//        val particleType = event.type
//        val distance = LocationUtils.playerLocation().distance(event.location).round(2)
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
}