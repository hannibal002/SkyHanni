package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dungeon.DragPrioConfig
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.test.DebugCommand
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DungeonDragonPriority {

    private val config get() = SkyHanniMod.feature.dungeon.dragPrio

    private val startPattern by RepoPattern.pattern(
        "dungeons.startphase5",
        "(.+)§r§a picked the §r§cCorrupted Blue Relic§r§a!"
    )

    enum class DragonInfo(
        val color: String,
        var hasSpawned: Boolean,
        val isEasy: Boolean,
        val priority: IntArray,
        val xRange: ClosedRange<Int>,
        val zRange: ClosedRange<Int>,
        val colorCode: Char
    ) {
        POWER("Red", false, false, intArrayOf(1, 3), 24..30, 56..62, LorenzColor.RED.chatColorCode),
        FLAME("Orange", false, true, intArrayOf(2, 1), 82..88, 56..62, LorenzColor.GOLD.chatColorCode),
        APEX("Green", false, true, intArrayOf(5, 2), 24..30, 91..97, LorenzColor.GREEN.chatColorCode),
        ICE("Blue", false, false, intArrayOf(3, 4), 82..88, 91..97, LorenzColor.AQUA.chatColorCode),
        SOUL("Purple", false, true, intArrayOf(4, 5), 53..59, 122..128, LorenzColor.LIGHT_PURPLE.chatColorCode),
        NONE("None", false, false, intArrayOf(0, 0), 0..0, 0..0, LorenzColor.CHROMA.chatColorCode);

        companion object {
            fun clearSpawned() {
                entries.forEach { it.hasSpawned = false }
            }

            fun getSpawnedAmount(): Int {
                var spawned = 0
                entries.forEach { if (it.hasSpawned) spawned += 1 }
                return spawned
            }
        }
    }

    private var inBerserkTeam = false
    private var inArcherTeam = false
    private var isHealer = false
    private var isTank = false

    private var isSearching = false
    private var titleString = ""

    private var dragonOrder = arrayOf(DragonInfo.NONE, DragonInfo.NONE)

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.enabled) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (!startPattern.matches(event.message)) return
        reset()
        ChatUtils.debug("starting p5")
        startP5()
    }

    private fun startP5() {
        if (!config.saySplit) return
        val currentClass = DungeonAPI.playerClass
        when (currentClass) {
            DungeonAPI.DungeonClass.MAGE -> inBerserkTeam = true
            DungeonAPI.DungeonClass.BERSERK -> inBerserkTeam = true
            DungeonAPI.DungeonClass.ARCHER -> inArcherTeam = true
            DungeonAPI.DungeonClass.TANK -> {
                inArcherTeam = true
                isTank = true
            }

            DungeonAPI.DungeonClass.HEALER -> isHealer = true
            else -> return
        }
        DelayedRun.runDelayed(2000.milliseconds) {
            val currentPower = getPower()
            when {
                currentPower >= config.splitPower -> ChatUtils.chat("Power: $currentPower | Split on all drags!")
                currentPower >= config.easyPower -> ChatUtils.chat("Power: $currentPower | Split on easy drags!")
                else -> ChatUtils.chat("Power: $currentPower | No split!")
            }
        }
        isSearching = true
    }

    private fun reset() {
        DragonInfo.clearSpawned()
        dragonOrder = arrayOf(DragonInfo.NONE, DragonInfo.NONE)
        inArcherTeam = false
        inBerserkTeam = false
        isHealer = false
        isTank = false
    }

    private fun checkCoordinates(particle: Packet<*>) {
        if (particle !is S2APacketParticles) return
        val vec = particle.toLorenzVec()
        val x = vec.x.toInt()
        val y = vec.y.toInt()
        val z = vec.z.toInt()
        if (y !in 15..22) return
        DragonInfo.entries.forEach {
            if (!it.hasSpawned && (x in it.xRange && z in it.zRange)) assignDragon(it)
        }
    }

    private fun assignDragon(dragon: DragonInfo) {
        ChatUtils.debug("try spawning ${dragon.name}")
        dragon.hasSpawned = true
        when (DragonInfo.NONE) {
            dragonOrder[0] -> {
                ChatUtils.debug("${dragon.name} is now dragon0")
                dragonOrder[0] = dragon
            }

            dragonOrder[1] -> {
                ChatUtils.debug("${dragon.name} is now dragon1")
                dragonOrder[1] = dragon
                determinePriority()
            }

            else -> {
                ChatUtils.debug("dragonOrder was full")
                if (config.showSingleDragons) sendTitle("§${dragon.colorCode}${dragon.color} is Spawning!")
            }
        }
    }

    private fun determinePriority() {
        val power = getPower()
        val isEasy: Boolean = dragonOrder[0].isEasy && dragonOrder[1].isEasy
        val split = if ((!isEasy && power >= config.splitPower) || (isEasy && power >= config.easyPower)) 1 else 0

        ChatUtils.debug("isEasy: $isEasy")
        ChatUtils.debug("split = $split")
        val (berserkDragon, archerDragon) = if (dragonOrder[0].priority[split] < dragonOrder[1].priority[split]) {
            dragonOrder[0] to dragonOrder[1]
        } else {
            dragonOrder[1] to dragonOrder[0]
        }
        displayDragons(berserkDragon, archerDragon, split)
    }

    private fun displayDragons(
        berserkDragon: DragonInfo,
        archerDragon: DragonInfo,
        split: Int
    ) {
        val purple = DragonInfo.SOUL in dragonOrder
        ChatUtils.debug("berserkDragon: ${berserkDragon.name} | archerDragon: ${archerDragon.name}")
        if (split == 1 && config.saySplit) {
            ChatUtils.chat("Berserk Team: ${berserkDragon.color} (send in pc)")
            ChatUtils.chat("Archer Team: ${archerDragon.color} (send in pc)")
        }
        if (DragonInfo.getSpawnedAmount() <= 2) {
            if (inBerserkTeam || (purple && (
                        (isHealer && config.healerPurple == DragPrioConfig.HealerPurpleValue.BERSERK)
                                || (isTank && config.tankPurple == DragPrioConfig.TankPurpleValue.BERSERK)))
            ) sendTitle("§${berserkDragon.colorCode}${berserkDragon.color.uppercase()} is Spawning!")
            else sendTitle("§${archerDragon.colorCode}${archerDragon.color.uppercase()} is Spawning!")
        } else {
            ChatUtils.chat("§cping me if you see this message")
            DebugCommand.command(arrayOf("Dragon Priority"))
        }
    }

    @SubscribeEvent
    fun onParticle(event: PacketEvent.ReceiveEvent) {
        if (!config.enabled) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (!isSearching) return
        if (event.packet !is S2APacketParticles) return
        if (event.packet.particleType != EnumParticleTypes.FLAME) return
        checkCoordinates(event.packet)
    }

    @SubscribeEvent
    fun onDungeonEnd(event: DungeonCompleteEvent) {
        if (!config.enabled) return
        if (!isSearching) return
        isSearching = false
    }

    @SubscribeEvent
    fun onDebugCollect(event: DebugDataCollectEvent) {
        event.title("Dragon Priority")
        if (!DungeonAPI.inDungeon() || DungeonAPI.dungeonFloor != "M7") {
            event.addIrrelevant("not in m7")
            return
        }

        event.addData {
            add("Power: ${getPower()}")
            add("inArchTeam: $inArcherTeam")
            add("inBersTeam: $inBerserkTeam")
            add("isHealer: $isHealer")
            add("isTank: $isTank")
            add("isSearching: $isSearching")
            add("dragon0: ${dragonOrder[0].name}")
            add("dragon1: ${dragonOrder[1].name}")
        }
    }

    private fun sendTitle(input: String) {
        titleString = input
        val duration: Duration = config.titleDuration.toDouble().seconds
        DelayedRun.runDelayed(duration) {
            titleString = ""
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (titleString == "") return
        config.dragonSpawnedPosition.renderString(titleString, posLabel = "Dragon Priority Title")
    }

    private fun getPower(): Double = DungeonAPI.DungeonBlessings.valueOf("POWER").power +
            (DungeonAPI.DungeonBlessings.valueOf("TIME").power.toDouble() / 2)
}